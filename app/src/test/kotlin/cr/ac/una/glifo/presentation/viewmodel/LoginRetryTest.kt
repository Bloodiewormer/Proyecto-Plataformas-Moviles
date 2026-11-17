package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.model.User
import cr.ac.una.glifo.domain.repository.AuthRepository
import cr.ac.una.glifo.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginRetryTest {

    private val dispatcher = StandardTestDispatcher()
    private val authRepository = mockk<AuthRepository>()
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { authRepository.getDemoAccountHints() } returns Result.Success(emptyList())
        viewModel = LoginViewModel(authRepository, sessionRepository, retryDelayMs = 0L)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `retries once on NoConnection and completes login when retry succeeds`() = runTest(dispatcher) {
        val user = User(id = 1L, fullName = "Docente Glifo", email = "docente@una.ac.cr", role = GlifoRole.Teacher)
        val observedRetrying = mutableListOf<Boolean>()
        var attempts = 0
        coEvery { authRepository.login("docente@una.ac.cr", "secret") } coAnswers {
            attempts += 1
            if (attempts == 1) {
                Result.Failure(AppError.NoConnection)
            } else {
                observedRetrying += viewModel.isRetrying.value
                Result.Success(user)
            }
        }

        val result = viewModel.login("docente@una.ac.cr", "secret")

        assertEquals(Result.Success(user), result)
        assertEquals(listOf(true), observedRetrying)
        assertFalse(viewModel.isRetrying.value)
        coVerify(exactly = 2) { authRepository.login("docente@una.ac.cr", "secret") }
        coVerify(exactly = 1) { sessionRepository.login(user) }
    }

    @Test
    fun `retries at most once when second attempt also fails with NoConnection`() = runTest(dispatcher) {
        coEvery { authRepository.login("docente@una.ac.cr", "secret") } returns Result.Failure(AppError.NoConnection)

        val result = viewModel.login("docente@una.ac.cr", "secret")

        assertEquals(Result.Failure(AppError.NoConnection), result)
        assertFalse(viewModel.isRetrying.value)
        coVerify(exactly = 2) { authRepository.login("docente@una.ac.cr", "secret") }
        coVerify(exactly = 0) { sessionRepository.login(any()) }
    }

    @Test
    fun `does not retry on 4xx or 5xx errors`() = runTest(dispatcher) {
        val nonRetryableErrors = listOf(
            AppError.Unauthorized,
            AppError.Validation("credentials", "Bad credentials"),
            AppError.Server("500", "Server error")
        )

        nonRetryableErrors.forEachIndexed { index, error ->
            val email = "user$index@una.ac.cr"
            coEvery { authRepository.login(email, "secret") } returns Result.Failure(error)

            val result = viewModel.login(email, "secret")

            assertTrue(result is Result.Failure)
            assertEquals(error, (result as Result.Failure).error)
            assertFalse(viewModel.isRetrying.value)
            coVerify(exactly = 1) { authRepository.login(email, "secret") }
        }
    }
}
