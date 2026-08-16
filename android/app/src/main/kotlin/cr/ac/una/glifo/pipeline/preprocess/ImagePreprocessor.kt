package cr.ac.una.glifo.pipeline.preprocess

import android.graphics.Bitmap

interface ImagePreprocessor {
    fun normalize(image: Bitmap): PreprocessResult
}

data class PreprocessResult(
    val bitmap: Bitmap,
    val skewAngle: Float
)
