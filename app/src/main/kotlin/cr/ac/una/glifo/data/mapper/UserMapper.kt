package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.data.datasource.dummy.DummyAccount
import cr.ac.una.glifo.data.dto.UserDto
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.model.User

// DTO -> Domain
fun UserDto.toDomain(): User {
    val parsedRole = when (role?.uppercase()) {
        "TEACHER", "DOCENTE" -> GlifoRole.Teacher
        "ADMIN", "ADMINISTRADOR" -> GlifoRole.Admin
        else -> GlifoRole.Student
    }
    val combinedName = listOfNotNull(firstName, lastName)
        .joinToString(" ")
        .trim()
        .ifEmpty { email.substringBefore("@") }

    return User(
        id = id,
        fullName = combinedName,
        email = email,
        role = parsedRole
    )
}

// DummyAccount -> Domain
fun DummyAccount.toDomain(): User = User(
    id = id,
    fullName = fullName,
    email = email,
    role = role
)
