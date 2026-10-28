package cr.ac.una.glifo.core.network

import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.domain.repository.SessionRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val sessionEventBus: SessionEventBus,
    private val sessionRepository: SessionRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = runBlocking { tokenManager.getToken() }

        val request = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(request)

        if (response.code == 401) {
            val isLoginRequest = request.url.encodedPath.contains("auth/login")
            val hadToken = !token.isNullOrBlank() || request.header("Authorization") != null
            if (!isLoginRequest && hadToken) {
                runBlocking {
                    tokenManager.clearAll()
                    sessionRepository.logout()
                }
                sessionEventBus.emit(SessionEvent.SessionExpired())
            }
        }

        return response
    }
}
