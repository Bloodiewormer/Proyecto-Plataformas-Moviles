package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.data.datasource.local.dao.LocalCourseContextDao
import cr.ac.una.glifo.data.datasource.local.entity.ContextVersions
import cr.ac.una.glifo.data.datasource.local.entity.LocalCourseContextEntity
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.CourseGlossaryResponse
import cr.ac.una.glifo.data.dto.GlossaryEntryResponse
import cr.ac.una.glifo.data.mapper.toEntity
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ContextSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CourseContextRepositoryImplTest {

    private val api = mockk<GlifoApi>()
    private val dao = mockk<LocalCourseContextDao>(relaxed = true)
    private val repository = CourseContextRepositoryImpl(api, dao)

    private fun glossary(glossaryVersion: Int = 3, syllabusVersion: Int = 2) = CourseGlossaryResponse(
        courseId = 1L,
        courseName = "Cálculo I",
        syllabusVersion = syllabusVersion,
        glossaryVersion = glossaryVersion,
        entries = listOf(
            GlossaryEntryResponse("Sumatoria", "\\sum_{n=1}^{\\infty}", "SYMBOL"),
            GlossaryEntryResponse("Converge", "conv.", "TERM")
        )
    )

    @Test
    fun `refresh stores the glossary the first time`() = runTest {
        val stored = slot<LocalCourseContextEntity>()
        coEvery { api.getCourseGlossary(1L) } returns ApiResponse(glossary())
        coEvery { dao.versions(1L) } returns null
        coEvery { dao.upsert(capture(stored)) } returns Unit
        coEvery { dao.forCourse(1L) } answers { stored.captured }

        val result = repository.refresh(1L)

        assertTrue(result is Result.Success)
        val context = (result as Result.Success).value
        assertEquals(ContextSource.Network, context.source)
        assertEquals(3, context.glossaryVersion)
        assertEquals(listOf("Sumatoria", "Converge"), context.glossaryTerms)
        assertEquals("\\sum_{n=1}^{\\infty}", context.entries.first().canonicalForm)
    }

    @Test
    fun `refresh does not rewrite the glossary when versions did not change`() = runTest {
        coEvery { api.getCourseGlossary(1L) } returns ApiResponse(glossary())
        coEvery { dao.versions(1L) } returns ContextVersions(syllabusVersion = 2, glossaryVersion = 3)
        coEvery { dao.forCourse(1L) } returns glossary().toEntity(cachedAt = 0L)

        repository.refresh(1L)

        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 1) { dao.touch(1L, any()) }
    }

    @Test
    fun `refresh replaces the cache when the teacher changed the glossary`() = runTest {
        coEvery { api.getCourseGlossary(1L) } returns ApiResponse(glossary(glossaryVersion = 4))
        coEvery { dao.versions(1L) } returns ContextVersions(syllabusVersion = 2, glossaryVersion = 3)
        coEvery { dao.forCourse(1L) } returns glossary(glossaryVersion = 4).toEntity(cachedAt = 0L)

        repository.refresh(1L)

        coVerify(exactly = 1) { dao.upsert(match { it.glossaryVersion == 4 }) }
    }

    @Test
    fun `refresh falls back to the Room copy when offline`() = runTest {
        coEvery { api.getCourseGlossary(1L) } throws IOException("sin red")
        coEvery { dao.forCourse(1L) } returns glossary().toEntity(cachedAt = 123L)

        val result = repository.refresh(1L)

        assertTrue(result is Result.Success)
        val context = (result as Result.Success).value
        assertEquals(ContextSource.Cache, context.source)
        assertEquals(123L, context.cachedAt)
        assertEquals(2, context.entries.size)
    }

    @Test
    fun `refresh fails with NoConnection when offline and nothing is cached`() = runTest {
        coEvery { api.getCourseGlossary(1L) } throws IOException("sin red")
        coEvery { dao.forCourse(1L) } returns null

        val result = repository.refresh(1L)

        assertEquals(Result.Failure(AppError.NoConnection), result)
    }

    @Test
    fun `getCached never calls the network`() = runTest {
        coEvery { dao.forCourse(7L) } returns null

        assertNull(repository.getCached(7L))
        coVerify(exactly = 0) { api.getCourseGlossary(any()) }
    }

    @Test
    fun `a corrupt glossary row yields an empty glossary instead of crashing`() = runTest {
        coEvery { dao.forCourse(1L) } returns glossary().toEntity(cachedAt = 0L).copy(glossaryEntries = "{no es json")

        val context = repository.getCached(1L)

        assertEquals(emptyList<String>(), context?.glossaryTerms)
    }
}
