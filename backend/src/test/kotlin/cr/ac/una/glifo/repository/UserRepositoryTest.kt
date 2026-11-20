package cr.ac.una.glifo.repository

import cr.ac.una.glifo.user.entity.User
import cr.ac.una.glifo.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Insert, find, update y delete de usuarios con UserRepository contra PostgreSQL (glifo_test).
 * Cada prueba se revierte al terminar (@Transactional), así no deja datos en la base.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var entityManager: EntityManager

    private fun newUser(email: String) = User().apply {
        this.email = email
        passwordHash = "hash-de-prueba"
        firstName = "Ana"
        lastName = "Mora"
    }

    /** Manda los cambios a PostgreSQL y vacía la memoria de Hibernate: el siguiente find lee de la base. */
    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    fun `insert guarda el usuario y le asigna id`() {
        val saved = userRepository.save(newUser("ana@est.una.ac.cr"))
        flushAndClear()

        assertTrue(saved.id > 0)
        assertTrue(userRepository.existsById(saved.id))
    }

    @Test
    fun `find por email trae el usuario guardado`() {
        userRepository.save(newUser("ana@est.una.ac.cr"))
        flushAndClear()

        val found = userRepository.findByEmail("ana@est.una.ac.cr")

        assertNotNull(found)
        assertEquals("Ana", found.firstName)
    }

    @Test
    fun `find por email inexistente devuelve null`() {
        assertNull(userRepository.findByEmail("nadie@una.ac.cr"))
        assertFalse(userRepository.existsByEmail("nadie@una.ac.cr"))
    }

    @Test
    fun `update cambia el nombre del usuario`() {
        val saved = userRepository.save(newUser("ana@est.una.ac.cr"))
        flushAndClear()

        val user = userRepository.findById(saved.id).orElseThrow()
        user.firstName = "Andrea"
        userRepository.save(user)
        flushAndClear()

        assertEquals("Andrea", userRepository.findById(saved.id).orElseThrow().firstName)
    }

    @Test
    fun `delete elimina el usuario`() {
        val saved = userRepository.save(newUser("ana@est.una.ac.cr"))
        flushAndClear()

        userRepository.deleteById(saved.id)
        flushAndClear()

        assertFalse(userRepository.findById(saved.id).isPresent)
    }
}
