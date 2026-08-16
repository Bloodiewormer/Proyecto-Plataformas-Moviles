package cr.ac.una.glifo.pipeline

import android.graphics.Bitmap
import cr.ac.una.glifo.pipeline.model.PageRegion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelineEngine @Inject constructor() {
    /**
     * Entry point for the ingestion pipeline.
     * N0 -> N1 -> ...
     */
    fun process(image: Bitmap): List<PageRegion> {
        // Implementation will follow the escalation policy
        return emptyList()
    }
}
