package cr.ac.una.glifo.ai.engine

interface MathOcrEngine {
    fun name(): String
    fun isAvailable(): Boolean
    // recognize(crop: RegionCrop): MathResult
}
