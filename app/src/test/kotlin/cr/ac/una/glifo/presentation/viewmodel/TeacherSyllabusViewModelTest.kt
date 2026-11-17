package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlossaryEntry
import cr.ac.una.glifo.domain.model.GlossarySuggestion
import cr.ac.una.glifo.domain.model.SyllabusDocument
import cr.ac.una.glifo.domain.model.SyllabusTopicRow
import cr.ac.una.glifo.domain.model.SyllabusUnitRow
import cr.ac.una.glifo.domain.model.TeacherGapTopic
import cr.ac.una.glifo.domain.repository.TeacherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherSyllabusViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeTeacherRepository : TeacherRepository {
        var syllabusResult: Result<SyllabusDocument> = Result.Success(
            SyllabusDocument(
                courseId = 10L,
                fileName = null,
                sizeBytes = null,
                formattedSize = null,
                parsedAtDisplay = null,
                syllabusVersion = 0,
                units = emptyList()
            )
        )
        var uploadResult: Result<SyllabusDocument> = Result.Failure(AppError.Unknown)
        var publishResult: Result<Unit> = Result.Success(Unit)
        var addTopicResult: Result<Unit> = Result.Success(Unit)

        var lastUploadedFileName: String? = null
        var lastUploadedSize: Int? = null
        var lastPublishedTopics: List<String>? = null
        var lastPublishedSourceUri: String? = "UNSET"

        override suspend fun getSyllabus(): Result<SyllabusDocument> = syllabusResult

        override suspend fun uploadSyllabusPdf(fileName: String, pdfBytes: ByteArray): Result<SyllabusDocument> {
            lastUploadedFileName = fileName
            lastUploadedSize = pdfBytes.size
            return uploadResult
        }

        override suspend fun addSyllabusTopic(label: String): Result<Unit> = addTopicResult

        override suspend fun publishSyllabus(topics: List<String>, sourceUri: String?): Result<Unit> {
            lastPublishedTopics = topics
            lastPublishedSourceUri = sourceUri
            return publishResult
        }

        override suspend fun getGlossaryEntries(): Result<List<GlossaryEntry>> = Result.Success(emptyList())
        override suspend fun addGlossaryEntry(term: String, notation: String, isSymbol: Boolean): Result<Unit> = Result.Success(Unit)
        override suspend fun getGlossarySuggestions(): Result<List<GlossarySuggestion>> = Result.Success(emptyList())
        override suspend fun resolveGlossarySuggestion(suggestionId: Long, approve: Boolean): Result<Unit> = Result.Success(Unit)
        override suspend fun getSyllabusUnits(): Result<List<SyllabusUnitRow>> = Result.Success(emptyList())
        override suspend fun getGaps(): Result<List<TeacherGapTopic>> = Result.Success(emptyList())
    }

    @Test
    fun `load emits empty syllabus state when backend has no document or topics`() = runTest(dispatcher) {
        val repo = FakeTeacherRepository()
        val vm = TeacherSyllabusViewModel(repo)
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertNull(data.fileName)
        assertNull(data.formattedSize)
        assertNull(data.parsedAtDisplay)
        assertTrue(data.units.isEmpty())
        assertFalse(data.isPublished)
        assertFalse(data.hasTopics)
    }

    @Test
    fun `load emits saved syllabus metadata without inventing unknown size`() = runTest(dispatcher) {
        val repo = FakeTeacherRepository().apply {
            syllabusResult = Result.Success(
                SyllabusDocument(
                    courseId = 10L,
                    fileName = "programa-eif411.pdf",
                    sizeBytes = null,
                    formattedSize = null,
                    parsedAtDisplay = "16/11/2026",
                    syllabusVersion = 1,
                    units = listOf(
                        SyllabusUnitRow("U1 · Intro", 1, listOf(SyllabusTopicRow("1.1 Bases", false)))
                    )
                )
            )
        }
        val vm = TeacherSyllabusViewModel(repo)
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertEquals("programa-eif411.pdf", data.fileName)
        assertNull(data.formattedSize)
        assertEquals("16/11/2026", data.parsedAtDisplay)
        assertEquals(1, data.units.size)
        assertTrue(data.isPublished)
        assertTrue(data.hasTopics)
    }

    @Test
    fun `load emits UiState Error when repository fails`() = runTest(dispatcher) {
        val repo = FakeTeacherRepository().apply {
            syllabusResult = Result.Failure(AppError.NoConnection)
        }
        val vm = TeacherSyllabusViewModel(repo)
        advanceUntilIdle()

        assertEquals(UiState.Error(AppError.NoConnection), vm.state.value)
    }

    @Test
    fun `uploadSyllabusPdf happy path shows file name, picked size fallback, parsed date and units`() = runTest(dispatcher) {
        val repo = FakeTeacherRepository().apply {
            uploadResult = Result.Success(
                SyllabusDocument(
                    courseId = 10L,
                    fileName = "moviles.pdf",
                    sizeBytes = null,
                    formattedSize = null,
                    parsedAtDisplay = "16/11/2026",
                    syllabusVersion = 1,
                    units = listOf(
                        SyllabusUnitRow("U1 · Compose", 2, listOf(SyllabusTopicRow("1.1 Estado", false), SyllabusTopicRow("1.2 Navegación", false)))
                    )
                )
            )
        }
        val vm = TeacherSyllabusViewModel(repo)
        advanceUntilIdle()

        vm.addTopic("1.0 Borrador previo")
        vm.uploadSyllabusPdf("moviles.pdf", ByteArray(2048) { 1 })
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertEquals("moviles.pdf", repo.lastUploadedFileName)
        assertEquals(2048, repo.lastUploadedSize)
        assertEquals("moviles.pdf", data.fileName)
        assertEquals("2.0 KB", data.formattedSize)
        assertEquals("16/11/2026", data.parsedAtDisplay)
        assertEquals("U1 · Compose", data.units.single().label)
        assertTrue(data.isPublished)
        assertFalse(data.isUploading)
        assertNull(data.actionError)
    }

    @Test
    fun `uploadSyllabusPdf rejects files over 10 MB client side without calling repository`() = runTest(dispatcher) {
        val repo = FakeTeacherRepository()
        val vm = TeacherSyllabusViewModel(repo)
        advanceUntilIdle()

        val oversized = ByteArray(TeacherSyllabusViewModel.MAX_PDF_BYTES.toInt() + 1)
        vm.uploadSyllabusPdf("gigante.pdf", oversized)
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertNull(repo.lastUploadedFileName)
        assertFalse(data.isUploading)
        assertEquals(
            AppError.Validation(TeacherSyllabusViewModel.FIELD_PDF_TOO_LARGE, TeacherSyllabusViewModel.FIELD_PDF_TOO_LARGE),
            data.actionError
        )
    }

    @Test
    fun `uploadSyllabusPdf failure keeps screen data and surfaces server error`() = runTest(dispatcher) {
        val repo = FakeTeacherRepository().apply {
            uploadResult = Result.Failure(AppError.Validation("request", "El PDF no tiene texto extraíble"))
        }
        val vm = TeacherSyllabusViewModel(repo)
        advanceUntilIdle()

        vm.uploadSyllabusPdf("escaneado.pdf", ByteArray(1024) { 1 })
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertFalse(data.isUploading)
        assertEquals(AppError.Validation("request", "El PDF no tiene texto extraíble"), data.actionError)
    }

    @Test
    fun `manual addTopic and publishSyllabus send null sourceUri when no PDF exists`() = runTest(dispatcher) {
        val repo = FakeTeacherRepository()
        val vm = TeacherSyllabusViewModel(repo)
        advanceUntilIdle()

        vm.addTopic("1.1 Límites")
        val result = vm.publishSyllabus()
        advanceUntilIdle()

        assertEquals(Result.Success(Unit), result)
        assertEquals(listOf("1.1 Límites"), repo.lastPublishedTopics)
        assertNull(repo.lastPublishedSourceUri)
    }
}
