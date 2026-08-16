package cr.ac.una.glifo.pipeline.hash

import android.graphics.Bitmap
import javax.inject.Inject

class PerceptualHasher @Inject constructor() {
    fun hash(image: Bitmap): String {
        return ""
    }
    
    fun isDuplicate(hash: String): Boolean {
        return false
    }
}
