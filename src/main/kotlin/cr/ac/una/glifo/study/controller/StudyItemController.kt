package cr.ac.una.glifo.study.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.security.GlifoUserDetails
import cr.ac.una.glifo.study.dto.GenerateStudyItemsRequest
import cr.ac.una.glifo.study.dto.StudyItemResponse
import cr.ac.una.glifo.study.service.StudyItemService
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * | Método | Ruta                                    | Privilegio          | Éxito    |
 * |--------|-----------------------------------------|---------------------|----------|
 * | POST   | /courses/{courseId}/study-items/generate | STUDY_ITEM_GENERATE | 201, 200 |
 * | GET    | /courses/{courseId}/study-items          | COURSE_READ         | 200      |
 */
@RestController
@RequestMapping("\${url.courses}", produces = [MediaType.APPLICATION_JSON_VALUE])
class StudyItemController(
    private val studyItemService: StudyItemService
) {

    @PostMapping("/{courseId}/study-items/generate", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('STUDY_ITEM_GENERATE')")
    fun generateStudyItems(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @Valid @RequestBody request: GenerateStudyItemsRequest,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<List<StudyItemResponse>>> {
        val outcome = studyItemService.generate(courseId, request, user.userId)
        val status = if (outcome.generatedTopics > 0) HttpStatus.CREATED else HttpStatus.OK
        val meta = mapOf(
            "generatedTopics" to outcome.generatedTopics,
            "reusedTopics" to outcome.reusedTopics,
            "generatedItems" to outcome.generatedItems
        )
        return ResponseEntity.status(status).body(
            ApiResponse(
                data = outcome.items,
                meta = meta
            )
        )
    }

    @GetMapping("/{courseId}/study-items")
    @PreAuthorize("hasAuthority('COURSE_READ')")
    fun getStudyItems(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @RequestParam(required = false) @Positive(message = "El id del tema debe ser positivo") topicId: Long? = null,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<List<StudyItemResponse>>> {
        val items = studyItemService.list(courseId, topicId, user.userId)
        return ResponseEntity.ok(ApiResponse(data = items))
    }
}
