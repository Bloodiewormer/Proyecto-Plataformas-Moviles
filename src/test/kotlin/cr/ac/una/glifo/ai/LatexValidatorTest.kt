package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.validation.LatexValidator
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.awt.GraphicsEnvironment
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pruebas UNITARIAS del validador con JLaTeXMath real: sin Spring, base de datos ni red.
 * Las entradas rechazadas son las que la biblioteca efectivamente rechaza (comprobado con
 * la 1.0.7), no las que un compilador de LaTeX completo rechazaría.
 */
class LatexValidatorTest {

    private val validator = LatexValidator()

    @ParameterizedTest
    @ValueSource(
        strings = [
            """\frac{a}{b}""",
            """\frac{d}{dx}\left[ x^2 \right]""",
            """\sqrt{x}""",
            """\sqrt[3]{x^2 + 1}""",
            """\lim_{x \to a} f(x) = L""",
            """\lim\limits_{n\to\infty} a_n""",
            """\sum_{i=1}^{n} i = \frac{n(n+1)}{2}""",
            """\int_0^1 x^2 \, dx""",
            """\oint_C \vec{F} \cdot d\vec{r}""",
            """\begin{pmatrix} a & b \\ c & d \end{pmatrix}""",
            """\forall \varepsilon > 0\ \exists \delta > 0"""
        ]
    )
    fun `formulas validas compilan`(latex: String) {
        assertTrue(validator.compiles(latex), latex)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "   \t\n ", "$$", "$ $", """\( \)""", """\[ \]"""])
    fun `vacio o en blanco no compila`(latex: String) {
        assertFalse(validator.compiles(latex))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """\foo{x}""",                         // comando desconocido
            """\sen x""",                          // en LaTeX es \sin (o \operatorname{sen})
            """x + y}""",                          // llave de cierre sin abrir
            """\left( x""",                        // \left sin \right
            """x^^2""",                            // doble superíndice mal formado
            """a & b""",                           // & fuera de un arreglo
            """\begin{matrix} a""",                // entorno sin cerrar
            """\begin{foo} a \end{foo}""",         // entorno desconocido
            """\hspace{abc}""",                    // unidad inválida
            """\over"""                            // \over sin operandos
        ]
    )
    fun `JLaTeXMath rechaza comandos desconocidos y expresiones mal formadas`(latex: String) {
        assertFalse(validator.compiles(latex), latex)
    }

    @ParameterizedTest
    @ValueSource(strings = ["""${'$'}\frac{a}{b}${'$'}""", """${'$'}${'$'}\frac{a}{b}${'$'}${'$'}""", """\(\frac{a}{b}\)""", """\[\frac{a}{b}\]""", """  ${'$'}x^2${'$'}  """])
    fun `un unico par de delimitadores externos se acepta`(latex: String) {
        assertTrue(validator.compiles(latex), latex)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """x${'$'}""",                         // $ suelto (JLaTeXMath lo tolera)
            """${'$'}x${'$'} + ${'$'}y${'$'}""",   // dos fórmulas pegadas
            """${'$'}x""",                         // par sin cerrar
            """\(x""",
            """x \] + 1""",
            """${'$'}\(x\)${'$'}"""                // delimitadores anidados
        ]
    )
    fun `delimitadores sueltos o anidados no compilan`(latex: String) {
        assertFalse(validator.compiles(latex), latex)
    }

    @Test
    fun `los delimitadores con barra invertida no se confunden con simbolos escapados`() {
        assertTrue(validator.compiles("""\${'$'}5 + 3\%"""))         // \$ y \% son símbolos
        assertTrue(validator.compiles("""a \\[2pt] b"""))            // salto de línea, no \[
        assertTrue(validator.compiles("""\left[ x \right]"""))
    }

    @Test
    fun `un signo de dolar dentro de un comentario no cuenta como delimitador`() {
        assertTrue(validator.compiles("x + 1 % precio en ${'$'}"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """\newcommand{\frac}{q}""",
            """\renewcommand{\alpha}{z}\alpha""",
            """\newenvironment{qq}{a}{b}""",
            """\DeclareMathOperator{\sen}{sen} \sen x""",
            """\definecolor{rojo}{rgb}{1,0,0}""",
            """\magnification{2} x""",
            """\includegraphics{/etc/passwd}"""
        ]
    )
    fun `comandos que cambian estado global o hacen E-S se rechazan`(latex: String) {
        assertFalse(validator.compiles(latex), latex)
    }

    @Test
    fun `una definicion rechazada no cambia la validacion siguiente`() {
        assertFalse(validator.compiles("""\newcommand{\miMacro}{x}"""))
        // Si JLaTeXMath la hubiera registrado, \miMacro pasaría a ser un comando conocido.
        assertFalse(validator.compiles("""\miMacro"""))
        assertTrue(validator.compiles("""\frac{a}{b}"""))
    }

    @Test
    fun `llamadas repetidas dan el mismo resultado`() {
        val inputs = listOf("""\frac{a}{b}""", """\foo""", "x}", """\int_0^1 x\,dx""", "", """${'$'}x${'$'}""")
        val first = inputs.map { validator.compiles(it) }
        repeat(20) { assertEquals(first, inputs.map { validator.compiles(it) }) }
    }

    @Test
    fun `llamadas concurrentes dan el mismo resultado que en serie`() {
        val inputs = listOf("""\frac{a}{b}""", """\foo""", "x}", """\sum_{i=1}^n i""", """\sqrt{2}""")
        val expected = inputs.associateWith { validator.compiles(it) }
        val pool = Executors.newFixedThreadPool(8)
        try {
            val futures = (0 until 200).map { i -> inputs[i % inputs.size].let { s -> s to pool.submit<Boolean> { validator.compiles(s) } } }
            futures.forEach { (s, f) -> assertEquals(expected[s], f.get(30, TimeUnit.SECONDS), s) }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `corre en modo headless`() {
        // build.gradle.kts arranca las pruebas con java.awt.headless=true, como Spring Boot.
        assertTrue(GraphicsEnvironment.isHeadless())
        assertTrue(validator.compiles("""\int_0^1 \frac{1}{\sqrt{x}}\,dx"""))
        assertFalse(validator.compiles("""\foo"""))
    }

    @Test
    fun `limitacion documentada - JLaTeXMath acepta argumentos faltantes y grupos sin cerrar`() {
        // No es un error del validador: la biblioteca completa los huecos con vacío. Estas
        // fórmulas no escalan por sintaxis (pueden escalar por confianza).
        assertTrue(validator.compiles("""\frac{a}"""))
        assertTrue(validator.compiles("{x"))
        assertTrue(validator.compiles("x^"))
    }
}
