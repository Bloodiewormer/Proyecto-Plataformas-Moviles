package cr.ac.una.glifo.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cr.ac.una.glifo.data.datasource.local.entity.LocalCourseContextEntity
import cr.ac.una.glifo.data.dto.CourseGlossaryResponse
import cr.ac.una.glifo.domain.model.ContextSource
import cr.ac.una.glifo.domain.model.GlossaryTerm
import cr.ac.una.glifo.domain.model.LocalCourseContext

private val gson = Gson()
private val termListType = object : TypeToken<List<GlossaryTerm>>() {}.type

fun CourseGlossaryResponse.toEntity(cachedAt: Long): LocalCourseContextEntity = LocalCourseContextEntity(
    courseId = courseId,
    courseName = courseName,
    syllabusVersion = syllabusVersion,
    glossaryVersion = glossaryVersion,
    glossaryEntries = gson.toJson(entries.map { GlossaryTerm(it.term, it.canonicalForm, it.kind) }),
    cachedAt = cachedAt
)

fun LocalCourseContextEntity.toDomain(source: ContextSource): LocalCourseContext = LocalCourseContext(
    courseId = courseId,
    courseName = courseName,
    syllabusVersion = syllabusVersion,
    glossaryVersion = glossaryVersion,
    // Una fila corrupta no debe tumbar la captura: sin glosario el scorer igual funciona,
    // solo sin la bonificación por términos del curso.
    entries = runCatching { gson.fromJson<List<GlossaryTerm>>(glossaryEntries, termListType) }
        .getOrNull().orEmpty(),
    cachedAt = cachedAt,
    source = source
)
