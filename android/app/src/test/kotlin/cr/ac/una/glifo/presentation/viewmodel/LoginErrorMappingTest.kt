package cr.ac.una.glifo.presentation.viewmodel

import android.content.Context
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.network.SessionEvent
import cr.ac.una.glifo.core.network.SessionEventBus
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.repository.AuthRepository
import cr.ac.una.glifo.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginErrorMappingTest {

    private val dispatcher = StandardTestDispatcher()
    private val authRepository = mockk<AuthRepository>()
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val context = mockk<Context>()

    private lateinit var loginViewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { context.getString(R.string.login_error_invalid_credentials) } returns "Correo o contraseña incorrectos."
        every { context.getString(R.string.error_unauthorized) } returns "Tu sesión expiró. Iniciá sesión de nuevo."
        coEvery { authRepository.getDemoAccountHints() } returns Result.Success(emptyList())

        loginViewModel = LoginViewModel(authRepository, sessionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login con 401 produce mensaje de credenciales incorrectas y no de sesion expirada`() = runTest {
        coEvery { authRepository.login("user@una.ac.cr", "bad-pass") } returns Result.Failure(AppError.Unauthorized)

        val result = loginViewModel.login("user@una.ac.cr", "bad-pass")

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        val loginMessage = error.toLoginMessage(context)

        assertEquals("Correo o contraseña incorrectos.", loginMessage)
        assertNotEquals("Tu sesión expiró. Iniciá sesión de nuevo.", loginMessage)
    }

    @Test
    fun `evento SessionExpired produce mensaje de sesion expirada`() = runTest {
        val bus = SessionEventBus()
        bus.emit(SessionEvent.SessionExpired())

        val event = bus.events.first()
        assertTrue(event is SessionEvent.SessionExpired)
        assertEquals("Tu sesión expiró. Iniciá sesión de nuevo.", (event as SessionEvent.SessionExpired).message)
    }

    @Test
    fun `otros 401 fuera de login siguen mostrando sesion expirada en toMessage`() {
        val error: AppError = AppError.Unauthorized
        val message = error.toMessage(context)

        assertEquals("Tu sesión expiró. Iniciá sesión de nuevo.", message)
        assertNotEquals("Correo o contraseña incorrectos.", message)
    }
}
