package cr.ac.una.glifo.note.entity

import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity(name = "note_pages")
@Table(
    name = "note_pages",
    // Dos capturas offline no pueden reclamar la misma página al sincronizar.
    uniqueConstraints = [
        UniqueConstraint(name = "uq_note_pages_note_page", columnNames = ["note_id", "page_index"]),
    ],
    indexes = [
        Index(name = "idx_note_pages_hash", columnList = "perceptual_hash"),
    ],
    check = [
        CheckConstraint(
            name = "ck_note_pages_level",
            constraint = "level_reached IN ('N0', 'N1', 'N1_5', 'N2', 'N3', 'UNRESOLVED')",
        ),
        CheckConstraint(
            name = "ck_note_pages_confidence",
            constraint = "overall_confidence BETWEEN 0 AND 1",
        ),
    ],
)
class NotePage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    lateinit var note: Note

    @Column(nullable = false)
    var pageIndex: Int = 0

    @Column(nullable = false, length = 16)
    var perceptualHash: String = ""

    @Column(nullable = false, length = 512)
    var storageUri: String = ""

    @Column(nullable = false, length = 12)
    var levelReached: String = "N0"

    @Column(nullable = false)
    var overallConfidence: Float = 0.0f

    // Métricas de N0 (blur, brillo, reflejo...); crecen durante la calibración (§5.6).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var qualityMetrics: String? = null

    // Lista de regiones con bbox normalizado, tipo y nivel que las resolvió (§5.6).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var regions: String? = null

    var processedAt: Instant? = null

    override fun equals(other: Any?): Boolean = other is NotePage && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}
