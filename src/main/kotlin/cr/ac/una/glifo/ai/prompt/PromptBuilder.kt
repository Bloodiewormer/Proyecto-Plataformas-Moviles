package cr.ac.una.glifo.ai.prompt

class PromptBuilder {
    private var role: String = ""
    private var courseName: String? = null
    private val topics: MutableList<String> = mutableListOf()
    private val glossary: MutableList<String> = mutableListOf()
    private val regions: MutableList<String> = mutableListOf()
    private val constraints: MutableList<String> = mutableListOf()
    private var outputSchema: String? = null

    fun withRole(text: String): PromptBuilder = apply {
        this.role = text.trim()
    }

    fun withCourse(name: String): PromptBuilder = apply {
        this.courseName = name.trim()
    }

    fun withTopics(topics: List<String>): PromptBuilder = apply {
        this.topics.clear()
        this.topics.addAll(topics.map { it.trim() }.filter { it.isNotEmpty() })
    }

    fun withGlossary(glossary: List<String>?): PromptBuilder = apply {
        this.glossary.clear()
        if (!glossary.isNullOrEmpty()) {
            this.glossary.addAll(glossary.map { it.trim() }.filter { it.isNotEmpty() })
        }
    }

    fun withRegions(regions: List<String>): PromptBuilder = apply {
        this.regions.clear()
        this.regions.addAll(regions.map { it.trim() }.filter { it.isNotEmpty() })
    }

    fun withConstraints(constraints: List<String>): PromptBuilder = apply {
        this.constraints.clear()
        this.constraints.addAll(constraints.map { it.trim() }.filter { it.isNotEmpty() })
    }

    fun withOutputSchema(schemaExample: String): PromptBuilder = apply {
        this.outputSchema = schemaExample.trim()
    }

    fun build(): Pair<String, String> {
        val systemPrompt = buildString {
            if (role.isNotEmpty()) {
                appendLine(role)
            }
            if (constraints.isNotEmpty()) {
                if (isNotEmpty()) appendLine()
                appendLine("Restricciones obligatorias:")
                constraints.forEach { appendLine("- $it") }
            }
            if (!outputSchema.isNullOrEmpty()) {
                if (isNotEmpty()) appendLine()
                appendLine("Respondé únicamente con un objeto JSON válido que respete este esquema:")
                appendLine(outputSchema)
            }
        }.trim()

        val userPrompt = buildString {
            if (!courseName.isNullOrEmpty()) {
                appendLine("Curso: $courseName")
            }
            if (topics.isNotEmpty()) {
                if (isNotEmpty()) appendLine()
                appendLine("Temas del lote:")
                topics.forEach { appendLine("- $it") }
            }
            if (glossary.isNotEmpty()) {
                if (isNotEmpty()) appendLine()
                appendLine("Glosario del curso:")
                glossary.forEach { appendLine("- $it") }
            }
            if (regions.isNotEmpty()) {
                if (isNotEmpty()) appendLine()
                appendLine("Regiones del apunte (en orden de página y posición vertical):")
                regions.forEach { appendLine("- $it") }
            }
        }.trim()

        return systemPrompt to userPrompt
    }
}
