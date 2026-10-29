package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.service.PageRegionStore
import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionSynchronizationManager
import kotlin.test.assertEquals

/**
 * Integración de PageRegionStore con Hibernate y PostgreSQL. Reproduce open-in-view: un mismo
 * EntityManager ligado al hilo hace la lectura inicial y la escritura bloqueada. Si la página
 * cambia entre ambas (otra petición), la escritura debe ver la versión de la base, no la cacheada.
 */
@SpringBootTest
@ActiveProfiles("test")
class PageRegionStoreIntegrationTest {

    @Autowired lateinit var store: PageRegionStore
    @Autowired lateinit var entityManagerFactory: EntityManagerFactory
    @Autowired lateinit var jdbc: JdbcTemplate

    @Test
    fun `con open-in-view la escritura bloqueada relee las regiones de la base`() {
        val (noteId, userId) = jdbc.queryForObject(
            "SELECT n.id, n.user_id FROM notes n JOIN note_pages p ON p.note_id = n.id WHERE p.page_index = 0 ORDER BY n.id LIMIT 1"
        ) { rs, _ -> rs.getLong(1) to rs.getLong(2) }!!
        val original = jdbc.queryForObject(
            "SELECT CAST(regions AS text) FROM note_pages WHERE note_id = ? AND page_index = 0", String::class.java, noteId
        )

        val entityManager = entityManagerFactory.createEntityManager()
        TransactionSynchronizationManager.bindResource(entityManagerFactory, EntityManagerHolder(entityManager))
        try {
            store.ownedPage(noteId, userId, 0)
            jdbc.update(
                "UPDATE note_pages SET regions = CAST(? AS jsonb) WHERE note_id = ? AND page_index = 0",
                """[{"id": "externo", "kind": "TEXT", "resolvedAt": "N1"}]""", noteId
            )

            val seen = store.update(noteId, 0) { regions, _ -> regions.map { it.path("id").asString() } to false }

            assertEquals(listOf("externo"), seen)
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory)
            entityManager.close()
            jdbc.update("UPDATE note_pages SET regions = CAST(? AS jsonb) WHERE note_id = ? AND page_index = 0", original, noteId)
        }
    }
}
