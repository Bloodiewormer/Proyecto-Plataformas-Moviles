package cr.ac.una.glifo.common.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

data class HealthResponse(
    val status: String,
    val service: String,
    val timestamp: Instant
)

/**
 * Punto de prueba de la comunicación cliente-servidor: responde sin autenticación, así un
 * cliente (la app, el navegador o curl) puede comprobar que llega al backend antes de
 * probar el login.
 */
@RestController
@RequestMapping("\${url.health}", produces = [MediaType.APPLICATION_JSON_VALUE])
class HealthController {

    @GetMapping
    fun health(): ApiResponse<HealthResponse> =
        ApiResponse(data = HealthResponse(status = "UP", service = "glifo-api", timestamp = Instant.now()))
}
