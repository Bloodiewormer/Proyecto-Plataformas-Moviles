package cr.ac.una.glifo.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cr.ac.una.glifo.data.datasource.local.entity.ContextVersions
import cr.ac.una.glifo.data.datasource.local.entity.LocalCourseContextEntity

@Dao
interface LocalCourseContextDao {

    @Upsert
    suspend fun upsert(context: LocalCourseContextEntity)

    @Query("SELECT * FROM local_course_context WHERE course_id = :courseId")
    suspend fun forCourse(courseId: Long): LocalCourseContextEntity?

    @Query("SELECT syllabus_version, glossary_version FROM local_course_context WHERE course_id = :courseId")
    suspend fun versions(courseId: Long): ContextVersions?

    @Query("UPDATE local_course_context SET cached_at = :cachedAt WHERE course_id = :courseId")
    suspend fun touch(courseId: Long, cachedAt: Long)
}
