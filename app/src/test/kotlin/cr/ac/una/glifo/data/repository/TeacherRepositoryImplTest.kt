package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.CourseGlossaryResponse
import cr.ac.una.glifo.data.dto.CreateGlossaryEntryRequest
import cr.ac.una.glifo.data.dto.GlossaryEntryResponse
import cr.ac.una.glifo.data.dto.GlossarySuggestionResponse
import cr.ac.una.glifo.data.dto.ResolveGlossarySuggestionRequest
import cr.ac.una.glifo.domain.error.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Glosario del docente contra la API real (issue #33), siempre sobre el curso activo. */
class TeacherRepositoryImplTest {

    private val api = mockk<GlifoApi>()
    private val activeCourseId = MutableStateFlow<Long?>(10L)
    private val activeCourseStore = mockk<ActiveCourseStore> { every { activeCourseId } returns this@TeacherRepositoryImplTest.activeCourseId }
    private val repository = TeacherRepositoryImpl(api, activeCourseStore)

    private fun httpException(code: Int, body: String = ""): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, responseBody))
    }

    @Test
    fun `getGlossaryEntries pide el glosario del curso activo y marca SYMBOL y NOTATION como simbolo`() = runTest {
        coEvery { api.getCourseGlossary(10L) } returns ApiResponse(
            CourseGlossaryResponse(
                courseId = 10L, courseName = "Cálculo I", syllabusVersion = 1, glossaryVersion = 4,
                entries = listOf(
                    GlossaryEntryResponse("ε", "\\varepsilon", "SYMBOL"),
                    GlossaryEntryResponse("lim", "\\lim_{x \\to a}", "NOTATION"),
                    GlossaryEntryResponse("compacto", "cerrado y acotado", "TERM")
                )
            )
        )

        val result = repository.getGlossaryEntries()

        val entries = (result as Result.Success).value
        assertEquals(listOf(true, true, false), entries.map { it.isSymbol })
        assertEquals("\\varepsilon", entries[0].notation)
        assertEquals("TÉRMINO", entries[2].tag)
    }

    @Test
    fun `sin curso activo no llama a la API y pide elegir un curso`() = runTest {
        activeCourseId.value = null

        val result = repository.getGlossaryEntries()

        assertTrue((result as Result.Failure).error is AppError.Validation)
        coVerify(exactly = 0) { api.getCourseGlossary(any()) }
    }

    @Test
    fun `addGlossaryEntry manda el termino recortado con kind SYMBOL`() = runTest {
        coEvery { api.addGlossaryEntry(10L, any()) } returns ApiResponse(GlossaryEntryResponse("Sumatoria", "\\sum", "SYMBOL", id = 9L))

        val result = repository.addGlossaryEntry("  Sumatoria ", " \\sum ", isSymbol = true)

        assertEquals(Result.Success(Unit), result)
        coVerify { api.addGlossaryEntry(10L, CreateGlossaryEntryRequest("Sumatoria", "\\sum", "SYMBOL")) }
    }

    @Test
    fun `addGlossaryEntry con campos vacios falla sin llamar a la API`() = runTest {
        val result = repository.addGlossaryEntry(" ", "\\sum", isSymbol = true)

        assertTrue((result as Result.Failure).error is AppError.Validation)
        coVerify(exactly = 0) { api.addGlossaryEntry(any(), any()) }
    }

    @Test
    fun `un termino repetido 409 llega como error del servidor con el mensaje del backend`() = runTest {
        coEvery { api.addGlossaryEntry(10L, any()) } throws
            httpException(409, """{"code":"DUPLICATE","message":"El término 'ε' ya está en el glosario como SYMBOL"}""")

        val result = repository.addGlossaryEntry("ε", "\\varepsilon", isSymbol = true)

        assertEquals(
            Result.Failure(AppError.Server("409", "El término 'ε' ya está en el glosario como SYMBOL")),
            result
        )
    }

    @Test
    fun `getGlossarySuggestions muestra el texto original y la correccion propuesta`() = runTest {
        coEvery { api.getGlossarySuggestions(10L) } returns ApiResponse(
            listOf(
                GlossarySuggestionResponse(
                    id = 5L, courseId = 10L, suggestedBy = "Ana Mora", noteId = null,
                    originalText = "cnv", suggestedCorrection = "converge", status = "PENDING",
                    createdAt = "2026-10-13T10:00:00Z", resolvedAt = null
                )
            )
        )

        val suggestion = (repository.getGlossarySuggestions() as Result.Success).value.single()

        assertEquals(5L, suggestion.id)
        assertEquals("cnv", suggestion.term)
        assertEquals("→ converge · Ana Mora", suggestion.meta)
    }

    @Test
    fun `aprobar y descartar mandan APPROVED y REJECTED`() = runTest {
        val response = GlossarySuggestionResponse(5L, 10L, "Ana", null, "cnv", "converge", "APPROVED", "", null)
        coEvery { api.resolveGlossarySuggestion(10L, 5L, any()) } returns ApiResponse(response)

        repository.resolveGlossarySuggestion(5L, approve = true)
        repository.resolveGlossarySuggestion(5L, approve = false)

        coVerify { api.resolveGlossarySuggestion(10L, 5L, ResolveGlossarySuggestionRequest("APPROVED")) }
        coVerify { api.resolveGlossarySuggestion(10L, 5L, ResolveGlossarySuggestionRequest("REJECTED")) }
    }
}
