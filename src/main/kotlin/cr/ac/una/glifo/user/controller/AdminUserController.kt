package cr.ac.una.glifo.user.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.security.GlifoUserDetails
import cr.ac.una.glifo.user.dto.AdminResetPasswordRequest
import cr.ac.una.glifo.user.dto.AdminResetPasswordResponse
import cr.ac.una.glifo.user.service.AdminUserService
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * | Método | Ruta                               | Éxito | Errores                                              |
 * |--------|------------------------------------|-------|------------------------------------------------------|
 * | POST   | /admin/users/reset-password        | 200   | 400 (correo inválido, propia cuenta u otro admin), 401, 403 (sin USER_MANAGE), 404 (no existe) |
 */
@RestController
@RequestMapping(
    "\${url.admin}/users",
    produces = [MediaType.APPLICATION_JSON_VALUE],
    consumes = [MediaType.APPLICATION_JSON_VALUE]
)
class AdminUserController(private val adminUserService: AdminUserService) {

    @PostMapping("/reset-password")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun resetPassword(
        @AuthenticationPrincipal admin: GlifoUserDetails,
        @Valid @RequestBody request: AdminResetPasswordRequest
    ): ResponseEntity<ApiResponse<AdminResetPasswordResponse>> =
        ResponseEntity.ok(ApiResponse(data = adminUserService.resetPassword(admin.userId, request.email)))
}
