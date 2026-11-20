package cr.ac.una.glifo

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Levanta la aplicación completa (controllers, servicios, repositorios y la conexión a
 * PostgreSQL). Si la base "glifo" no existe o las credenciales de application.properties
 * no coinciden, este test falla: es la primera prueba de que el backend arranca.
 */
@SpringBootTest
@ActiveProfiles("test")
class GlifoApplicationTests {

    @Test
    fun contextLoads() {
    }
}
