package cr.ac.una.glifo.ai.validation

import org.scilab.forge.jlatexmath.JMathTeXException
import org.scilab.forge.jlatexmath.ResourceParseException
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Validación sintáctica determinista de LaTeX (issue #39): decide si la fórmula que devolvió
 * N1.5 compila con el parser real de JLaTeXMath, sin red, API keys ni IA.
 *
 * "Compila" significa que JLaTeXMath la parsea Y arma su caja de composición (TeXIcon). Se
 * construye la caja porque algunos errores aparecen al componer y no al parsear, y porque
 * es lo mismo que necesitaría un visor; no se pinta ninguna imagen, así que no hace falta
 * pantalla (corre en modo headless, que Spring Boot activa por defecto).
 *
 * Política de delimitadores: se acepta como mucho UN par externo `$…$`, `$$…$$`, `\(…\)` o
 * `\[…\]` alrededor de toda la fórmula y se quita antes de parsear. Cualquier otro
 * delimitador de modo matemático dentro del cuerpo (un `$` suelto, `\(`, `\]`…) la rechaza:
 * la fórmula ya está en modo matemático y JLaTeXMath los tolera sin avisar. No se corrige ni
 * reescribe nada; el resultado es solo sí/no.
 *
 * Límites (lo que esto NO garantiza):
 * - Aceptar una fórmula no dice que sea matemáticamente correcta ni que coincida con la
 *   imagen original; solo que es LaTeX que JLaTeXMath sabe componer.
 * - JLaTeXMath es permisivo: acepta argumentos faltantes (`\frac{a}`, `x^`) y grupos sin
 *   cerrar (`{x`, `\sqrt{`). Esas fórmulas pasan y no escalan por sintaxis.
 * - Solo conoce los comandos y entornos que trae (por ejemplo, no `\begin{equation}` ni
 *   `\color`); un LaTeX válido para pdflatex puede no compilar aquí.
 */
@Component
class LatexValidator {

    private val log = LoggerFactory.getLogger(LatexValidator::class.java)

    fun compiles(latex: String): Boolean {
        val body = stripOuterDelimiters(latex.trim())
        if (body.isBlank()) return false

        val lexemes = scan(body)
        if (lexemes.hasMathShift || lexemes.controlSymbols.any { it in MATH_DELIMITER_SYMBOLS }) return false
        // Estos comandos cambian tablas estáticas de JLaTeXMath (o leen archivos): una
        // fórmula podría alterar cómo se valida la siguiente. No se ejecutan nunca.
        if (lexemes.controlWords.any { it in STATEFUL_COMMANDS }) return false

        return try {
            TeXFormula(body).createTeXIcon(TeXConstants.STYLE_DISPLAY, RENDER_SIZE)
            true
        } catch (e: ResourceParseException) {
            // Falla al cargar los recursos de la propia biblioteca: no es culpa de la fórmula.
            throw e
        } catch (e: JMathTeXException) {
            log.debug("LaTeX rechazado por JLaTeXMath: {}", e.message)
            false
        }
    }

    /** Quita un único par de delimitadores que envuelva toda la fórmula, si lo hay. */
    internal fun stripOuterDelimiters(latex: String): String {
        val pair = OUTER_DELIMITERS.firstOrNull { (open, close) ->
            latex.length >= open.length + close.length && latex.startsWith(open) && latex.endsWith(close)
        } ?: return latex
        return latex.substring(pair.first.length, latex.length - pair.second.length)
    }

    /**
     * Lee la fórmula como lo haría TeX: `\nombre` es una palabra de control, `\` + otro
     * carácter es un símbolo de control (así `\\` y `\$` no se confunden con delimitadores) y
     * `%` comenta hasta el final de la línea. No reemplaza al parser: solo extrae lo que hace
     * falta para la política de delimitadores y de comandos con estado.
     */
    private fun scan(body: String): Lexemes {
        val words = mutableSetOf<String>()
        val symbols = mutableSetOf<Char>()
        var mathShift = false
        var i = 0
        while (i < body.length) {
            val c = body[i]
            when {
                c == '%' -> {
                    while (i < body.length && body[i] != '\n') i++
                }
                c == '$' -> {
                    mathShift = true
                    i++
                }
                c == '\\' && i + 1 < body.length && body[i + 1].isAsciiLetter() -> {
                    val start = i + 1
                    i = start
                    while (i < body.length && body[i].isAsciiLetter()) i++
                    words += body.substring(start, i)
                }
                c == '\\' && i + 1 < body.length -> {
                    symbols += body[i + 1]
                    i += 2
                }
                else -> i++
            }
        }
        return Lexemes(words, symbols, mathShift)
    }

    /** En TeX las palabras de control solo llevan letras ASCII; `\é` es un símbolo de control. */
    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

    private data class Lexemes(
        val controlWords: Set<String>,
        val controlSymbols: Set<Char>,
        val hasMathShift: Boolean
    )

    private companion object {
        /** Tamaño en puntos de la caja de composición; no cambia el resultado, solo la escala. */
        const val RENDER_SIZE = 20f

        /** En orden: `$$` antes que `$` para no partir `$$x$$` en `$x$`. */
        val OUTER_DELIMITERS = listOf("$$" to "$$", "\\[" to "\\]", "\\(" to "\\)", "$" to "$")

        /** `\(`, `\)`, `\[`, `\]` dentro del cuerpo. */
        val MATH_DELIMITER_SYMBOLS = setOf('(', ')', '[', ']')

        /**
         * Comandos de JLaTeXMath 1.0.7 que modifican estado global compartido entre llamadas
         * (macros, entornos, colores, tamaños, fuentes) o que hacen E/S (archivos, XML,
         * convertidores externos). Con `\newcommand{\frac}{q}` cada `\frac` posterior del
         * proceso cambiaría de significado.
         */
        val STATEFUL_COMMANDS = setOf(
            "newcommand", "renewcommand", "newenvironment", "renewenvironment", "DeclareMathOperator",
            "definecolor", "DeclareMathSizes", "magnification", "jlmExternalFont",
            "includegraphics", "jlmXML", "jlmDynamic"
        )
    }
}
