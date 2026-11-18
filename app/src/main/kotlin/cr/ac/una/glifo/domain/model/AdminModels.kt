package cr.ac.una.glifo.domain.model

/** Una cuenta tal como la ve el admin en la pantalla Usuarios. */
data class AdminUser(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: GlifoRole
)

/** Un rol con sus privilegios y cuántas cuentas lo tienen (pantalla Roles). */
data class AdminRole(
    val name: String,
    val description: String?,
    val privileges: List<String>,
    val userCount: Int
)
