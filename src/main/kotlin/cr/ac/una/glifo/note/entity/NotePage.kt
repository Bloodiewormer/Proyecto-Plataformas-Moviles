package cr.ac.una.glifo.note.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "note_pages")
class NotePage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    lateinit var note: Note

    @Column(name = "page_index", nullable = false)
    var pageIndex: Int = 0

    @Column(name = "perceptual_hash", nullable = false, length = 16)
    var perceptualHash: String = ""

    @Column(name = "storage_uri", nullable = false, length = 512)
    var storageUri: String = ""

    @Column(name = "level_reached", nullable = false, length = 10)
    var levelReached: String = "N0"

    @Column(name = "overall_confidence", nullable = false)
    var overallConfidence: Float = 0.0f

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quality_metrics", columnDefinition = "jsonb")
    var qualityMetrics: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var regions: String? = null

    @Column(name = "processed_at")
    var processedAt: Instant? = null

    override fun equals(other: Any?): Boolean = other is NotePage && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "NotePage(id=$id, pageIndex=$pageIndex, perceptualHash='$perceptualHash', storageUri='$storageUri', levelReached='$levelReached', overallConfidence=$overallConfidence)"
}
