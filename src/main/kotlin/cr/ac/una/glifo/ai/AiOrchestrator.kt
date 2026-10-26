package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.service.GeneratedBatch
import cr.ac.una.glifo.ai.service.GenerationService
import cr.ac.una.glifo.ai.service.TopicInput
import org.springframework.stereotype.Service

@Service
class AiOrchestrator(
    private val generationService: GenerationService
) {
    // Facade for AI services: IA-00, IA-01, IA-02, etc.
    fun generateStudyItems(
        courseName: String,
        topics: List<TopicInput>,
        itemsPerTopic: Int
    ): GeneratedBatch = generationService.generateStudyItems(courseName, topics, itemsPerTopic)
}
