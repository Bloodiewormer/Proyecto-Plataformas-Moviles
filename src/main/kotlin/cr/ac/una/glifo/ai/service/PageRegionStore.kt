package cr.ac.una.glifo.ai.service

import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ConflictException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.note.entity.NotePage
import cr.ac.una.glifo.note.entity.NoteStatus
import cr.ac.una.glifo.note.repository.NotePageRepository
import cr.ac.una.glifo.note.repository.NoteRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionOperations
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode

/**
 * Acceso compartido de N1.5 y N2 a las regiones de una página propia. Las escrituras vacían el
 * contexto de persistencia (open-in-view lo comparte con la lectura inicial) y releen la página
 * con bloqueo dentro de una transacción corta (la IA se llama antes, fuera de ella) y, si
 * cambian regiones de un apunte ya reconstruido, invalidan su contenido para que IA-01 lo rehaga.
 */
@Component
class PageRegionStore(
    private val noteRepository: NoteRepository,
    private val notePageRepository: NotePageRepository,
    private val transactions: TransactionOperations,
    private val jsonMapper: JsonMapper,
    private val entityManager: EntityManager
) {

    fun ownedPage(noteId: Long, userId: Long, pageIndex: Int): NotePage {
        val note = noteRepository.findById(noteId).orElseThrow { ResourceNotFoundException("Apunte no encontrado") }
        if (note.user.id != userId) throw ResourceNotFoundException("Apunte no encontrado")
        return notePageRepository.findByNoteIdAndPageIndex(noteId, pageIndex)
            ?: throw ResourceNotFoundException("Página no encontrada")
    }

    fun regions(regionsJson: String?): List<ObjectNode> {
        val root = jsonMapper.readTree(regionsJson ?: return emptyList())
        return if (root.isArray) root.filterIsInstance<ObjectNode>() else emptyList()
    }

    /** La única región con [regionId]; null si no existe y 400 si el id está repetido en la página. */
    fun uniqueRegion(regions: List<ObjectNode>, regionId: String): ObjectNode? {
        if (!REGION_ID.matches(regionId)) throw BadRequestException("El id de la región no es válido")
        val matches = regions.filter { it.get("id")?.takeIf { id -> id.isString }?.asString() == regionId }
        if (matches.size > 1) throw BadRequestException("La región $regionId está repetida en la página")
        return matches.firstOrNull()
    }

    fun attempts(region: JsonNode, attemptsField: String): Int = region.path(attemptsField).asInt(0).coerceAtLeast(0)

    fun requireAttemptsLeft(region: JsonNode, attemptsField: String) {
        if (attempts(region, attemptsField) >= MAX_ATTEMPTS) {
            throw ConflictException(
                "La región ${region.path("id").asString("")} alcanzó el límite de $MAX_ATTEMPTS intentos",
                "AI_ATTEMPTS_EXCEEDED"
            )
        }
    }

    /**
     * Relee la página bloqueada y aplica [change] sobre sus regiones. [change] devuelve true si
     * modificó contenido que IA-01 usa; en ese caso un apunte READY vuelve a PROCESSING sin content.
     */
    fun <T : Any> update(noteId: Long, pageIndex: Int, change: (List<ObjectNode>, NotePage) -> Pair<T, Boolean>): T =
        transactions.execute {
            entityManager.clear()
            val page = notePageRepository.findForUpdate(noteId, pageIndex)
                ?: throw ResourceNotFoundException("Página no encontrada")
            val root = jsonMapper.readTree(page.regions ?: "[]")
            val regions = if (root.isArray) root.filterIsInstance<ObjectNode>() else emptyList()
            val (result, contentChanged) = change(regions, page)
            page.regions = jsonMapper.writeValueAsString(root)
            notePageRepository.save(page)
            if (contentChanged) {
                val note = noteRepository.findById(noteId).orElseThrow { ResourceNotFoundException("Apunte no encontrado") }
                if (note.status == NoteStatus.READY.name) {
                    note.content = null
                    note.contentGeneratedAt = null
                    note.status = NoteStatus.PROCESSING.name
                    noteRepository.save(note)
                }
            }
            result
        }

    companion object {
        const val MAX_ATTEMPTS = 3
        private val REGION_ID = Regex("[A-Za-z0-9_-]{1,64}")
        private val LEVEL_ORDER = listOf("UNRESOLVED", "N0", "N1", "N1_5", "N2", "N3")

        /** Sube el nivel alcanzado de la página si [level] es mayor; nunca lo baja. */
        fun raiseLevel(page: NotePage, level: String) {
            if (LEVEL_ORDER.indexOf(page.levelReached) < LEVEL_ORDER.indexOf(level)) page.levelReached = level
        }
    }
}
