package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.engine.RegionCrop
import cr.ac.una.glifo.ai.service.GeneratedBatch
import cr.ac.una.glifo.ai.service.GenerationService
import cr.ac.una.glifo.ai.service.NoteReconstructionOutcome
import cr.ac.una.glifo.ai.service.PageRepairResponse
import cr.ac.una.glifo.ai.service.ReconstructionService
import cr.ac.una.glifo.ai.service.TopicInput
import cr.ac.una.glifo.ai.service.VisionRepairService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AiOrchestrator @Autowired constructor(
    private val generationService: GenerationService,
    private val reconstructionService: ReconstructionService? = null,
    private val visionRepairService: VisionRepairService? = null
) {

    constructor(generationService: GenerationService) : this(generationService, null, null)

    // Facade for AI services: IA-00, IA-01, IA-02, etc.
    fun generateStudyItems(
        courseName: String,
        topics: List<TopicInput>,
        itemsPerTopic: Int
    ): GeneratedBatch = generationService.generateStudyItems(courseName, topics, itemsPerTopic)

    fun reconstructNote(
        noteId: Long,
        userId: Long
    ): NoteReconstructionOutcome {
        val service = reconstructionService
            ?: throw IllegalStateException("ReconstructionService no configurado")
        return service.reconstructNote(noteId, userId)
    }

    fun repairPage(
        noteId: Long,
        userId: Long,
        pageIndex: Int,
        crops: Map<String, RegionCrop>
    ): PageRepairResponse {
        val service = visionRepairService
            ?: throw IllegalStateException("VisionRepairService no configurado")
        return service.repairPage(noteId, userId, pageIndex, crops)
    }
}
