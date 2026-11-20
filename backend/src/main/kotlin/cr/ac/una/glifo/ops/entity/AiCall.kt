package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.math.BigDecimal
import java.time.Instant

// Relacional a propósito: se agrega por tipo, nivel y periodo, que es el argumento de
// ahorro del proyecto (Glifo_UML_Modeling.md §5.1).
//
// Una fila = una solicitud que de verdad salió hacia un proveedor de IA (issue #46). Lo
// que no llegó a enviarse (IA sin configurar, material reutilizado, validación local de
// JLaTeXMath) no se registra acá; el material reutilizado se cuenta en ai_avoided_calls. NULL significa "desconocido", nunca cero: tokens que el
// proveedor no informó, costo sin tarifa configurada, nivel que no aplica.
@Entity
@Table(
    name = "ai_calls",
    indexes = [
        Index(name = "idx_ai_calls_user_created", columnList = "user_id, created_at"),
    ],
    check = [
        CheckConstraint(
            name = "ck_ai_calls_type",
            constraint = "call_type IN ('OCR_M', 'IA_00', 'IA_01', 'IA_02', 'IA_03', 'IA_04')",
        ),
        CheckConstraint(
            name = "ck_ai_calls_level",
            constraint = "level IN ('N0', 'N1', 'N1_5', 'N2', 'N3', 'UNRESOLVED')",
        ),
        // Si el proveedor falló no hubo salida que aceptar, y el fallo lleva código.
        CheckConstraint(
            name = "ck_ai_calls_outcome",
            constraint = "success = true OR (output_accepted IS NULL AND error_code IS NOT NULL)",
        ),
    ],
)
class AiCall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var course: Course? = null

    @Column(name = "call_type", nullable = false, length = 16)
    var callType: String = ""

    // Nivel de la escalera de percepción (N0…N3) que originó la llamada. NULL para las
    // funciones IA-0x que no pertenecen a esa escalera, como IA-01 e IA-02.
    @Column(length = 12)
    var level: String? = null

    // Modelo que reportó el proveedor en la respuesta. NULL si la llamada falló sin respuesta.
    @Column(length = 100)
    var model: String? = null

    // Tokens que informó el proveedor (usage); NULL si no los informó.
    @Column(name = "input_tokens")
    var inputTokens: Int? = null

    @Column(name = "output_tokens")
    var outputTokens: Int? = null

    // Costo estimado en USD con 8 decimales, calculado al registrar con la tarifa vigente
    // (glifo.ai.pricing). NULL si faltan tokens o tarifa. Cambiar una tarifa no lo reescribe.
    @Column(name = "estimated_cost", precision = 12, scale = 8)
    var estimatedCost: BigDecimal? = null

    // Desde que empezó la llamada hasta que el proveedor respondió o falló.
    @Column(name = "latency_ms", nullable = false)
    var latencyMs: Int = 0

    // Resultado de la llamada AL PROVEEDOR: true si devolvió una respuesta utilizable. No
    // dice si el contenido cumplió el esquema de negocio; eso es outputAccepted.
    @Column(nullable = false)
    var success: Boolean? = null

    // Solo con success = true: si el servicio aceptó el contenido (esquema IA-01/IA-02).
    @Column(name = "output_accepted")
    var outputAccepted: Boolean? = null

    // Código estable del fallo del proveedor o del rechazo del contenido (PROVIDER_HTTP_429…).
    @Column(name = "error_code", length = 64)
    var errorCode: String? = null

    // Resumen técnico saneado y recortado; nunca el cuerpo de la respuesta ni secretos.
    @Column(name = "error_message", length = 255)
    var errorMessage: String? = null

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as AiCall
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String = "AiCall(id=$id, callType='$callType', level=$level, model=$model, success=$success, latencyMs=$latencyMs)"
}
