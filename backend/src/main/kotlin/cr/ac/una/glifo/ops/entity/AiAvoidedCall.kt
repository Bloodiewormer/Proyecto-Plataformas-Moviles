package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant

// Una fila = una solicitud a una función de IA que el backend atendió con un resultado ya
// guardado, sin llamar al proveedor (issue #46): fórmula ya resuelta en N1.5, página sin
// regiones pendientes para N2, apunte ya reconstruido (IA-01), temas que ya tenían ítems
// (IA-02). Solo se cuenta cuando se omitió la llamada COMPLETA; no lleva tokens ni costo,
// porque serían una suposición sobre una llamada que no ocurrió.
//
// Tabla aparte de ai_calls a propósito: así los totales de llamadas reales no cambian.
@Entity
@Table(
    name = "ai_avoided_calls",
    indexes = [
        Index(name = "idx_ai_avoided_calls_user_created", columnList = "user_id, created_at"),
    ],
    check = [
        CheckConstraint(
            name = "ck_ai_avoided_calls_type",
            constraint = "call_type IN ('OCR_M', 'IA_00', 'IA_01', 'IA_02', 'IA_03', 'IA_04')",
        ),
        CheckConstraint(
            name = "ck_ai_avoided_calls_level",
            constraint = "level IN ('N0', 'N1', 'N1_5', 'N2', 'N3', 'UNRESOLVED')",
        ),
    ],
)
class AiAvoidedCall {
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

    // Tipo de la llamada que se habría hecho, con los mismos valores que ai_calls.call_type.
    @Column(name = "call_type", nullable = false, length = 16)
    var callType: String = ""

    // Nivel en el que se habría hecho; NULL para IA-01 e IA-02, igual que en ai_calls.
    @Column(length = 12)
    var level: String? = null

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as AiAvoidedCall
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String = "AiAvoidedCall(id=$id, callType='$callType', level=$level)"
}
