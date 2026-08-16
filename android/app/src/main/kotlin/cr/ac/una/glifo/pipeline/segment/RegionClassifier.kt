package cr.ac.una.glifo.pipeline.segment

import cr.ac.una.glifo.pipeline.model.PageRegion
import cr.ac.una.glifo.pipeline.model.RegionKind
import javax.inject.Inject

class RegionClassifier @Inject constructor() {
    // classify(region: PageRegion, ocr: OcrResult): RegionKind
    fun classify(region: PageRegion): RegionKind {
        return RegionKind.TEXT
    }
}
