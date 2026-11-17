package cr.ac.una.glifo.core.datastore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copia local de la foto de perfil de cada usuario. Se copia y se reduce al elegirla: el
 * permiso del selector de fotos sobre la URI original no dura más que la actividad. El
 * servidor guarda la misma imagen para verla en otros dispositivos (SessionRepositoryImpl).
 */
@Singleton
class ProfilePhotoStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dir: File get() = File(context.filesDir, DIR_NAME)

    /** Ruta de la foto guardada del usuario, o null si no tiene. */
    fun find(userId: Long): String? =
        dir.listFiles { file -> file.name.startsWith("$userId-") }
            ?.maxByOrNull { it.lastModified() }
            ?.absolutePath

    /** Copia la imagen elegida como nueva foto del usuario; null si no se pudo leer. */
    suspend fun save(userId: Long, sourceUri: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(sourceUri)
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val degrees = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
            val scale = MAX_SIDE_PX.toFloat() / maxOf(decoded.width, decoded.height)
            val matrix = Matrix().apply {
                if (scale < 1f) postScale(scale, scale)
                postRotate(degrees.toFloat())
            }
            val bitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            val jpeg = ByteArrayOutputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
                it.toByteArray()
            }
            write(userId, jpeg)
        }.getOrNull()
    }

    /** Guarda como foto local la imagen que devolvió el servidor. */
    suspend fun saveDownloaded(userId: Long, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        runCatching { write(userId, bytes) }.getOrNull()
    }

    private fun write(userId: Long, bytes: ByteArray): String {
        dir.mkdirs()
        val target = File(dir, "$userId-${System.currentTimeMillis()}.jpg")
        target.writeBytes(bytes)
        dir.listFiles { file -> file.name.startsWith("$userId-") && file != target }?.forEach { it.delete() }
        return target.absolutePath
    }

    private val ExifInterface.rotationDegrees: Int
        get() = when (getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private companion object {
        const val DIR_NAME = "profile_photos"
        const val MAX_SIDE_PX = 512
        const val JPEG_QUALITY = 90
    }
}
