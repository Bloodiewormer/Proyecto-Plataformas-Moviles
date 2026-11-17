package cr.ac.una.glifo.coverage.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.coverage.dto.CourseGapsResponse
import cr.ac.una.glifo.coverage.dto.CoverageDeltaResponse
import cr.ac.una.glifo.coverage.dto.CoverageResponse
import cr.ac.una.glifo.coverage.service.CoverageEvaluationService
import cr.ac.una.glifo.coverage.service.CoverageService
import cr.ac.una.glifo.security.GlifoUserDetails
import jakarta.validation.constraints.Positive
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Cobertura del temario del usuario autenticado en un curso.
 *
 * | Método | Ruta                                  | Éxito | Errores                                   |
 * |--------|---------------------------------------|-------|-------------------------------------------|
 * | GET    | /courses/{id}/coverage                | 200   | 400, 403 (sin COVERAGE_READ_OWN), 404     |
 * | GET    | /courses/{id}/coverage/delta?since=   | 200   | 400 (fecha inválida o futura), 403, 404   |
 * | POST   | /courses/{id}/coverage/evaluate       | 200   | 400, 401, 403, 404, 409 (en curso o espera), 502 |
 * | GET    | /courses/{id}/coverage/gaps           | 200   | 400, 401, 403 (sin COVERAGE_READ_COURSE), 404 (no es el dueño) |
 *
 * /gaps es del docente: su @PreAuthorize de método reemplaza al de la clase.
 */
@RestController
@RequestMapping("\${url.courses}/{courseId}/coverage", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAuthority('COVERAGE_READ_OWN')")
class CoverageController(
    private val coverageService: CoverageService,
    private val coverageEvaluationService: CoverageEvaluationService
) {

    @GetMapping
    fun getCoverage(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<CoverageResponse>> =
        ResponseEntity.ok(ApiResponse(data = coverageService.getCoverage(courseId, user.userId)))

    /** `since` en ISO-8601, por ejemplo 2026-10-07T00:00:00Z. */
    @GetMapping("/delta")
    fun getCoverageDelta(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) since: Instant,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<CoverageDeltaResponse>> =
        ResponseEntity.ok(ApiResponse(data = coverageService.getCoverageDelta(courseId, user.userId, since)))

    /** Brechas del grupo (docente dueño o admin): por tema, cuántos estudiantes no lo tienen SOLID. */
    @GetMapping("/gaps")
    @PreAuthorize("hasAuthority('COVERAGE_READ_COURSE')")
    fun getCourseGaps(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<CourseGapsResponse>> =
        ResponseEntity.ok(ApiResponse(data = coverageService.getCourseGaps(courseId, user.userId, user.isAdmin())))

    /** IA-04: recalcula la cobertura; en meta va cuántos temas resolvió el pre-filtro y cuántos la IA. */
    @PostMapping("/evaluate")
    fun evaluate(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<CoverageResponse>> {
        val result = coverageEvaluationService.evaluate(courseId, user.userId)
        return ResponseEntity.ok(
            ApiResponse(
                data = result.coverage,
                meta = mapOf(
                    "prefilter_resolved" to result.prefilterResolved,
                    "ai_adjudicated" to result.adjudicated,
                    "uncertain" to result.uncertain,
                    "ai_called" to result.aiCalled
                )
            )
        )
    }
}
