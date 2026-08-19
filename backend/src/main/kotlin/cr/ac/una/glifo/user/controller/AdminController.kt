package cr.ac.una.glifo.user.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.user.dto.UpdateRolesRequest
import cr.ac.una.glifo.user.dto.UpdateStatusRequest
import cr.ac.una.glifo.user.dto.UserAdminResponse
import cr.ac.una.glifo.user.service.AdminService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin")
class AdminController(private val adminService: AdminService) {

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun getAllUsers(): ResponseEntity<ApiResponse<List<UserAdminResponse>>> {
        val users = adminService.getAllUsers()
        return ResponseEntity.ok(ApiResponse(data = users))
    }

    @PatchMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    fun updateUserRoles(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateRolesRequest
    ): ResponseEntity<ApiResponse<UserAdminResponse>> {
        val updated = adminService.updateUserRoles(id, request)
        return ResponseEntity.ok(ApiResponse(data = updated))
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun updateUserStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateStatusRequest
    ): ResponseEntity<ApiResponse<UserAdminResponse>> {
        val updated = adminService.updateUserStatus(id, request)
        return ResponseEntity.ok(ApiResponse(data = updated))
    }
}
