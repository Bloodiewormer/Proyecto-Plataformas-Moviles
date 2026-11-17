package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.config.OpenAiProperties
import cr.ac.una.glifo.ai.engine.RegionCrop
import cr.ac.una.glifo.ai.service.PageRegionStore
import cr.ac.una.glifo.ai.service.VisionRepairService
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
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.transaction.support.TransactionOperations
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.Optional
import kotlin.math.exp
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas UNITARIAS de N2 / IA-00 (issue #41): repositorios simulados, LlmClient falso que
 * devuelve tokens con logprobs y el LatexValidator real. Sin base de datos ni OpenAI.
 */
class VisionRepairServiceTest {

    private class FakeLlmClient : LlmClient {
        var tokens: List<TokenLogprob> = emptyList()
        var finishReason = "stop"
        val requests = mutableListOf<LlmRequest>()
        override fun completeJson(request: LlmRequest): LlmResult {
            requests.add(request)
            return LlmResult(tokens.joinToString("") { it.token }.trim(), "gpt-4o-mini", 9000, tokens.size, 800, tokens, finishReason)
        }
    }

    private val json = JsonMapper.builder().findAndAddModules().build()
    private lateinit var noteRepository: NoteRepository
    private lateinit var pageRepository: NotePageRepository
    private lateinit var llm: FakeLlmClient
    private lateinit var store: PageRegionStore
    private lateinit var service: VisionRepairService

    private val owner = User().apply { id = 1L }
    private val note = Note().apply { id = 7L; user = owner; course = cr.ac.una.glifo.course.entity.Course().apply { id = 10L } }
    private val png = RegionCrop(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00), "image/png")

    private var regions = """
        [
          {"id": "r0", "kind": "TEXT", "resolvedAt": "N1", "confidence": 0.93, "isUncertain": false, "text": "Límite"},
          {"id": "r1", "kind": "TEXT", "resolvedAt": "UNRESOLVED", "confidence": 0.3, "isUncertain": true, "text": "pa7a t0do"},
          {"id": "r2", "kind": "MATH", "resolvedAt": "UNRESOLVED", "confidence": 0.4, "isUncertain": true, "text": "lim"},
          {"id": "r3", "kind": "DRAWING", "resolvedAt": "UNRESOLVED", "confidence": 0.0, "isUncertain": true, "text": "x 0"}
        ]
    """.trimIndent()

    private fun page() = NotePage().apply {
        id = 70L
        pageIndex = 0
        levelReached = "N1"
        note = this@VisionRepairServiceTest.note
        this.regions = this@VisionRepairServiceTest.regions
    }

    private fun tokens(vararg pieces: Pair<String, Double>) = pieces.map { TokenLogprob(it.first, it.second) }

    @BeforeEach
    fun setUp() {
        noteRepository = mock(NoteRepository::class.java)
        pageRepository = mock(NotePageRepository::class.java)
        llm = FakeLlmClient()
        store = PageRegionStore(noteRepository, pageRepository, TransactionOperations.withoutTransaction(), json, mock(EntityManager::class.java))
        service = VisionRepairService(store, llm, OpenAiProperties(apiKey = "key"), LatexValidator(), discardingLedger())
        `when`(noteRepository.findById(7L)).thenReturn(Optional.of(note))
        `when`(pageRepository.findByNoteIdAndPageIndex(7L, 0)).thenAnswer { page() }
        `when`(pageRepository.findForUpdate(7L, 0)).thenAnswer { page() }
        `when`(pageRepository.save(any(NotePage::class.java))).thenAnswer { it.arguments[0] }
    }

    private fun saved(): Pair<NotePage, Map<String, JsonNode>> {
        val captor = ArgumentCaptor.forClass(NotePage::class.java)
        verify(pageRepository).save(captor.capture())
        return captor.value to json.readTree(captor.value.regions).associateBy { it["id"].asString() }
    }

    @Test
    fun `las regiones de la pagina viajan en una sola llamada y las seguras quedan en N2`() {
        llm.tokens = tokens(
            "r1|" to 0.0, "para" to -0.05, " todo" to -0.05, "\n" to 0.0,
            "r2|" to 0.0, "\\frac{a}{b}" to -0.1, "\n" to 0.0,
            "r3|" to 0.0, "Una" to -0.1, " parábola" to -0.1
        )

        val response = service.repairPage(7L, 1L, 0, linkedMapOf("r1" to png, "r2" to png, "r3" to png))

        assertEquals(1, llm.requests.size)
        assertEquals(3, llm.requests.single().images.size)
        assertEquals(3, response.repaired)
        assertEquals("N2", response.levelReached)
        val (page, byId) = saved()
        assertEquals("N2", page.levelReached)
        assertEquals("para todo", byId.getValue("r1")["text"].asString())
        assertEquals("\\frac{a}{b}", byId.getValue("r2")["latex"].asString())
        assertEquals("Una parábola", byId.getValue("r3")["caption"].asString())
        assertTrue(byId.getValue("r2")["latexCompiles"].asBoolean())
        assertEquals("N2", byId.getValue("r2")["resolvedAt"].asString())
        assertFalse(byId.getValue("r3")["isUncertain"].asBoolean())
        assertEquals("N1", byId.getValue("r0")["resolvedAt"].asString())
    }

    @Test
    fun `la confianza de cada region sale solo de sus propios tokens`() {
        val lines = VisionRepairService.parseLines(
            "",
            tokens("r1|" to -5.0, "x" to -0.2, "\n" to -5.0, "r2|" to -5.0, "y" to -1.0)
        )

        assertEquals(listOf("r1", "r2"), lines.map { it.regionId })
        assertEquals(exp(-0.2).toFloat(), lines[0].confidence, 1e-6f)
        assertEquals(exp(-1.0).toFloat(), lines[1].confidence, 1e-6f)
    }

    @Test
    fun `un caracter UTF-8 repartido entre dos tokens se reconstruye con sus bytes`() {
        val a = "á".toByteArray(Charsets.UTF_8).map { it.toInt() and 0xFF }
        val lines = VisionRepairService.parseLines(
            "",
            listOf(
                TokenLogprob("r1|", 0.0),
                TokenLogprob("bytes:\\xc3", -0.1, listOf(a[0])),
                TokenLogprob("bytes:\\xa1", -0.1, listOf(a[1])),
                TokenLogprob("rea", -0.1)
            )
        )

        assertEquals("área", lines.single().value)
        assertEquals(exp(-0.1).toFloat(), lines.single().confidence, 1e-6f)
    }

    @Test
    fun `el LaTeX aceptado se guarda sin delimitadores y la pagina sube desde UNRESOLVED`() {
        `when`(pageRepository.findForUpdate(7L, 0)).thenAnswer { page().apply { levelReached = "UNRESOLVED" } }
        llm.tokens = tokens("r2|" to 0.0, "\$x^2\$" to -0.01)

        val response = service.repairPage(7L, 1L, 0, linkedMapOf("r2" to png))

        assertEquals("x^2", response.regions.single().value)
        val (page, byId) = saved()
        assertEquals("x^2", byId.getValue("r2")["latex"].asString())
        assertEquals("N2", page.levelReached)
    }

    @Test
    fun `baja confianza, LaTeX que no compila o ilegible quedan inciertos y no se escriben`() {
        llm.tokens = tokens(
            "r1|" to 0.0, "quiza" to -2.0, "\n" to 0.0,
            "r2|" to 0.0, "\\sen x" to -0.01, "\n" to 0.0,
            "r3|" to 0.0, "ILEGIBLE" to -0.01
        )

        val response = service.repairPage(7L, 1L, 0, linkedMapOf("r1" to png, "r2" to png, "r3" to png))

        assertEquals(0, response.repaired)
        assertEquals(3, response.uncertain)
        assertTrue(response.regions.all { it.value == null && it.resolvedAt == "UNRESOLVED" })
        val (page, byId) = saved()
        assertEquals("N1", page.levelReached)
        assertEquals("pa7a t0do", byId.getValue("r1")["text"].asString())
        assertNull(byId.getValue("r2")["latex"])
        assertNull(byId.getValue("r3")["caption"])
    }

    @Test
    fun `una region sin linea en la respuesta queda incierta`() {
        llm.tokens = tokens("r1|" to 0.0, "para todo" to -0.01)

        val response = service.repairPage(7L, 1L, 0, linkedMapOf("r1" to png, "r3" to png))

        assertEquals(listOf("N2", "UNRESOLVED"), response.regions.map { it.resolvedAt })
    }

    @Test
    fun `un recorte de una region inexistente o con id invalido se rechaza sin llamar a la IA`() {
        assertThrows<BadRequestException> { service.repairPage(7L, 1L, 0, mapOf("r9" to png)) }
        assertThrows<BadRequestException> { service.repairPage(7L, 1L, 0, mapOf("a|b" to png)) }
        assertThrows<BadRequestException> { service.repairPage(7L, 1L, 0, mapOf(" r1" to png)) }
        assertTrue(llm.requests.isEmpty())
    }

    @Test
    fun `reintentar un lote ya resuelto devuelve su estado actual sin llamar a la IA`() {
        val response = service.repairPage(7L, 1L, 0, mapOf("r0" to png))

        assertEquals("Límite", response.regions.single().value)
        assertEquals("N1", response.regions.single().resolvedAt)
        assertEquals(0, response.repaired)
        assertTrue(llm.requests.isEmpty())
    }

    @Test
    fun `la respuesta expone isUncertain con ese nombre`() {
        llm.tokens = tokens("r1|" to 0.0, "para todo" to -0.01)

        val body = json.writeValueAsString(service.repairPage(7L, 1L, 0, linkedMapOf("r1" to png)))

        assertTrue(body.contains("\"isUncertain\":false"), body)
    }

    @Test
    fun `sin recortes, con demasiados o con un archivo invalido se rechaza sin llamar a la IA`() {
        assertThrows<BadRequestException> { service.repairPage(7L, 1L, 0, emptyMap()) }
        val tooMany = (0..VisionRepairService.MAX_CROPS).associate { "x$it" to png }
        assertThrows<BadRequestException> { service.repairPage(7L, 1L, 0, tooMany) }
        assertThrows<BadRequestException> {
            service.repairPage(7L, 1L, 0, mapOf("r1" to RegionCrop("hola".toByteArray(), "image/png")))
        }
        assertTrue(llm.requests.isEmpty())
    }

    @Test
    fun `una salida truncada no acepta ninguna region`() {
        llm.tokens = tokens("r1|" to 0.0, "para todo" to -0.01)
        llm.finishReason = "length"

        val response = service.repairPage(7L, 1L, 0, linkedMapOf("r1" to png))

        assertEquals(0, response.repaired)
        assertEquals(1, saved().second.getValue("r1")["visionRepairAttempts"].asInt())
    }

    @Test
    fun `un id repetido o una region sin intentos restantes se rechaza sin llamar a la IA`() {
        val original = regions
        regions = original.replace("\"id\": \"r0\"", "\"id\": \"r1\"")
        assertThrows<BadRequestException> { service.repairPage(7L, 1L, 0, mapOf("r1" to png)) }

        regions = original.replace("\"id\": \"r1\", \"kind\": \"TEXT\"", "\"id\": \"r1\", \"visionRepairAttempts\": 3, \"kind\": \"TEXT\"")
        val ex = assertThrows<ConflictException> { service.repairPage(7L, 1L, 0, mapOf("r1" to png)) }
        assertEquals("AI_ATTEMPTS_EXCEEDED", ex.code)
        assertTrue(llm.requests.isEmpty())
    }

    @Test
    fun `una region resuelta por otra peticion mientras corria la IA no se sobrescribe`() {
        llm.tokens = tokens("r1|" to 0.0, "para todo" to -0.01)
        val resolvedMeanwhile = regions.replace("\"resolvedAt\": \"UNRESOLVED\", \"confidence\": 0.3, \"isUncertain\": true", "\"resolvedAt\": \"N2\", \"confidence\": 0.9, \"isUncertain\": false")
        `when`(pageRepository.findForUpdate(7L, 0)).thenAnswer { page().apply { this.regions = resolvedMeanwhile } }

        val response = service.repairPage(7L, 1L, 0, linkedMapOf("r1" to png))

        assertEquals(0, response.repaired)
        assertEquals("pa7a t0do", response.regions.single().value)
        assertEquals("N2", response.regions.single().resolvedAt)
        assertFalse(response.regions.single().isUncertain)
        assertEquals("pa7a t0do", saved().second.getValue("r1")["text"].asString())
    }

    @Test
    fun `reparar un apunte ya reconstruido lo devuelve a PROCESSING sin content`() {
        note.status = NoteStatus.READY.name
        note.content = "{}"
        llm.tokens = tokens("r1|" to 0.0, "para todo" to -0.01)

        service.repairPage(7L, 1L, 0, linkedMapOf("r1" to png))

        assertEquals(NoteStatus.PROCESSING.name, note.status)
        assertNull(note.content)
    }

    @Test
    fun `el apunte de otra persona responde 404`() {
        assertThrows<ResourceNotFoundException> { service.repairPage(7L, 2L, 0, mapOf("r1" to png)) }
        assertTrue(llm.requests.isEmpty())
    }

    @Test
    fun `sin api key responde AI_NOT_CONFIGURED`() {
        val unconfigured = VisionRepairService(store, llm, OpenAiProperties(apiKey = ""), LatexValidator(), discardingLedger())

        val ex = assertThrows<AiProviderException> { unconfigured.repairPage(7L, 1L, 0, mapOf("r1" to png)) }

        assertEquals("AI_NOT_CONFIGURED", ex.code)
        assertTrue(llm.requests.isEmpty())
    }

    @Test
    fun `un recorte etiquetado DRAWING con formula se reclasifica a MATH y guarda su LaTeX`() {
        regions = """
            [
              {"id": "r3", "kind": "DRAWING", "resolvedAt": "UNRESOLVED", "confidence": 0.31, "isUncertain": true, "text": "A-"},
              {"id": "r4", "kind": "DRAWING", "resolvedAt": "UNRESOLVED", "confidence": 0.35, "isUncertain": true, "text": "U-"}
            ]
        """.trimIndent()
        llm.tokens = tokens(
            "r3|" to 0.0, "MATH|" to 0.0, "\\lim_{x \\to \\infty} \\frac{a^3}{x^2+a^2} = 0" to -0.05, "\n" to 0.0,
            "r4|" to 0.0, "\\int_{0}^{\\infty} x^2\\,dx" to -0.05
        )

        val response = service.repairPage(7L, 1L, 0, linkedMapOf("r3" to png, "r4" to png))

        assertEquals(2, response.repaired)
        assertEquals(listOf("MATH", "MATH"), response.regions.map { it.kind })
        val (_, byId) = saved()
        assertEquals("MATH", byId.getValue("r3")["kind"].asString())
        assertEquals("\\lim_{x \\to \\infty} \\frac{a^3}{x^2+a^2} = 0", byId.getValue("r3")["latex"].asString())
        assertEquals("MATH", byId.getValue("r4")["kind"].asString())
        assertEquals("\\int_{0}^{\\infty} x^2\\,dx", byId.getValue("r4")["latex"].asString())
    }

    @Test
    fun `un dibujo real con descripcion breve de confianza moderada se acepta en N2`() {
        llm.tokens = tokens("r3|" to 0.0, "DRAWING|" to 0.0, "Curva de campana simétrica respecto al eje y" to -0.65)

        val response = service.repairPage(7L, 1L, 0, linkedMapOf("r3" to png))

        assertEquals(1, response.repaired)
        assertEquals("N2", response.regions.single().resolvedAt)
        assertEquals("DRAWING", response.regions.single().kind)
        assertEquals("Curva de campana simétrica respecto al eje y", saved().second.getValue("r3")["caption"].asString())
    }

    @Test
    fun `una pagina con 16 regiones pendientes se repara en una sola llamada`() {
        regions = (1..16).joinToString(prefix = "[", postfix = "]") { i ->
            """{"id": "r$i", "kind": "TEXT", "resolvedAt": "UNRESOLVED", "confidence": 0.4, "isUncertain": true, "text": "x$i"}"""
        }
        val tokenList = (1..16).flatMap { i ->
            listOf("r$i|" to 0.0, "TEXT|" to 0.0, "línea $i" to -0.05) + if (i < 16) listOf("\n" to 0.0) else emptyList()
        }
        llm.tokens = tokens(*tokenList.toTypedArray())
        val crops = (1..16).associate { "r$it" to png }

        val response = service.repairPage(7L, 1L, 0, crops)

        assertEquals(1, llm.requests.size)
        assertEquals(16, llm.requests.single().images.size)
        assertEquals(16, response.repaired)
    }

    @Test
    fun `una formula o texto legible de confianza intermedia conserva su transcripcion como incierta`() {
        llm.tokens = tokens(
            "r1|" to 0.0, "TEXT|" to 0.0, "por el método de los discos" to -0.8, "\n" to 0.0,
            "r2|" to 0.0, "MATH|" to 0.0, "\\int_{0}^{\\pi/2} a^3 \\cos^2 t\\,dt" to -0.5
        )

        val response = service.repairPage(7L, 1L, 0, linkedMapOf("r1" to png, "r2" to png))

        assertEquals(0, response.repaired)
        assertEquals(2, response.uncertain)
        assertEquals("por el método de los discos", response.regions[0].value)
        assertEquals("\\int_{0}^{\\pi/2} a^3 \\cos^2 t\\,dt", response.regions[1].value)
        val (_, byId) = saved()
        assertEquals("por el método de los discos", byId.getValue("r1")["text"].asString())
        assertTrue(byId.getValue("r1")["isUncertain"].asBoolean())
        assertEquals("\\int_{0}^{\\pi/2} a^3 \\cos^2 t\\,dt", byId.getValue("r2")["latex"].asString())
        assertTrue(byId.getValue("r2")["isUncertain"].asBoolean())
    }
}
