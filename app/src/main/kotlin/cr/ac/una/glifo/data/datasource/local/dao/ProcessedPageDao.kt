package cr.ac.una.glifo.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cr.ac.una.glifo.data.datasource.local.entity.PageHashRow
import cr.ac.una.glifo.data.datasource.local.entity.ProcessedPageEntity

@Dao
interface ProcessedPageDao {

    @Insert
    suspend fun insert(page: ProcessedPageEntity): Long

    /**
     * SQLite no tiene popcount, así que la distancia de Hamming se calcula en Kotlin sobre
     * los hashes del curso. Son decenas de filas por curso, no miles.
     */
    @Query("SELECT id, perceptual_hash FROM processed_pages WHERE course_id = :courseId")
    suspend fun hashesForCourse(courseId: Long): List<PageHashRow>

    @Query("SELECT * FROM processed_pages WHERE id = :id")
    suspend fun getById(id: Long): ProcessedPageEntity?

    /** Todas las páginas que pasaron por el pipeline en este teléfono (uso de IA, #46/#47). */
    @Query("SELECT * FROM processed_pages ORDER BY processed_at")
    suspend fun all(): List<ProcessedPageEntity>
}
