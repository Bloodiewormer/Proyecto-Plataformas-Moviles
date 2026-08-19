package cr.ac.una.glifo.feature.home.domain

enum class UserRole {
    STUDENT,
    TEACHER;

    companion object {
        /**
         * Deriva el rol principal del usuario a partir de su colección de roles.
         * Si no hay rol asignado, es nulo o es desconocido, aplica un fallback controlado y seguro a STUDENT.
         */
        fun fromRoles(roles: Collection<String>?): UserRole {
            if (roles.isNullOrEmpty()) {
                return STUDENT
            }

            val upperRoles = roles.map { it.uppercase().trim() }

            return when {
                upperRoles.any { it == "ROLE_TEACHER" || it == "DOCENTE" || it == "TEACHER" || it == "PROFESOR" } -> TEACHER
                upperRoles.any { it == "ROLE_STUDENT" || it == "ESTUDIANTE" || it == "STUDENT" || it == "ALUMNO" } -> STUDENT
                else -> STUDENT // Fallback seguro y controlado
            }
        }
    }
}
