package cr.ac.una.glifo.user.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.security.GlifoUserDetails
import cr.ac.una.glifo.user.dto.AdminResetPasswordRequest
import cr.ac.una.glifo.user.dto.AdminChangeRoleRequest
import cr.ac.una.glifo.user.dto.UserResponse
import cr.ac.una.glifo.user.dto.AdminRoleResponse
import cr.ac.una.glifo.user.dto.AdminResetPasswordResponse
import cr.ac.una.glifo.user.service.AdminUserService
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * | Método | Ruta                               | Éxito | Errores                                              |
 * |--------|------------------------------------|-------|------------------------------------------------------|
 * | GET    | /admin/users                       | 200   | 401, 403 (sin USER_MANAGE)                           |
 * | GET    | /admin/users/roles                 | 200   | 401, 403 (sin USER_MANAGE)                           |
 * | PUT    | /admin/users/{id}/role             | 200   | 400 (rol inválido o propia cuenta), 401, 403 (sin ROLE_MANAGE), 404 (no existe) |
 * | POST   | /admin/users/reset-password        | 200   | 400 (correo inválido, propia cuenta u otro admin), 401, 403 (sin USER_MANAGE), 404 (no existe) |
 */
@RestController
@RequestMapping(
    "\${url.admin}/users",
    produces = [MediaType.APPLICATION_JSON_VALUE],
    consumes = [MediaType.APPLICATION_JSON_VALUE]
)
class AdminUserController(private val adminUserService: AdminUserService) {

    @GetMapping(consumes = [MediaType.ALL_VALUE])
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun listUsers(): ResponseEntity<ApiResponse<List<UserResponse>>> =
        ResponseEntity.ok(ApiResponse(data = adminUserService.listUsers()))

    @GetMapping("/roles", consumes = [MediaType.ALL_VALUE])
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun listRoles(): ResponseEntity<ApiResponse<List<AdminRoleResponse>>> =
        ResponseEntity.ok(ApiResponse(data = adminUserService.listRoles()))

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    fun changeRole(
        @AuthenticationPrincipal admin: GlifoUserDetails,
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminChangeRoleRequest
    ): ResponseEntity<ApiResponse<UserResponse>> =
        ResponseEntity.ok(ApiResponse(data = adminUserService.changeRole(admin.userId, id, request.role)))

    @PostMapping("/reset-password")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun resetPassword(
        @AuthenticationPrincipal admin: GlifoUserDetails,
        @Valid @RequestBody request: AdminResetPasswordRequest
    ): ResponseEntity<ApiResponse<AdminResetPasswordResponse>> =
        ResponseEntity.ok(ApiResponse(data = adminUserService.resetPassword(admin.userId, request.email)))
}
