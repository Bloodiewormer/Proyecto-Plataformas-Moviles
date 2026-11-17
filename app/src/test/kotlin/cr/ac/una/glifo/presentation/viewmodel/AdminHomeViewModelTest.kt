package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ApiHealth
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.repository.AdminRepository
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminHomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `load aggregates api health, deduplicated course count, and pending sync count`() = runTest(dispatcher) {
        val sharedCourse = Course(id = 1L, name = "Estructuras", code = "EIF-207", joinCode = "ABCD1234", term = "II 2026")
        val teacherOnly = Course(id = 2L, name = "Bases", code = "EIF-402", joinCode = "EFGH5678", term = "II 2026")
        val adminRepo = mockk<AdminRepository> {
            coEvery { checkHealth() } returns Result.Success(ApiHealth("ok", "glifo-api", "2026-11-17T00:00:00Z"))
        }
        val courseRepo = mockk<CourseRepository> {
            coEvery { getStudentCourses() } returns Result.Success(listOf(sharedCourse))
            coEvery { getTeacherCourses() } returns Result.Success(listOf(sharedCourse, teacherOnly))
        }
        val syncRepo = mockk<SyncRepository> {
            coEvery { getPendingCount() } returns Result.Success(3)
        }

        val vm = AdminHomeViewModel(adminRepo, courseRepo, syncRepo)
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertEquals("ok", data.apiHealth?.status)
        assertNull(data.healthError)
        assertEquals(2, data.courseCount)
        assertEquals(3, data.pendingSyncCount)
    }

    @Test
    fun `load sets courseCount null when both course queries fail and surfaces healthError`() = runTest(dispatcher) {
        val adminRepo = mockk<AdminRepository> {
            coEvery { checkHealth() } returns Result.Failure(AppError.NoConnection)
        }
        val courseRepo = mockk<CourseRepository> {
            coEvery { getStudentCourses() } returns Result.Failure(AppError.NoConnection)
            coEvery { getTeacherCourses() } returns Result.Failure(AppError.NoConnection)
        }
        val syncRepo = mockk<SyncRepository> {
            coEvery { getPendingCount() } returns Result.Success(0)
        }

        val vm = AdminHomeViewModel(adminRepo, courseRepo, syncRepo)
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertNull(data.apiHealth)
        assertEquals(AppError.NoConnection, data.healthError)
        assertNull(data.courseCount)
        assertEquals(0, data.pendingSyncCount)
    }
}
