package cr.ac.una.glifo.glossary.mapper

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.glossary.dto.CourseGlossaryResponse
import cr.ac.una.glifo.glossary.dto.GlossaryEntryResponse
import cr.ac.una.glifo.glossary.dto.GlossarySuggestionResponse
import cr.ac.una.glifo.glossary.entity.CourseGlossary
import cr.ac.una.glifo.glossary.entity.GlossarySuggestion

fun CourseGlossary.toResponse(): GlossaryEntryResponse = GlossaryEntryResponse(
    id = this.id,
    term = this.term,
    canonicalForm = this.canonicalForm,
    kind = this.kind
)

fun Course.toGlossaryResponse(entries: List<CourseGlossary>): CourseGlossaryResponse = CourseGlossaryResponse(
    courseId = this.id,
    courseName = this.name,
    syllabusVersion = this.syllabusVersion,
    glossaryVersion = this.glossaryVersion,
    entries = entries.map { it.toResponse() }
)

fun GlossarySuggestion.toResponse(): GlossarySuggestionResponse = GlossarySuggestionResponse(
    id = this.id,
    courseId = this.course?.id ?: 0,
    suggestedBy = listOfNotNull(this.user?.firstName, this.user?.lastName).joinToString(" ")
        .ifBlank { this.user?.email ?: "" },
    noteId = this.note?.id,
    originalText = this.originalText,
    suggestedCorrection = this.suggestedCorrection,
    status = this.status,
    createdAt = this.createdAt,
    resolvedAt = this.resolvedAt
)
