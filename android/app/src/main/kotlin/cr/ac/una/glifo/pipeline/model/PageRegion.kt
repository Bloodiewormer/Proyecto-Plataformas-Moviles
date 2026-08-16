package cr.ac.una.glifo.pipeline.model

import android.graphics.Bitmap
import android.graphics.Rect

data class PageRegion(
    val id: String,
    val bbox: Rect,
    val kind: RegionKind,
    val crop: Bitmap
)
