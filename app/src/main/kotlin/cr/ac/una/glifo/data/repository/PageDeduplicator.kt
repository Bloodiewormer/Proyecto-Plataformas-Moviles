package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.pipeline.PerceptualHasher
import cr.ac.una.glifo.data.datasource.local.dao.ProcessedPageDao
import cr.ac.una.glifo.data.datasource.local.entity.ProcessedPageEntity
import cr.ac.una.glifo.data.mapper.pageAnalysisFromSnapshotJson
import cr.ac.una.glifo.data.mapper.toSnapshotJson
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.PageReuse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deduplicación de capturas por hash perceptual (issue #28): recuerda cada página que pasó
 * por el pipeline y, ante una captura nueva del mismo curso con un hash casi igual, devuelve
 * el análisis guardado en lugar de volver a procesar.
 */
@Singleton
class PageDeduplicator @Inject constructor(
    private val dao: ProcessedPageDao
) {

    /** La página más parecida del curso, si está dentro del umbral y su análisis se puede leer. */
    suspend fun findSamePage(courseId: Long, hash: String): PageAnalysis? {
        val closest = dao.hashesForCourse(courseId)
            .map { it to PerceptualHasher.hammingDistance(hash, it.perceptualHash) }
            .filter { (_, distance) -> distance <= PerceptualHasher.DUPLICATE_THRESHOLD }
            .minByOrNull { (_, distance) -> distance }
            ?: return null

        val (row, distance) = closest
        val stored = dao.getById(row.id) ?: return null
        val analysis = pageAnalysisFromSnapshotJson(stored.analysisJson) ?: return null
        return analysis.copy(
            reusedFrom = PageReuse(
                perceptualHash = stored.perceptualHash,
                hammingDistance = distance,
                processedAt = stored.processedAt
            )
        )
    }

    suspend fun remember(courseId: Long, hash: String, photoPath: String, analysis: PageAnalysis) {
        dao.insert(
            ProcessedPageEntity(
                courseId = courseId,
                perceptualHash = hash,
                photoPath = photoPath,
                analysisJson = analysis.toSnapshotJson(),
                processedAt = System.currentTimeMillis()
            )
        )
    }
}
