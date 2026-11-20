package cr.ac.una.glifo.ops.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.ops.dto.SyncCompleteRequest
import cr.ac.una.glifo.ops.dto.SyncCompleteResponse
import cr.ac.una.glifo.ops.service.SyncNotificationService
import cr.ac.una.glifo.security.GlifoUserDetails
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Cierre de un lote de sincronización de la cola offline de un dispositivo específico
 * y disparo de la notificación push dirigida exclusivamente a ese `device_id`.
 */
@Validated
@RestController
@RequestMapping("\${url.sync}", produces = [MediaType.APPLICATION_JSON_VALUE])
class SyncController(
    private val syncNotificationService: SyncNotificationService,
) {

    @PostMapping("/complete", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun completeSync(
        @Valid @RequestBody request: SyncCompleteRequest,
        @AuthenticationPrincipal user: GlifoUserDetails,
    ): ResponseEntity<ApiResponse<SyncCompleteResponse>> {
        val outcome = syncNotificationService.completeSync(user.userId, request)
        val status = if (outcome.created) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(ApiResponse(data = outcome.response))
    }
}
