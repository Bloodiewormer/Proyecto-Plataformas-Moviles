package cr.ac.una.glifo.user.mapper

import cr.ac.una.glifo.user.dto.UserResponse
import cr.ac.una.glifo.user.entity.User

fun User.toResponse(): UserResponse {
    val roleNames = this.roles.map { it.name }
    return UserResponse(
        id = this.id,
        email = this.email,
        firstName = this.firstName,
        lastName = this.lastName,
        role = primaryRole(roleNames),
        roles = roleNames
    )
}

/**
 * La app maneja un solo rol por usuario (enum GlifoRole: Student, Teacher, Admin). Si
 * alguien tuviera varios, gana el de más permisos.
 */
private fun primaryRole(roleNames: List<String>): String = when {
    "ROLE_ADMIN" in roleNames -> "Admin"
    "ROLE_TEACHER" in roleNames -> "Teacher"
    else -> "Student"
}
