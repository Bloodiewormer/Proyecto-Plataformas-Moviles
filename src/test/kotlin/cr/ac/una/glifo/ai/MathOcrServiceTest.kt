package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.engine.MathOcrEngine
import cr.ac.una.glifo.ai.engine.MathResult
import cr.ac.una.glifo.ai.engine.RegionCrop
import cr.ac.una.glifo.ai.service.MathOcrService
import cr.ac.una.glifo.ai.service.PageRegionStore
import cr.ac.una.glifo.ai.validation.FormulaEscalationPolicy
import cr.ac.una.glifo.ai.validation.LatexValidator
import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ConflictException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.entity.NotePage
import cr.ac.una.glifo.note.entity.NoteStatus
import cr.ac.una.glifo.note.repository.NotePageRepository
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.transaction.support.TransactionOperations
import tools.jackson.databind.json.JsonMapper
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas UNITARIAS de N1.5 (issue #38): repositorios simulados con Mockito, motor falso en
 * memoria y la compuerta real de #39. Sin base de datos ni llamadas a OpenAI.
 */
class MathOcrServiceTest {

    private class FakeEngine(var result: MathResult, var available: Boolean = true) : MathOcrEngine {
        var calls = 0
        override fun name() = "Fake"
        override fun isAvailable() = available
        override fun recognize(crop: RegionCrop): MathResult {
            calls++
            return result
        }
    }

    private val json = JsonMapper.builder().findAndAddModules().build()
    private lateinit var noteRepository: NoteRepository
    private lateinit var pageRepository: NotePageRepository
    private lateinit var engine: FakeEngine
    private lateinit var service: MathOcrService

    private val owner = User().apply { id = 1L }
    private val note = Note().apply { id = 7L; user = owner; course = cr.ac.una.glifo.course.entity.Course().apply { id = 10L } }
    private val png = RegionCrop(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00), "image/png")

    private fun page(regions: String) = NotePage().apply {
        id = 70L
        pageIndex = 0
        note = this@MathOcrServiceTest.note
        this.regions = regions
    }

    private val pendingRegions = """
        [
          {"id": "r0", "kind": "TEXT", "resolvedAt": "N1", "confidence": 0.93, "isUncertain": false, "text": "Límite"},
          {"id": "r1", "kind": "MATH", "resolvedAt": "UNRESOLVED", "confidence": 0.41, "isUncertain": true, "text": "lim x→a f(x) = L"}
        ]
    """.trimIndent()

    private fun result(latex: String, confidence: Float) = MathResult(latex, confidence, "Fake", "gpt-4o-mini", 2900, 12, 300)

    @BeforeEach
    fun setUp() {
        noteRepository = mock(NoteRepository::class.java)
        pageRepository = mock(NotePageRepository::class.java)
        engine = FakeEngine(result("\\lim_{x \\to a} f(x) = L", 0.92f))
        val store = PageRegionStore(noteRepository, pageRepository, TransactionOperations.withoutTransaction(), json, mock(EntityManager::class.java))
        service = MathOcrService(store, engine, FormulaEscalationPolicy(LatexValidator()), discardingLedger())
        `when`(noteRepository.findById(7L)).thenReturn(Optional.of(note))
        `when`(pageRepository.save(any(NotePage::class.java))).thenAnswer { it.arguments[0] }
    }

    private fun stubPage(page: NotePage, forUpdate: NotePage = page) {
        `when`(pageRepository.findByNoteIdAndPageIndex(7L, 0)).thenReturn(page)
        `when`(pageRepository.findForUpdate(7L, 0)).thenReturn(forUpdate)
    }

    private fun savedRegion(regionId: String): tools.jackson.databind.JsonNode {
        val captor = ArgumentCaptor.forClass(NotePage::class.java)
        verify(pageRepository).save(captor.capture())
        return json.readTree(captor.value.regions).first { it["id"].asString() == regionId }
    }

    @Test
    fun `LaTeX que compila con confianza alta queda en N1_5 y se guarda en la region`() {
        stubPage(page(pendingRegions))

        val response = service.transcribeRegion(7L, 1L, 0, "r1", png)

        assertEquals("KEEP_N1_5", response.status)
        assertEquals("N1_5", response.resolvedAt)
        assertTrue(response.latexCompiles)
        assertFalse(response.cached)
        val region = savedRegion("r1")
        assertEquals("\\lim_{x \\to a} f(x) = L", region["latex"].asString())
        assertEquals("N1_5", region["resolvedAt"].asString())
        assertFalse(region["isUncertain"].asBoolean())
        assertEquals("Fake", region["engine"].asString())
        assertEquals("lim x→a f(x) = L", region["text"].asString())
        assertTrue(region["latexCompiles"].asBoolean())
    }

    @Test
    fun `aceptar una formula sube el nivel de la pagina a N1_5`() {
        val captor = ArgumentCaptor.forClass(NotePage::class.java)
        stubPage(page(pendingRegions).apply { levelReached = "UNRESOLVED" })

        service.transcribeRegion(7L, 1L, 0, "r1", png)

        verify(pageRepository).save(captor.capture())
        assertEquals("N1_5", captor.value.levelReached)
    }

    @Test
    fun `una region ya resuelta por N2 responde 409 sin llamar al motor`() {
        stubPage(page(pendingRegions.replace("\"resolvedAt\": \"UNRESOLVED\", \"confidence\": 0.41", "\"resolvedAt\": \"N2\", \"confidence\": 0.9")))

        val ex = assertThrows<ConflictException> { service.transcribeRegion(7L, 1L, 0, "r1", png) }

        assertEquals("REGION_ALREADY_RESOLVED", ex.code)
        assertEquals(0, engine.calls)
    }

    @Test
    fun `un contador de intentos negativo subido por el cliente no amplia el limite`() {
        engine.result = result("x^2", 0.4f)
        stubPage(page(pendingRegions.replace("\"isUncertain\": true,", "\"isUncertain\": true, \"mathOcrAttempts\": -1000,")))

        service.transcribeRegion(7L, 1L, 0, "r1", png)

        assertEquals(1, savedRegion("r1")["mathOcrAttempts"].asInt())
    }

    @Test
    fun `confianza baja escala a N2 y la region sigue sin resolver`() {
        engine.result = result("x^2", 0.4f)
        stubPage(page(pendingRegions))

        val response = service.transcribeRegion(7L, 1L, 0, "r1", png)

        assertEquals("NEEDS_N2", response.status)
        assertEquals("UNRESOLVED", response.resolvedAt)
        assertEquals(listOf("LOW_CONFIDENCE"), response.escalationReasons)
        val region = savedRegion("r1")
        assertTrue(region["isUncertain"].asBoolean())
        assertNull(region["latex"])
        assertEquals("UNRESOLVED", region["resolvedAt"].asString())
    }

    @Test
    fun `sin matematica legible escala porque el LaTeX vacio no compila`() {
        engine.result = result("", 0f)
        stubPage(page(pendingRegions))

        val response = service.transcribeRegion(7L, 1L, 0, "r1", png)

        assertEquals("NEEDS_N2", response.status)
        assertEquals(listOf("LATEX_DOES_NOT_COMPILE", "LOW_CONFIDENCE"), response.escalationReasons)
    }

    @Test
    fun `una region ya resuelta en N1_5 se devuelve sin llamar al motor`() {
        val resolved = pendingRegions.replace(
            "\"resolvedAt\": \"UNRESOLVED\", \"confidence\": 0.41, \"isUncertain\": true",
            "\"resolvedAt\": \"N1_5\", \"confidence\": 0.9, \"isUncertain\": false, \"latex\": \"x^2\", \"latexCompiles\": true, \"engine\": \"Fake\""
        )
        stubPage(page(resolved))

        val response = service.transcribeRegion(7L, 1L, 0, "r1", png)

        assertTrue(response.cached)
        assertEquals("x^2", response.latex)
        assertEquals(0, engine.calls)
        verify(pageRepository, never()).save(any(NotePage::class.java))
    }

    @Test
    fun `el apunte de otra persona responde 404 sin llamar al motor`() {
        assertThrows<ResourceNotFoundException> { service.transcribeRegion(7L, 2L, 0, "r1", png) }
        assertEquals(0, engine.calls)
    }

    @Test
    fun `una region que no es MATH responde 404`() {
        stubPage(page(pendingRegions))

        assertThrows<ResourceNotFoundException> { service.transcribeRegion(7L, 1L, 0, "r0", png) }
        assertEquals(0, engine.calls)
    }

    @Test
    fun `un archivo que no es imagen se rechaza aunque declare image png`() {
        stubPage(page(pendingRegions))

        assertThrows<BadRequestException> {
            service.transcribeRegion(7L, 1L, 0, "r1", RegionCrop("no es imagen".toByteArray(), "image/png"))
        }
        assertThrows<BadRequestException> {
            service.transcribeRegion(7L, 1L, 0, "r1", RegionCrop(png.bytes, "application/pdf"))
        }
        assertEquals(0, engine.calls)
    }

    @Test
    fun `un id de region repetido en la pagina se rechaza sin llamar al motor`() {
        stubPage(page(pendingRegions.replace("\"id\": \"r0\"", "\"id\": \"r1\"")))

        assertThrows<BadRequestException> { service.transcribeRegion(7L, 1L, 0, "r1", png) }
        assertEquals(0, engine.calls)
    }

    @Test
    fun `tras el limite de intentos responde 409 sin llamar al motor`() {
        stubPage(page(pendingRegions.replace("\"isUncertain\": true,", "\"isUncertain\": true, \"mathOcrAttempts\": 3,")))

        val ex = assertThrows<ConflictException> { service.transcribeRegion(7L, 1L, 0, "r1", png) }

        assertEquals("AI_ATTEMPTS_EXCEEDED", ex.code)
        assertEquals(0, engine.calls)
    }

    @Test
    fun `cada intento se cuenta en la region`() {
        engine.result = result("x^2", 0.4f)
        stubPage(page(pendingRegions))

        service.transcribeRegion(7L, 1L, 0, "r1", png)

        assertEquals(1, savedRegion("r1")["mathOcrAttempts"].asInt())
    }

    @Test
    fun `si otra peticion resolvio la region mientras corria el motor se conserva su resultado`() {
        val resolvedMeanwhile = pendingRegions.replace(
            "\"resolvedAt\": \"UNRESOLVED\", \"confidence\": 0.41, \"isUncertain\": true",
            "\"resolvedAt\": \"N1_5\", \"confidence\": 0.95, \"isUncertain\": false, \"latex\": \"y\", \"latexCompiles\": true"
        )
        stubPage(page(pendingRegions), forUpdate = page(resolvedMeanwhile))

        val response = service.transcribeRegion(7L, 1L, 0, "r1", png)

        assertTrue(response.cached)
        assertEquals("y", response.latex)
        assertEquals("y", savedRegion("r1")["latex"].asString())
    }

    @Test
    fun `un apunte ya reconstruido vuelve a PROCESSING sin content para que IA-01 lo rehaga`() {
        note.status = NoteStatus.READY.name
        note.content = "{}"
        stubPage(page(pendingRegions))

        service.transcribeRegion(7L, 1L, 0, "r1", png)

        assertEquals(NoteStatus.PROCESSING.name, note.status)
        assertNull(note.content)
    }

    @Test
    fun `sin motor configurado responde AI_NOT_CONFIGURED`() {
        engine.available = false
        stubPage(page(pendingRegions))

        val ex = assertThrows<AiProviderException> { service.transcribeRegion(7L, 1L, 0, "r1", png) }

        assertEquals("AI_NOT_CONFIGURED", ex.code)
        assertEquals(0, engine.calls)
    }
}
