package cr.ac.una.glifo.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cr.ac.una.glifo.data.datasource.local.entity.StudyItemEntity

@Dao
interface StudyItemDao {

    @Query("SELECT * FROM study_items WHERE course_id = :courseId ORDER BY id ASC")
    suspend fun itemsForCourse(courseId: Long): List<StudyItemEntity>

    @Upsert
    suspend fun upsertAll(items: List<StudyItemEntity>)

    @Query("DELETE FROM study_items WHERE course_id = :courseId")
    suspend fun deleteForCourse(courseId: Long)
}
