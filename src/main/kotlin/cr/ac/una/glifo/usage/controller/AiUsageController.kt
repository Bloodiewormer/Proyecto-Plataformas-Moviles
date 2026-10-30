package cr.ac.una.glifo.usage.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.security.GlifoUserDetails
import cr.ac.una.glifo.usage.dto.AiUsageResponse
import cr.ac.una.glifo.usage.service.AiUsageService
import jakarta.validation.constraints.Positive
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Uso y costo de IA registrado en ai_calls (#46). Solo totales, nunca filas sueltas.
 *
 * | Método | Ruta                          | Privilegio        | Éxito | Errores                              |
 * |--------|-------------------------------|-------------------|-------|--------------------------------------|
 * | GET    | /ai-usage/me?courseId=        | USAGE_READ_OWN    | 200   | 400, 401, 403                        |
 * | GET    | /courses/{courseId}/ai-usage  | USAGE_READ_COURSE | 200   | 400, 401, 403 (no es el dueño), 404  |
 */
@RestController
class AiUsageController(private val aiUsageService: AiUsageService) {

    /** Llamadas del usuario autenticado; con `courseId`, solo las de ese curso. */
    @GetMapping("\${url.ai-usage}/me", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('USAGE_READ_OWN')")
    fun getOwnUsage(
        @RequestParam(required = false) @Positive(message = "El curso no es válido") courseId: Long?,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<AiUsageResponse>> =
        ResponseEntity.ok(ApiResponse(data = aiUsageService.getOwnUsage(user.userId, courseId)))

    /** Todas las llamadas del curso, agregadas; solo para el docente dueño. */
    @GetMapping("\${url.courses}/{courseId}/ai-usage", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('USAGE_READ_COURSE')")
    fun getCourseUsage(
        @PathVariable @Positive(message = "El id del curso debe ser positivo") courseId: Long,
        @AuthenticationPrincipal user: GlifoUserDetails
    ): ResponseEntity<ApiResponse<AiUsageResponse>> =
        ResponseEntity.ok(ApiResponse(data = aiUsageService.getCourseUsage(courseId, user.userId)))
}
