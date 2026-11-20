package cr.ac.una.glifo.core.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Círculo con la foto de perfil guardada en [photoPath], o las iniciales si no hay foto. */
@Composable
fun ProfileAvatar(
    photoPath: String?,
    initials: String,
    size: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val colors = GlifoTheme.colors
    val photo by produceState<ImageBitmap?>(null, photoPath) {
        value = photoPath?.let { path ->
            withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.surfaceHigh)
            .border(1.dp, colors.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val current = photo
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = stringResource(R.string.profile_photo_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(initials, fontSize = fontSize, fontWeight = FontWeight.SemiBold, color = colors.accentText)
        }
    }
}
