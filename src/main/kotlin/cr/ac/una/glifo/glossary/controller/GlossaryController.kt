package cr.ac.una.glifo.glossary.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.glossary.dto.CourseGlossaryResponse
import cr.ac.una.glifo.glossary.dto.CreateGlossaryEntryRequest
import cr.ac.una.glifo.glossary.dto.CreateGlossarySuggestionRequest
import cr.ac.una.glifo.glossary.dto.GlossaryEntryResponse
import cr.ac.una.glifo.glossary.dto.GlossarySuggestionResponse
import cr.ac.una.glifo.glossary.dto.ResolveGlossarySuggestionRequest
import cr.ac.una.glifo.glossary.service.GlossaryService
import cr.ac.una.glifo.security.GlifoUserDetails
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * Glosario canónico del curso y sugerencias de los estudiantes (issue #33, D-15).
 *
 * | Método | Ruta                                        | Éxito | Errores                                        |
 * |--------|---------------------------------------------|-------|------------------------------------------------|
 * | GET    | /courses/{id}/glossary                      | 200   | 400, 404 (no es miembro)                       |
 * | POST   | /courses/{id}/glossary                      | 201   | 400, 403 (sin GLOSSARY_WRITE o no es dueño), 409 (duplicado), 415 |
 * | DELETE | /courses/{id}/glossary/{entryId}            | 204   | 400, 403, 404                                  |
 * | GET    | /courses/{id}/glossary/suggestions?status=  | 200   | 400, 403                                       |
 * | POST   | /courses/{id}/glossary/suggestions          | 201   | 400, 404, 415                                  |
 * | PATCH  | /courses/{id}/glossary/suggestions/{sid}    | 200   | 400, 403, 404, 409 (ya resuelta), 415          |
 */
@RestController
@RequestMapping("\${url.courses}/{courseId}/glossary", produces = [MediaType.APPLICATION_JSON_VALUE])
class GlossaryController(private val glossaryService: GlossaryService) {

    /** Lo usa la app para su copia offline del contexto del curso (D-17). */
    @GetMapping
    @PreAuthorize("hasAuthority('COURSE_READ')")
    fun getGlossary(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<CourseGlossaryResponse>> =
        ResponseEntity.ok(ApiResponse(data = glossaryService.getGlossary(courseId, user.userId)))

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('GLOSSARY_WRITE')")
    fun addEntry(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @Valid @RequestBody request: CreateGlossaryEntryRequest,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<GlossaryEntryResponse>> =
        // Sin Location: una entrada no tiene un GET propio, se consulta con GET /glossary.
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(data = glossaryService.addEntry(courseId, user.userId, request)))

    @DeleteMapping("/{entryId}")
    @PreAuthorize("hasAuthority('GLOSSARY_WRITE')")
    fun deleteEntry(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @PathVariable @Positive(message = "El id de la entrada debe ser positivo") entryId: Long,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<Void> {
        glossaryService.deleteEntry(courseId, entryId, user.userId)
        return ResponseEntity.noContent().build()
    }

    /** status: PENDING (por defecto), APPROVED, REJECTED o ALL. */
    @GetMapping("/suggestions")
    @PreAuthorize("hasAuthority('GLOSSARY_WRITE')")
    fun getSuggestions(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @RequestParam(required = false) status: String?,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<List<GlossarySuggestionResponse>>> =
        ResponseEntity.ok(ApiResponse(data = glossaryService.getSuggestions(courseId, user.userId, status)))

    /** Cualquier miembro del curso puede sugerir; NOTE_WRITE_OWN lo tiene quien corrige apuntes. */
    @PostMapping("/suggestions", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('NOTE_WRITE_OWN')")
    fun suggest(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @Valid @RequestBody request: CreateGlossarySuggestionRequest,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<GlossarySuggestionResponse>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(data = glossaryService.suggest(courseId, user.userId, request)))

    @PatchMapping("/suggestions/{suggestionId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('GLOSSARY_WRITE')")
    fun resolveSuggestion(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @PathVariable @Positive(message = "El id de la sugerencia debe ser positivo") suggestionId: Long,
        @Valid @RequestBody request: ResolveGlossarySuggestionRequest,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<GlossarySuggestionResponse>> =
        ResponseEntity.ok(
            ApiResponse(data = glossaryService.resolveSuggestion(courseId, suggestionId, user.userId, request))
        )
}
