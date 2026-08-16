package cr.ac.una.glifo.pipeline.segment

import android.graphics.Bitmap
import cr.ac.una.glifo.pipeline.model.PageRegion
import javax.inject.Inject

class RegionSegmenter @Inject constructor() {
    fun segment(image: Bitmap): List<PageRegion> {
        return emptyList()
    }
}
