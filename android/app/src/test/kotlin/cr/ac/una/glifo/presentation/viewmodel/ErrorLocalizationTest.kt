package cr.ac.una.glifo.presentation.viewmodel

import android.content.Context
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler.toAppError
import io.mockk.every
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ErrorLocalizationTest {

    private val englishContext = mockk<Context> {
        every { getString(R.string.error_no_connection) } returns "No connection. Try again."
        every { getString(R.string.error_timeout) } returns "The server is taking too long to respond. Try again in a moment."
        every { getString(R.string.error_invalid_request) } returns "Invalid request data"
        every { getString(R.string.error_not_found) } returns "Resource not found"
        every { getString(R.string.error_conflict) } returns "Request conflict"
        every { getString(R.string.error_server_code, "500") } returns "Server error (500)"
        every { getString(R.string.error_unknown) } returns "Something went wrong. Please try again."
        every { getString(R.string.note_reconstruct_error_not_synced) } returns "This note is still uploading. Try again when it finishes."
    }

    private fun httpException(code: Int, body: String = ""): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaTypeOrNull())))

    @Test
    fun `empty HTTP error bodies resolve to localized UI fallback messages`() {
        assertEquals("Invalid request data", httpException(422).toAppError().toMessage(englishContext))
        assertEquals("Resource not found", httpException(404).toAppError().toMessage(englishContext))
        assertEquals("Request conflict", httpException(409).toAppError().toMessage(englishContext))
        assertEquals("Server error (500)", httpException(500).toAppError().toMessage(englishContext))
        assertEquals("Something went wrong. Please try again.", AppError.Database("").toMessage(englishContext))
    }

    @Test
    fun `explicit server messages are preserved in toMessage`() {
        val error = httpException(409, """{"message":"Duplicate entry"}""").toAppError()

        assertEquals("Duplicate entry", error.toMessage(englishContext))
    }

    @Test
    fun `timeout error resolves to localized timeout message`() {
        assertEquals(
            "The server is taking too long to respond. Try again in a moment.",
            AppError.Timeout.toMessage(englishContext)
        )
        assertEquals(
            "The server is taking too long to respond. Try again in a moment.",
            AppError.Timeout.toReconstructMessage(englishContext)
        )
    }

    @Test
    fun `reconstruct with unsynced note resolves to uploading message`() {
        val error = AppError.Validation("serverId", "ignored")
        assertEquals(
            "This note is still uploading. Try again when it finishes.",
            error.toReconstructMessage(englishContext)
        )
    }
}
