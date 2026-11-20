package cr.ac.una.glifo.ai.ledger

import cr.ac.una.glifo.ai.AiProviderException
import cr.ac.una.glifo.ai.LlmResult
import cr.ac.una.glifo.ai.config.AiPricingProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/** Lo que informó el proveedor sobre una respuesta recibida; null = no lo informó. */
data class ProviderUsage(
    val model: String?,
    val inputTokens: Int?,
    val outputTokens: Int?,
    val latencyMs: Long
)

fun LlmResult.usage() = ProviderUsage(model, inputTokens, outputTokens, latencyMs)

/**
 * Registro de llamadas a proveedores de IA en `ai_calls` (issue #46). Es la ÚNICA capa que
 * audita: cada servicio IA-0x envuelve su llamada al proveedor en [track] una sola vez, y ni
 * el cliente HTTP ni el orquestador registran por su cuenta.
 *
 * Qué se registra:
 * - Llamada enviada que falló (HTTP, red, respuesta inutilizable): success = false.
 * - Respuesta utilizable cuyo contenido el servicio rechazó: success = true, outputAccepted = false.
 * - Respuesta aceptada: success = true, outputAccepted = true.
 * - IA sin configurar ([AiProviderException.isNotConfigured]): NO se registra, la solicitud
 *   nunca salió.
 * - Solicitud atendida con un resultado ya guardado (material reutilizado): no va a ai_calls
 *   sino a ai_avoided_calls, con [avoided].
 *
 * Política ante fallos de la auditoría: guardar el registro nunca cambia el resultado. Si la
 * base de datos rechaza el INSERT, se emite un error saneado en el log y la operación sigue
 * con su valor o su excepción original. El registro, por lo tanto, es de mejor esfuerzo: si
 * la base no está disponible esa llamada se pierde (solo queda el log).
 */
@Service
class CostLedgerService(
    private val recorder: AiCallRecorder,
    private val costCalculator: CostCalculator,
    private val nanoTime: () -> Long
) {

    @Autowired
    constructor(recorder: AiCallRecorder, pricing: AiPricingProperties) :
        this(recorder, CostCalculator(pricing), System::nanoTime)

    private val log = LoggerFactory.getLogger(CostLedgerService::class.java)

    /**
     * Ejecuta [call] (la solicitud al proveedor) y luego [accept] (la validación del
     * contenido) y registra una fila con el resultado. Devuelve lo que devuelve [accept] y
     * relanza sin cambios cualquier excepción de [call] o de [accept].
     *
     * [accept] debe limitarse a interpretar y validar la respuesta; la persistencia de
     * negocio va después, fuera de [track], para que un fallo al guardar no se confunda con
     * un contenido rechazado. Un contenido se rechaza lanzando una excepción en [accept] o,
     * cuando el servicio sigue adelante igual (una fórmula que escala a N2), devolviendo un
     * motivo en [rejection].
     */
    fun <T> track(
        context: AiCallContext,
        call: () -> LlmResult,
        accept: (LlmResult) -> T,
        rejection: (T) -> String? = { null }
    ): T = track(context, call, { it.usage() }, accept, rejection)

    /**
     * Igual que la otra sobrecarga, para un proveedor cuyo resultado no es [LlmResult] (por
     * ejemplo un MathOcrEngine): [usage] dice de dónde salen modelo, tokens y latencia.
     */
    fun <R, T> track(
        context: AiCallContext,
        call: () -> R,
        usage: (R) -> ProviderUsage,
        accept: (R) -> T,
        rejection: (T) -> String? = { null }
    ): T {
        val start = nanoTime()
        val result = try {
            call()
        } catch (ex: Exception) {
            if (ex is AiProviderException && ex.isNotConfigured) throw ex
            val diagnostic = AiErrorDiagnostics.providerFailure(ex)
            record(
                AiCallRecord(
                    context = context,
                    model = null,
                    inputTokens = null,
                    outputTokens = null,
                    estimatedCostUsd = null,
                    latencyMs = elapsedMs(start),
                    success = false,
                    outputAccepted = null,
                    errorCode = diagnostic.code,
                    errorMessage = diagnostic.message
                )
            )
            throw ex
        }

        val received = usage(result)
        val accepted = try {
            accept(result)
        } catch (ex: Exception) {
            val diagnostic = AiErrorDiagnostics.outputRejected(ex)
            record(receivedRecord(context, received, outputAccepted = false, diagnostic))
            throw ex
        }
        val reason = rejection(accepted)
        val diagnostic = reason?.let { AiErrorDiagnostic("OUTPUT_REJECTED", AiErrorDiagnostics.sanitize(it)) }
        record(receivedRecord(context, received, outputAccepted = diagnostic == null, diagnostic))
        return accepted
    }

    /**
     * Registra en ai_avoided_calls que el servicio atendió la solicitud con un resultado ya
     * guardado y omitió la llamada completa al proveedor. Solo cuenta: sin tokens ni costo,
     * que serían una suposición. Como [track], un fallo al guardarlo solo queda en el log.
     */
    fun avoided(context: AiCallContext) {
        try {
            recorder.saveAvoided(context)
        } catch (ex: RuntimeException) {
            log.error(
                "No se pudo registrar la llamada IA evitada (tipo={}, usuario={}): {}",
                context.callType, context.origin.userId,
                AiErrorDiagnostics.sanitize("${ex.javaClass.simpleName}: ${ex.message}")
            )
        }
    }

    /** Respuesta recibida: modelo, consumo y latencia los informó el proveedor. */
    private fun receivedRecord(
        context: AiCallContext,
        usage: ProviderUsage,
        outputAccepted: Boolean,
        diagnostic: AiErrorDiagnostic?
    ) = AiCallRecord(
        context = context,
        model = usage.model?.ifBlank { null },
        inputTokens = usage.inputTokens,
        outputTokens = usage.outputTokens,
        estimatedCostUsd = costCalculator.estimate(usage.model, usage.inputTokens, usage.outputTokens),
        latencyMs = usage.latencyMs,
        success = true,
        outputAccepted = outputAccepted,
        errorCode = diagnostic?.code,
        errorMessage = diagnostic?.message
    )

    private fun record(record: AiCallRecord) {
        try {
            recorder.save(record)
        } catch (ex: RuntimeException) {
            log.error(
                "No se pudo registrar la llamada IA en ai_calls (tipo={}, success={}, usuario={}): {}",
                record.context.callType, record.success, record.context.origin.userId,
                AiErrorDiagnostics.sanitize("${ex.javaClass.simpleName}: ${ex.message}")
            )
        }
    }

    private fun elapsedMs(start: Long): Long = ((nanoTime() - start) / 1_000_000L).coerceAtLeast(0L)
}
