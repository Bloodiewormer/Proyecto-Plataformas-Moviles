package cr.ac.una.glifo.ai.validation

import org.springframework.stereotype.Component

@Component
class LatexValidator {
    fun compiles(latex: String): Boolean {
        return true
    }
}
