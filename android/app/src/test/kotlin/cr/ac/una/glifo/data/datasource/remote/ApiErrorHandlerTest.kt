package cr.ac.una.glifo.data.datasource.remote

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.domain.error.Result
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiErrorHandlerTest {

    @Test
    fun `safeApiCall maps SocketTimeoutException to AppError Timeout`() = runTest {
        val result = ApiErrorHandler.safeApiCall<String, String>(
            apiCall = { throw SocketTimeoutException("Read timed out") },
            transform = { it }
        )

        assertTrue(result is Result.Failure)
        assertEquals(AppError.Timeout, (result as Result.Failure).error)
    }

    @Test
    fun `safeApiCall maps UnknownHostException to AppError NoConnection`() = runTest {
        val result = ApiErrorHandler.safeApiCall<String, String>(
            apiCall = { throw UnknownHostException("Unable to resolve host") },
            transform = { it }
        )

        assertTrue(result is Result.Failure)
        assertEquals(AppError.NoConnection, (result as Result.Failure).error)
    }

    @Test
    fun `safeApiCall maps ConnectException to AppError NoConnection`() = runTest {
        val result = ApiErrorHandler.safeApiCall<String, String>(
            apiCall = { throw ConnectException("Failed to connect") },
            transform = { it }
        )

        assertTrue(result is Result.Failure)
        assertEquals(AppError.NoConnection, (result as Result.Failure).error)
    }

    @Test
    fun `safeApiCall maps general IOException to AppError NoConnection`() = runTest {
        val result = ApiErrorHandler.safeApiCall<String, String>(
            apiCall = { throw IOException("Broken pipe") },
            transform = { it }
        )

        assertTrue(result is Result.Failure)
        assertEquals(AppError.NoConnection, (result as Result.Failure).error)
    }

    @Test
    fun `safeApiCall maps HttpException to server error`() = runTest {
        val response = Response.error<String>(
            500,
            "Internal server error".toResponseBody("text/plain".toMediaTypeOrNull())
        )
        val result = ApiErrorHandler.safeApiCall<String, String>(
            apiCall = { throw HttpException(response) },
            transform = { it }
        )

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.Server)
        assertEquals("500", (error as AppError.Server).code)
    }

    @Test
    fun `safeApiCall returns Result Success with transformed value`() = runTest {
        val result = ApiErrorHandler.safeApiCall(
            apiCall = { "payload" },
            transform = { it.uppercase() }
        )

        assertTrue(result is Result.Success)
        assertEquals("PAYLOAD", (result as Result.Success).value)
    }
}
