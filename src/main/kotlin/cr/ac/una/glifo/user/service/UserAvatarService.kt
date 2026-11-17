package cr.ac.una.glifo.user.service

import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.user.entity.UserAvatar
import cr.ac.una.glifo.user.repository.UserAvatarRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

@Service
class UserAvatarService(private val avatarRepository: UserAvatarRepository) {

    /** Guarda o reemplaza la foto del usuario. Solo JPEG o PNG, detectados por su contenido. */
    @Transactional
    fun save(userId: Long, file: MultipartFile) {
        if (file.isEmpty) throw BadRequestException("La imagen está vacía")
        if (file.size > MAX_BYTES) throw BadRequestException("La imagen supera $MAX_KB KB")
        val bytes = file.bytes
        val contentType = imageType(bytes) ?: throw BadRequestException("La imagen debe ser JPEG o PNG")
        val avatar = avatarRepository.findById(userId).orElse(null) ?: UserAvatar(userId = userId)
        avatar.contentType = contentType
        avatar.data = bytes
        avatar.updatedAt = Instant.now()
        avatarRepository.save(avatar)
    }

    @Transactional(readOnly = true)
    fun find(userId: Long): UserAvatar =
        avatarRepository.findById(userId).orElseThrow { ResourceNotFoundException("El usuario no tiene foto de perfil") }

    private fun imageType(bytes: ByteArray): String? = when {
        bytes.startsWith(JPEG_MAGIC) -> "image/jpeg"
        bytes.startsWith(PNG_MAGIC) -> "image/png"
        else -> null
    }

    private fun ByteArray.startsWith(prefix: ByteArray) =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    companion object {
        const val MAX_KB = 1024
        private const val MAX_BYTES = MAX_KB * 1024L
        private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
