package cr.ac.una.glifo.core.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class PerceptualHasherTest {

    private val size = PerceptualHasher.SAMPLE_SIZE

    /**
     * Una "página" sintética: renglones de trazos de largo y grosor irregulares, como una
     * hoja escrita a mano. La semilla fija la hace reproducible. (Una página perfectamente
     * uniforme no sirve: deja casi todos los coeficientes DCT en cero, empatados con la
     * mediana, algo que una foto real no tiene.)
     */
    private fun handwrittenPage(seed: Int): DoubleArray {
        val random = Random(seed)
        val page = DoubleArray(size * size) { 230.0 }
        for (row in 2 until size - 2 step 3) {
            val start = random.nextInt(1, 6)
            val end = random.nextInt(start + 8, size - 1)
            for (col in start until end) {
                if (random.nextDouble() < 0.8) page[row * size + col] = random.nextDouble(20.0, 90.0)
            }
        }
        return page
    }

    /** Otra página sin relación: un patrón diagonal, como un diagrama. */
    private fun diagramPage(): DoubleArray = DoubleArray(size * size) { i ->
        val row = i / size
        val col = i % size
        128 + 100 * sin((row + col) * PI / 6)
    }

    @Test
    fun `hash is 16 hex characters`() {
        val hash = PerceptualHasher.hashFromLuminance(handwrittenPage(seed = 7))

        assertEquals(16, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `same page gives the same hash`() {
        val a = PerceptualHasher.hashFromLuminance(handwrittenPage(seed = 7))
        val b = PerceptualHasher.hashFromLuminance(handwrittenPage(seed = 7))

        assertEquals(0, PerceptualHasher.hammingDistance(a, b))
    }

    @Test
    fun `uniform brightness change does not change the hash`() {
        val original = handwrittenPage(seed = 7)
        val brighter = DoubleArray(original.size) { original[it] + 20.0 }

        val distance = PerceptualHasher.hammingDistance(
            PerceptualHasher.hashFromLuminance(original),
            PerceptualHasher.hashFromLuminance(brighter)
        )

        assertEquals(0, distance)
    }

    @Test
    fun `sensor noise on the same page stays within the duplicate threshold`() {
        val random = Random(seed = 42)
        val original = handwrittenPage(seed = 7)
        val noisy = DoubleArray(original.size) { original[it] + random.nextDouble(-12.0, 12.0) }

        val a = PerceptualHasher.hashFromLuminance(original)
        val b = PerceptualHasher.hashFromLuminance(noisy)

        assertTrue(PerceptualHasher.isSamePage(a, b))
    }

    @Test
    fun `different pages are far apart`() {
        val notes = PerceptualHasher.hashFromLuminance(handwrittenPage(seed = 7))
        val diagram = PerceptualHasher.hashFromLuminance(diagramPage())

        assertFalse(PerceptualHasher.isSamePage(notes, diagram))
    }

    @Test
    fun `two different handwritten pages are not confused`() {
        val first = PerceptualHasher.hashFromLuminance(handwrittenPage(seed = 7))
        val second = PerceptualHasher.hashFromLuminance(handwrittenPage(seed = 8))

        assertFalse(PerceptualHasher.isSamePage(first, second))
    }

    @Test
    fun `hamming distance is symmetric and counts differing bits`() {
        assertEquals(0, PerceptualHasher.hammingDistance("0000000000000000", "0000000000000000"))
        assertEquals(64, PerceptualHasher.hammingDistance("0000000000000000", "ffffffffffffffff"))
        assertEquals(4, PerceptualHasher.hammingDistance("000000000000000f", "0000000000000000"))
        assertEquals(
            PerceptualHasher.hammingDistance("8000000000000001", "0000000000000000"),
            PerceptualHasher.hammingDistance("0000000000000000", "8000000000000001")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects input that is not 32 by 32`() {
        PerceptualHasher.hashFromLuminance(DoubleArray(10))
    }
}
