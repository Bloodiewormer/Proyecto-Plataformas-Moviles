package cr.ac.una.glifo.domain.model

data class AdminUserRow(
    val initials: String,
    val name: String,
    val email: String,
    val role: String, // "DOCENTE" | "ESTUDIANTE" | "ADMIN" — the real role, kept even while inactive
    val isActive: Boolean
) {
    /** What the badge/chip should show — "INACTIVO" overrides the real role visually. */
    val displayLabel: String get() = if (!isActive) "INACTIVO" else role
}

data class AdminRoleInfo(
    val name: String,
    val userCount: Int,
    val description: String,
    val privileges: List<String>,
    val extraPrivilegeCount: Int,
    val isSensitive: Boolean = false
)
