package cr.ac.una.glifo.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Apunte guardado en el teléfono (issue #25). Sirve para las dos cosas que pide el alcance:
 * caché de lectura de los apuntes que vienen del servidor y almacenamiento de las capturas
 * hechas sin conexión. [id] es local y es el que usa la navegación; [serverId] se llena
 * cuando el servidor confirma el apunte (al listar, o cuando la cola lo sube).
 */
@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["course_id"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,
    @ColumnInfo(name = "course_id")
    val courseId: Long,
    val title: String,
    /** ISO yyyy-MM-dd, igual que class_date en el backend. */
    @ColumnInfo(name = "class_date")
    val classDate: String,
    /** DRAFT · PROCESSING · READY · ARCHIVED (NoteStatus, D-16). */
    val status: String,
    /** Reconstrucción IA-01 del servidor (JSON §5.5), si ya existe. */
    @ColumnInfo(name = "content_json")
    val contentJson: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "remote_page_count", defaultValue = "0")
    val remotePageCount: Int = 0
)

/** Página capturada y procesada en el teléfono: la foto se queda en el almacenamiento de la app. */
@Entity(
    tableName = "note_pages",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["note_id", "page_index"], unique = true),
        Index(value = ["perceptual_hash"])
    ]
)
data class NotePageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "note_id")
    val noteId: Long,
    @ColumnInfo(name = "page_index")
    val pageIndex: Int,
    @ColumnInfo(name = "perceptual_hash")
    val perceptualHash: String,
    @ColumnInfo(name = "photo_path")
    val photoPath: String,
    @ColumnInfo(name = "level_reached")
    val levelReached: String,
    @ColumnInfo(name = "overall_confidence")
    val overallConfidence: Float,
    /** Resultado del pipeline (snapshot Gson de PageAnalysis). */
    @ColumnInfo(name = "analysis_json")
    val analysisJson: String,
    @ColumnInfo(name = "processed_at")
    val processedAt: Long
)

/**
 * Corrección manual de un fragmento (issue #20, D-15). Se guarda aparte del análisis para no
 * reescribir el resultado del pipeline: al leer el apunte, la corrección se aplica encima.
 */
@Entity(
    tableName = "fragment_corrections",
    primaryKeys = ["note_id", "fragment_id"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FragmentCorrectionEntity(
    @ColumnInfo(name = "note_id")
    val noteId: Long,
    @ColumnInfo(name = "fragment_id")
    val fragmentId: String,
    @ColumnInfo(name = "corrected_text")
    val correctedText: String,
    @ColumnInfo(name = "original_text")
    val originalText: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
