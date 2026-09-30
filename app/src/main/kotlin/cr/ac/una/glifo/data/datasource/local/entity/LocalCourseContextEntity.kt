package cr.ac.una.glifo.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una fila por curso (D-17). El glosario va serializado en [glossaryEntries] en vez de en
 * una tabla propia: se lee y se reemplaza completo, nunca se consulta término por término.
 */
@Entity(tableName = "local_course_context")
data class LocalCourseContextEntity(
    @PrimaryKey
    @ColumnInfo(name = "course_id")
    val courseId: Long,
    @ColumnInfo(name = "course_name")
    val courseName: String,
    @ColumnInfo(name = "syllabus_version")
    val syllabusVersion: Int,
    @ColumnInfo(name = "glossary_version")
    val glossaryVersion: Int,
    /** JSON (Gson) con la lista de términos. */
    @ColumnInfo(name = "glossary_entries")
    val glossaryEntries: String,
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long
)

/** Proyección liviana para comparar versiones sin deserializar el glosario. */
data class ContextVersions(
    @ColumnInfo(name = "syllabus_version")
    val syllabusVersion: Int,
    @ColumnInfo(name = "glossary_version")
    val glossaryVersion: Int
)
