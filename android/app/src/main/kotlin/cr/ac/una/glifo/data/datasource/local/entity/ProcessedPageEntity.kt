package cr.ac.una.glifo.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Página que ya pasó por el pipeline local (issue #28). Si una captura nueva tiene un hash
 * perceptual casi igual, se reutiliza [analysisJson] en vez de volver a correr N0 + N1.
 */
@Entity(
    tableName = "processed_pages",
    indices = [Index(value = ["course_id"])]
)
data class ProcessedPageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "course_id")
    val courseId: Long,
    /** 64 bits en 16 caracteres hex, igual que note_pages.perceptual_hash en el backend. */
    @ColumnInfo(name = "perceptual_hash")
    val perceptualHash: String,
    @ColumnInfo(name = "photo_path")
    val photoPath: String,
    /** Resultado del pipeline serializado (Gson). */
    @ColumnInfo(name = "analysis_json")
    val analysisJson: String,
    @ColumnInfo(name = "processed_at")
    val processedAt: Long
)

/** Solo lo necesario para comparar distancias de Hamming sin cargar los análisis. */
data class PageHashRow(
    val id: Long,
    @ColumnInfo(name = "perceptual_hash")
    val perceptualHash: String
)
