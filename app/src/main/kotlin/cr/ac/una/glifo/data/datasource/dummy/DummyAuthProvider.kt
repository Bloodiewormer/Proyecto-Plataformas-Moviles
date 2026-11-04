package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.domain.model.GlifoRole

/**
 * Cuenta de demostración mostrada como pista rápida en la pantalla de inicio de sesión.
 */
data class DummyAccount(
    val id: Long,
    val fullName: String,
    val email: String,
    val password: String,
    val role: GlifoRole
)

/**
 * Mantiene únicamente [demoAccounts] para los botones de acceso rápido de prueba en LoginScreen.
 */
object DummyAuthProvider {

    /** Credential hints shown on the login screen to make testing the 3 roles trivial. */
    val demoAccounts: List<DummyAccount> = listOf(
        DummyAccount(
            id = 1L,
            fullName = "Brandon Brenes",
            email = "estudiante@est.una.ac.cr",
            password = "Estudiante123",
            role = GlifoRole.Student
        ),
        DummyAccount(
            id = 2L,
            fullName = "Maikol Guzmán",
            email = "docente@una.ac.cr",
            password = "Docente123",
            role = GlifoRole.Teacher
        ),
        DummyAccount(
            id = 3L,
            fullName = "Admin Glifo",
            email = "admin@una.ac.cr",
            password = "Admin1234",
            role = GlifoRole.Admin
        )
    )
}
