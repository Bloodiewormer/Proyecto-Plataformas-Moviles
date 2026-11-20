package cr.ac.una.glifo.core.ui.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.model.RegionBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Recorte de la foto original que corresponde a una región del pipeline. Los [bounds] se
 * midieron sobre la imagen que procesó N0 ([analyzedWidth] × [analyzedHeight]), así que se
 * escalan a la foto guardada. N0 también endereza la hoja: en fotos muy inclinadas el
 * recorte es aproximado, pero sirve para comparar con lo que se leyó.
 */
@Composable
fun RegionCropImage(
    photoPath: String?,
    bounds: RegionBounds?,
    analyzedWidth: Int,
    analyzedHeight: Int,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp = 110.dp
) {
    val colors = GlifoTheme.colors
    val crop by produceState<Bitmap?>(initialValue = null, photoPath, bounds, analyzedWidth, analyzedHeight) {
        value = withContext(Dispatchers.IO) { loadCrop(photoPath, bounds, analyzedWidth, analyzedHeight) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colors.surfaceHigh, RoundedCornerShape(8.dp))
            .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        val image = crop
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = placeholder,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(6.dp)
            )
        } else {
            Text(placeholder, fontSize = 12.sp, color = colors.textSecondary)
        }
    }
}

private fun loadCrop(photoPath: String?, bounds: RegionBounds?, analyzedWidth: Int, analyzedHeight: Int): Bitmap? {
    if (photoPath == null || !File(photoPath).exists()) return null
    return runCatching {
        val sizeOnly = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photoPath, sizeOnly)
        // Se decodifica a ~1600 px de lado mayor: suficiente para un recorte y sin riesgo de
        // agotar memoria con fotos de 12 MP.
        var sample = 1
        while (max(sizeOnly.outWidth, sizeOnly.outHeight) / (sample * 2) >= 1600) sample *= 2
        val photo = BitmapFactory.decodeFile(photoPath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        if (bounds == null || analyzedWidth <= 0 || analyzedHeight <= 0) return photo

        val scaleX = photo.width.toFloat() / analyzedWidth
        val scaleY = photo.height.toFloat() / analyzedHeight
        val left = (bounds.left * scaleX).toInt().coerceIn(0, photo.width - 1)
        val top = (bounds.top * scaleY).toInt().coerceIn(0, photo.height - 1)
        val width = (bounds.width * scaleX).toInt().coerceIn(1, photo.width - left)
        val height = (bounds.height * scaleY).toInt().coerceIn(1, photo.height - top)
        Bitmap.createBitmap(photo, left, top, width, height)
    }.getOrNull()
}
