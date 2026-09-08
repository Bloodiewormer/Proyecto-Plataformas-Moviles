package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.domain.model.AdminRoleInfo
import cr.ac.una.glifo.domain.model.AdminUserRow

object DummyAdminProvider {

    private val users = mutableListOf(
        AdminUserRow("MG", "Maikol Guzmán", "docente@una.ac.cr", "DOCENTE", isActive = true),
        AdminUserRow("BB", "Brandon Brenes", "estudiante@est.una.ac.cr", "ESTUDIANTE", isActive = true),
        AdminUserRow("DG", "David González", "dgonzalez@est.una.ac.cr", "ESTUDIANTE", isActive = true),
        AdminUserRow("FU", "Felipe Ugalde", "fugalde@est.una.ac.cr", "ESTUDIANTE", isActive = true),
        AdminUserRow("KJ", "Keneth Jara", "kjara@est.una.ac.cr", "ESTUDIANTE", isActive = false)
    )

    fun getUsers(): List<AdminUserRow> = users

    fun findUser(name: String): AdminUserRow = users.find { it.name == name } ?: users[2]

    fun getRecentSignups(): List<AdminUserRow> = users.takeLast(2)

    fun addUser(name: String, email: String, role: String) {
        val initials = name.trim().split(" ").filter { it.isNotBlank() }
            .take(2).map { it.first().uppercaseChar() }.joinToString("").ifEmpty { "??" }
        users.add(AdminUserRow(initials, name.trim(), email.trim(), role, isActive = true))
    }

    fun setActive(name: String, active: Boolean) {
        val index = users.indexOfFirst { it.name == name }
        if (index >= 0) users[index] = users[index].copy(isActive = active)
    }

    fun getRoles(): List<AdminRoleInfo> = listOf(
        AdminRoleInfo(
            "Estudiante", 131, "Captura, revisa y estudia sobre sus propios apuntes.",
            listOf("note:create", "note:read:own", "coverage:read:own"), extraPrivilegeCount = 3
        ),
        AdminRoleInfo(
            "Docente", 9, "Publica temario y glosario; ve brechas agregadas del grupo.",
            listOf("course:create", "syllabus:publish", "glossary:write"), extraPrivilegeCount = 6
        ),
        AdminRoleInfo(
            "Administrador", 2, "Usuarios, roles y estado del sistema.",
            listOf("user:manage", "role:assign", "system:read"), extraPrivilegeCount = 11, isSensitive = true
        )
    )

    fun getPrivilegesFor(roleLabel: String): List<String> = when (roleLabel.lowercase()) {
        "estudiante" -> listOf("note:create", "note:read:own", "coverage:read:own", "study:attempt", "course:join", "glossary:suggest")
        "docente" -> listOf("course:create", "syllabus:publish", "glossary:write", "coverage:read:group", "gaps:read", "ai_cost:read:course")
        else -> listOf("user:manage", "role:assign", "system:read", "ai_cost:read:system", "course:manage:any", "sync:read:any")
    }
}
