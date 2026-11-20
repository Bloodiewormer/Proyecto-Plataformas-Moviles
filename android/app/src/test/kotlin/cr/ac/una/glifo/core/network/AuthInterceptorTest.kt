package cr.ac.una.glifo.core.network

import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val sessionEventBus = mockk<SessionEventBus>(relaxed = true)
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val chain = mockk<Interceptor.Chain>()

    private lateinit var interceptor: AuthInterceptor

    @Before
    fun setUp() {
        interceptor = AuthInterceptor(tokenManager, sessionEventBus, sessionRepository)
    }

    private fun createResponse(request: Request, code: Int): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Unauthorized")
            .build()
    }

    @Test
    fun `intercept adds Authorization header when token is present`() {
        val request = Request.Builder().url("http://10.0.2.2:8080/api/v1/notes").build()
        val capturedRequest = slot<Request>()

        coEvery { tokenManager.getToken() } returns "my-token-123"
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } returns createResponse(request, 200)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals("Bearer my-token-123", capturedRequest.captured.header("Authorization"))
    }

    @Test
    fun `intercept emits SessionExpired and expires the session without wiping local data on 401`() {
        val request = Request.Builder().url("http://10.0.2.2:8080/api/v1/notes").build()

        coEvery { tokenManager.getToken() } returns "expired-token"
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers { createResponse(firstArg(), 401) }

        val response = interceptor.intercept(chain)

        assertEquals(401, response.code)
        coVerify(exactly = 1) { sessionRepository.expireSession() }
        coVerify(exactly = 0) { sessionRepository.logout() }
        coVerify(exactly = 0) { tokenManager.clearAll() }
        verify(exactly = 1) {
            sessionEventBus.emit(match { it is SessionEvent.SessionExpired && it.message == "Tu sesión expiró. Iniciá sesión de nuevo." })
        }
    }

    @Test
    fun `intercept does NOT emit SessionExpired on 401 from auth login endpoint`() {
        val loginRequest = Request.Builder().url("http://10.0.2.2:8080/api/v1/auth/login").build()

        coEvery { tokenManager.getToken() } returns null
        every { chain.request() } returns loginRequest
        every { chain.proceed(any()) } returns createResponse(loginRequest, 401)

        val response = interceptor.intercept(chain)

        assertEquals(401, response.code)
        coVerify(exactly = 0) { tokenManager.clearAll() }
        coVerify(exactly = 0) { sessionRepository.logout() }
        verify(exactly = 0) { sessionEventBus.emit(any()) }
    }

    @Test
    fun `intercept does NOT emit SessionExpired on 401 if request had no token`() {
        val publicRequest = Request.Builder().url("http://10.0.2.2:8080/api/v1/public/info").build()

        coEvery { tokenManager.getToken() } returns null
        every { chain.request() } returns publicRequest
        every { chain.proceed(any()) } returns createResponse(publicRequest, 401)

        val response = interceptor.intercept(chain)

        assertEquals(401, response.code)
        coVerify(exactly = 0) { tokenManager.clearAll() }
        coVerify(exactly = 0) { sessionRepository.logout() }
        verify(exactly = 0) { sessionEventBus.emit(any()) }
    }

    @Test
    fun `real SessionEventBus emits and receives SessionExpired event`() = runTest {
        val realBus = SessionEventBus()
        realBus.emit(SessionEvent.SessionExpired())

        val event = realBus.events.first()
        assertTrue(event is SessionEvent.SessionExpired)
        assertEquals("Tu sesión expiró. Iniciá sesión de nuevo.", (event as SessionEvent.SessionExpired).message)
    }
}
