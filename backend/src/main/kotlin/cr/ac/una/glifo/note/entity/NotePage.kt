package cr.ac.una.glifo.note.entity

import jakarta.persistence.*
import java.time.Instant

@Entity(name = "note_pages")
class NotePage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    lateinit var note: Note

    @Column(nullable = false)
    var pageIndex: Int = 0

    @Column(nullable = false, length = 16)
    var perceptualHash: String = ""

    @Column(nullable = false, length = 512)
    var storageUri: String = ""

    @Column(nullable = false)
    var levelReached: String = "N0"

    @Column(nullable = false)
    var overallConfidence: Float = 0.0f

    @Column(columnDefinition = "jsonb")
    var qualityMetrics: String? = null

    @Column(columnDefinition = "jsonb")
    var regions: String? = null

    var processedAt: Instant? = null

    override fun equals(other: Any?): Boolean = other is NotePage && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}
