package cr.ac.una.glifo.pipeline.model

enum class ProcessingLevel {
    N0, // Preprocessing
    N1, // Local OCR (ML Kit)
    N1_5, // Math OCR
    N2, // Vision Repair (Batch)
    N3, // Full Page Vision
    UNRESOLVED
}
