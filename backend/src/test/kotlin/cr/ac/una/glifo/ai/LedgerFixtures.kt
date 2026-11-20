package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.config.AiPricingProperties
import cr.ac.una.glifo.ai.ledger.AiCallContext
import cr.ac.una.glifo.ai.ledger.AiCallRecord
import cr.ac.una.glifo.ai.ledger.AiCallRecorder
import cr.ac.una.glifo.ai.ledger.CostCalculator
import cr.ac.una.glifo.ai.ledger.CostLedgerService
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.ops.repository.AiAvoidedCallRepository
import cr.ac.una.glifo.ops.repository.AiCallRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.mockito.Mockito.mock

/**
 * CostLedgerService real con el guardado simulado, para las pruebas de un servicio IA que no
 * verifican ai_calls. El registro en sí se prueba en AiCallAuditTest y CostLedgerServiceTest.
 */
fun discardingLedger(): CostLedgerService =
    CostLedgerService(mock(AiCallRecorder::class.java), CostCalculator(AiPricingProperties()), System::nanoTime)

/** Recorder que junta en memoria las filas que se habrían insertado en ai_calls y ai_avoided_calls. */
class RecordingAiCallRecorder : AiCallRecorder(
    mock(AiCallRepository::class.java),
    mock(AiAvoidedCallRepository::class.java),
    mock(UserRepository::class.java),
    mock(CourseRepository::class.java)
) {
    val records = mutableListOf<AiCallRecord>()
    val avoided = mutableListOf<AiCallContext>()
    override fun save(record: AiCallRecord): Long {
        records += record
        return records.size.toLong()
    }
    override fun saveAvoided(context: AiCallContext): Long {
        avoided += context
        return avoided.size.toLong()
    }
}

fun recordingLedger(recorder: RecordingAiCallRecorder): CostLedgerService =
    CostLedgerService(recorder, CostCalculator(AiPricingProperties()), System::nanoTime)
