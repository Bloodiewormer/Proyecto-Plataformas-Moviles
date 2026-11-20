package cr.ac.una.glifo.ops.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.ops.dto.DeviceResponse
import cr.ac.una.glifo.ops.dto.RegisterDeviceRequest
import cr.ac.una.glifo.ops.dto.UpdateDeviceTokenRequest
import cr.ac.una.glifo.ops.service.DeviceService
import cr.ac.una.glifo.security.GlifoUserDetails
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * Registro, rotación de token FCM y desvinculación de dispositivos del usuario autenticado.
 * El usuario propietario siempre se obtiene del JWT (`@AuthenticationPrincipal`), nunca del body.
 */
@Validated
@RestController
@RequestMapping("\${url.devices}", produces = [MediaType.APPLICATION_JSON_VALUE])
class DeviceController(
    private val deviceService: DeviceService,
) {

    @GetMapping
    fun getMyDevices(
        @AuthenticationPrincipal user: GlifoUserDetails,
    ): ResponseEntity<ApiResponse<List<DeviceResponse>>> =
        ResponseEntity.ok(ApiResponse(data = deviceService.getUserDevices(user.userId)))

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun registerDevice(
        @Valid @RequestBody request: RegisterDeviceRequest,
        @AuthenticationPrincipal user: GlifoUserDetails,
    ): ResponseEntity<ApiResponse<DeviceResponse>> {
        val outcome = deviceService.registerOrUpdateDevice(user.userId, request)
        return if (outcome.created) {
            val location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(outcome.device.id)
                .toUri()
            ResponseEntity.created(location).body(ApiResponse(data = outcome.device))
        } else {
            ResponseEntity.ok(ApiResponse(data = outcome.device))
        }
    }

    @PutMapping("/{id}/token", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateDeviceToken(
        @PathVariable @Positive(message = "El id del dispositivo debe ser positivo") id: Long,
        @Valid @RequestBody request: UpdateDeviceTokenRequest,
        @AuthenticationPrincipal user: GlifoUserDetails,
    ): ResponseEntity<ApiResponse<DeviceResponse>> =
        ResponseEntity.ok(ApiResponse(data = deviceService.updateDeviceToken(user.userId, id, request)))

    @DeleteMapping("/{id}")
    fun unlinkDevice(
        @PathVariable @Positive(message = "El id del dispositivo debe ser positivo") id: Long,
        @AuthenticationPrincipal user: GlifoUserDetails,
    ): ResponseEntity<Void> {
        deviceService.unlinkDevice(user.userId, id)
        return ResponseEntity.noContent().build()
    }
}
