package cr.ac.una.glifo.user.controller

import cr.ac.una.glifo.security.GlifoUserDetails
import cr.ac.una.glifo.user.service.UserAvatarService
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Foto de perfil del usuario con sesión, para que se vea igual en todos sus dispositivos.
 *
 * | Método | Ruta             | Éxito                 | Errores                                  |
 * |--------|------------------|-----------------------|------------------------------------------|
 * | PUT    | /users/me/avatar | 204                   | 400 (vacía, > 1 MB, no JPEG/PNG), 401    |
 * | GET    | /users/me/avatar | 200 image/jpeg o png  | 401, 404 (sin foto)                      |
 */
@RestController
@RequestMapping("\${url.users}")
class UserAvatarController(private val avatarService: UserAvatarService) {

    @PutMapping("/me/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @AuthenticationPrincipal user: GlifoUserDetails,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<Void> {
        avatarService.save(user.userId, file)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me/avatar")
    fun download(@AuthenticationPrincipal user: GlifoUserDetails): ResponseEntity<ByteArray> {
        val avatar = avatarService.find(user.userId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(avatar.contentType))
            .cacheControl(CacheControl.noCache())
            .body(avatar.data)
    }
}
