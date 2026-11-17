package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.model.GlossaryEntry
import cr.ac.una.glifo.domain.model.GlossarySuggestion
import cr.ac.una.glifo.domain.model.SyllabusUnitRow
import cr.ac.una.glifo.domain.model.TeacherGapTopic
import cr.ac.una.glifo.domain.model.TeacherGroupCoverage
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.TeacherRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherCoursesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val activeCourseId = MutableStateFlow<Long?>(null)
    private val store = mockk<ActiveCourseStore>(relaxed = true) {
        every { activeCourseId } returns this@TeacherCoursesViewModelTest.activeCourseId
    }

    private val course = Course(id = 10L, name = "Calculo I", code = "MAT-101", joinCode = "GLF-CALC1")
    private val baseStats = CourseStats(
        courseId = 10L,
        instructorName = "",
        coveragePercent = 40,
        verified = 40f,
        repaired = 10f,
        escalated = 30f,
        uncertain = 20f,
        studentCount = 0,
        noteCount = 0,
        topicCount = 5,
        pendingCount = 0,
        weeklyDeltaPoints = 0,
        hasSyllabus = true
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeTeacherRepository(
        private val groupCoverageResult: Result<TeacherGroupCoverage>
    ) : TeacherRepository {
        override suspend fun getGlossaryEntries(): Result<List<GlossaryEntry>> = Result.Success(emptyList())
        override suspend fun addGlossaryEntry(term: String, notation: String, isSymbol: Boolean) = Result.Success(Unit)
        override suspend fun getGlossarySuggestions(): Result<List<GlossarySuggestion>> = Result.Success(emptyList())
        override suspend fun resolveGlossarySuggestion(suggestionId: Long, approve: Boolean) = Result.Success(Unit)
        override suspend fun getSyllabusUnits(): Result<List<SyllabusUnitRow>> = Result.Success(emptyList())
        override suspend fun addSyllabusTopic(label: String) = Result.Success(Unit)
        override suspend fun getGaps(): Result<List<TeacherGapTopic>> = Result.Success(emptyList())
        override suspend fun getGroupCoverage(courseId: Long): Result<TeacherGroupCoverage> = groupCoverageResult
    }

    @Test
    fun `overlays enrolledStudents from group coverage onto course studentCount`() = runTest(dispatcher) {
        val courses = mockk<CourseRepository> {
            coEvery { getTeacherCourses() } returns Result.Success(listOf(course))
            coEvery { getCourseStats(10L) } returns Result.Success(baseStats)
        }
        val teacher = FakeTeacherRepository(
            Result.Success(
                TeacherGroupCoverage(
                    enrolledStudents = 3,
                    coveragePercent = 80,
                    topics = emptyList(),
                    solidShare = 70f,
                    partialShare = 15f,
                    absentShare = 10f,
                    uncertainShare = 5f
                )
            )
        )

        val vm = TeacherCoursesViewModel(courses, store, teacher)
        advanceUntilIdle()

        val entry = (vm.state.value as UiState.Success).data.single()
        assertEquals(10L, entry.course.id)
        assertEquals(3, entry.stats.studentCount)
        assertEquals(80, entry.stats.coveragePercent)
        assertEquals(70f, entry.stats.verified, 0.01f)
    }

    @Test
    fun `keeps original stats and course when group coverage fails`() = runTest(dispatcher) {
        val courses = mockk<CourseRepository> {
            coEvery { getTeacherCourses() } returns Result.Success(listOf(course))
            coEvery { getCourseStats(10L) } returns Result.Success(baseStats)
        }
        val teacher = FakeTeacherRepository(Result.Failure(AppError.Unknown))

        val vm = TeacherCoursesViewModel(courses, store, teacher)
        advanceUntilIdle()

        val entry = (vm.state.value as UiState.Success).data.single()
        assertEquals(baseStats, entry.stats)
    }
}
