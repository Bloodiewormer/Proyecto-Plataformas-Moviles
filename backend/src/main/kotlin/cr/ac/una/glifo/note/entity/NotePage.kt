package cr.ac.una.glifo.note.entity

import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    lateinit var note: Note

    @Column(name = "page_index", nullable = false)
    var pageIndex: Int = 0

    @Column(name = "perceptual_hash", nullable = false, length = 16)
    var perceptualHash: String = ""

    @Column(name = "storage_uri", nullable = false, length = 512)
    var storageUri: String = ""

    @Column(name = "level_reached", nullable = false, length = 12)
    var levelReached: String = "N0"

    @Column(name = "overall_confidence", nullable = false)
    var overallConfidence: Float = 0.0f

    // Métricas de N0 (blur, brillo, reflejo...); crecen durante la calibración (§5.6).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quality_metrics", columnDefinition = "jsonb")
    var qualityMetrics: String? = null

    // Lista de regiones con bbox normalizado, tipo y nivel que las resolvió (§5.6).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var regions: String? = null

    @Column(name = "processed_at")
    var processedAt: Instant? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as NotePage
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String = "NotePage(id=$id, pageIndex=$pageIndex, perceptualHash='$perceptualHash', storageUri='$storageUri', levelReached='$levelReached', overallConfidence=$overallConfidence)"
}
