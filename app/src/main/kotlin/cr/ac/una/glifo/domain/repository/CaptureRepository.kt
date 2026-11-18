package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.model.PageAnalysis

interface CaptureRepository {
    suspend fun getQualityChecks(photoPath: String? = null): Result<List<CaptureQualityCheck>>
    suspend fun hasQualityWarning(photoPath: String? = null): Result<Boolean>

    /**
     * Corre el tramo local del pipeline sobre la foto: N0, segmentacion, N1 (OCR con ML Kit)
     * y el clasificador que enruta cada region. Sin foto no hay nada que analizar.
     *
     * Con [courseId], el scorer usa el glosario del curso guardado en Room (#27) y, si la
     * pagina ya se habia procesado para ese curso, se reutiliza ese resultado (#28).
     * Con [skipDedup] en true se fuerza el analisis completo de la foto omitiendo deduplicacion.
     */
    suspend fun analyzePage(
        photoPath: String?,
        courseId: Long? = null,
        skipDedup: Boolean = false
    ): Result<PageAnalysis>

    /** Linea de tiempo del procesamiento, derivada del analisis real cuando hay foto. */
    suspend fun getProcessingSteps(
        photoPath: String? = null,
        courseId: Long? = null,
        skipDedup: Boolean = false
    ): Result<List<CaptureStep>>

    /**
     * Guarda la captura ya analizada como apunte del curso (#25) y la deja en la cola de
     * sincronizacion (#26). Devuelve el id local del apunte para abrir sus resultados.
     */
    suspend fun saveAsNote(photoPath: String, courseId: Long): Result<Long>
}
