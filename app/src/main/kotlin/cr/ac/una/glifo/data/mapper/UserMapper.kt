package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.core.database.entity.UserEntity
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

// Entity (Room) -> Domain
fun UserEntity.toDomain(): User {
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

// Domain -> Entity (Room)
fun User.toEntity(token: String? = null): UserEntity {
    val parts = fullName.split(" ", limit = 2)
    val first = parts.getOrNull(0) ?: fullName
    val last = parts.getOrNull(1)

    return UserEntity(
        id = id,
        email = email,
        firstName = first,
        lastName = last,
        role = role.name,
        token = token
    )
}

// DummyAccount -> Domain
fun DummyAccount.toDomain(): User = User(
    id = id,
    fullName = fullName,
    email = email,
    role = role
)
