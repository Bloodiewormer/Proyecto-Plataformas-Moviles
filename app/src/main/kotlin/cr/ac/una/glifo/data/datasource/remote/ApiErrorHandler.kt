package cr.ac.una.glifo.data.datasource.remote

import com.google.gson.JsonParser
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.domain.error.Result
import retrofit2.HttpException
import java.io.IOException

/**
 * Standardized mapping of network and HTTP errors to domain [AppError] / [Result].
 * Enforces the architectural rule that no Retrofit or OkHttp exception leaks beyond the data layer.
 */
object ApiErrorHandler {

    suspend fun <T, R> safeApiCall(
        apiCall: suspend () -> T,
        transform: (T) -> R
    ): Result<R> {
        return try {
            val response = apiCall()
            Result.Success(transform(response))
        } catch (e: IOException) {
            Result.Failure(AppError.NoConnection)
        } catch (e: HttpException) {
            Result.Failure(e.toAppError())
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown)
        }
    }

    fun HttpException.toAppError(defaultValidationField: String = "request"): AppError {
        val serverMessage = parseServerMessage()
        return when (code()) {
            401 -> AppError.Unauthorized
            400, 422 -> AppError.Validation(
                field = defaultValidationField,
                issue = serverMessage.orEmpty()
            )
            404 -> AppError.Server(
                code = "404",
                message = serverMessage.orEmpty()
            )
            409 -> AppError.Server(
                code = "409",
                message = serverMessage.orEmpty()
            )
            else -> AppError.Server(
                code = code().toString(),
                message = serverMessage.orEmpty()
            )
        }
    }

    private fun HttpException.parseServerMessage(): String? {
        return try {
            val raw = response()?.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: return null
            try {
                val json = JsonParser.parseString(raw).asJsonObject
                json.get("message")?.asString ?: raw
            } catch (_: Exception) {
                raw
            }
        } catch (e: Exception) {
            null
        }
    }
}
