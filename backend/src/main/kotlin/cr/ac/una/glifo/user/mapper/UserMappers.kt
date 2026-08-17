package cr.ac.una.glifo.user.mapper

import cr.ac.una.glifo.user.dto.UserResponse
import cr.ac.una.glifo.user.entity.User

fun User.toResponse(): UserResponse = UserResponse(
    id = this.id,
    email = this.email,
    firstName = this.firstName,
    lastName = this.lastName,
    roles = this.roles.map { it.name }
)
