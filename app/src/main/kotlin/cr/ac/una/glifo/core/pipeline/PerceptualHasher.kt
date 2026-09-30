package cr.ac.una.glifo.core.pipeline

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Hash perceptual (pHash por DCT) de una página, para detectar que ya se procesó y no volver a
 * gastar N0 + N1 en ella (issue #28, Glifo_Alcance.md §5: "page already processed → reused,
 * zero calls").
 *
 * A diferencia de un hash criptográfico, dos fotos de la misma hoja tomadas con otro encuadre
 * o luz dan hashes casi iguales: se comparan por distancia de Hamming, no por igualdad.
 *
 * 1. Se reduce la imagen a 32×32 en escala de grises (se pierden detalles, queda la forma).
 * 2. DCT 2D; se toman las frecuencias más bajas (8×8), que describen la estructura global.
 * 3. Cada bit dice si ese coeficiente está sobre la mediana → 64 bits → 16 caracteres hex,
 *    el mismo formato que `note_pages.perceptual_hash CHAR(16)` en el backend.
 *
 * El componente DC (brillo promedio) se excluye de la mediana, así que aclarar u oscurecer
 * toda la foto no cambia el hash.
 */
object PerceptualHasher {

    const val SAMPLE_SIZE = 32
    private const val LOW_FREQ = 8

    /**
     * Distancia máxima (de 64 bits) para considerar dos capturas la misma página. Medido en
     * GlifoDatabaseTest con hojas de apuntes dibujadas: volver a fotografiar la misma hoja dio
     * 0 (otra luz), 6 (rotada 1,5°) y 14 (corrida ~1,5 %); hojas distintas dieron 24–32.
     *
     * El error caro es confundir dos hojas distintas (el estudiante perdería una), así que el
     * umbral queda más cerca de las repeticiones que de las hojas distintas. pHash no es
     * invariante a encuadres muy distintos: detecta dobles disparos, reintentos con un
     * encuadre parecido y la misma foto importada dos veces, no una hoja reencuadrada de cero.
     */
    const val DUPLICATE_THRESHOLD = 16

    fun hash(bitmap: Bitmap): String {
        val scaled = downsample(bitmap)
        val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
        scaled.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
        if (scaled !== bitmap) scaled.recycle()

        val luminance = DoubleArray(pixels.size) { i ->
            val c = pixels[i]
            0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)
        }
        return hashFromLuminance(luminance)
    }

    /** Parte pura del algoritmo: recibe 32×32 valores de luminancia en orden de filas. */
    fun hashFromLuminance(luminance: DoubleArray): String {
        require(luminance.size == SAMPLE_SIZE * SAMPLE_SIZE) {
            "Se esperaban ${SAMPLE_SIZE * SAMPLE_SIZE} valores, llegaron ${luminance.size}"
        }

        val lowFrequencies = DoubleArray(LOW_FREQ * LOW_FREQ)
        for (u in 0 until LOW_FREQ) {
            for (v in 0 until LOW_FREQ) {
                lowFrequencies[u * LOW_FREQ + v] = dctCoefficient(luminance, u, v)
            }
        }

        // 63 coeficientes sin el DC: la mediana es el del medio.
        val median = lowFrequencies.drop(1).sorted().let { it[it.size / 2] }

        var bits = 0UL
        lowFrequencies.forEachIndexed { index, value ->
            if (index > 0 && value > median) bits = bits or (1UL shl (63 - index))
        }
        return bits.toString(16).padStart(16, '0')
    }

    /**
     * Reduce a 32×32 promediando de verdad. Un solo `createScaledBitmap` de una foto de miles
     * de píxeles a 32 lee apenas unos pocos píxeles por celda: los renglones de texto generan
     * aliasing y correr la hoja unos milímetros cambiaba medio hash. Cada reducción a la mitad
     * con filtro sí promedia bloques de 2×2, así que se encadenan hasta quedar cerca de 32.
     */
    private fun downsample(bitmap: Bitmap): Bitmap {
        var current = bitmap
        while (current.width >= SAMPLE_SIZE * 4 && current.height >= SAMPLE_SIZE * 4) {
            val half = Bitmap.createScaledBitmap(current, current.width / 2, current.height / 2, true)
            if (current !== bitmap) current.recycle()
            current = half
        }
        val result = Bitmap.createScaledBitmap(current, SAMPLE_SIZE, SAMPLE_SIZE, true)
        if (current !== bitmap && current !== result) current.recycle()
        return result
    }

    fun hammingDistance(a: String, b: String): Int =
        java.lang.Long.bitCount((a.toULong(16) xor b.toULong(16)).toLong())

    fun isSamePage(a: String, b: String): Boolean = hammingDistance(a, b) <= DUPLICATE_THRESHOLD

    private fun dctCoefficient(pixels: DoubleArray, u: Int, v: Int): Double {
        var sum = 0.0
        for (x in 0 until SAMPLE_SIZE) {
            val cosU = cos((2 * x + 1) * u * PI / (2 * SAMPLE_SIZE))
            for (y in 0 until SAMPLE_SIZE) {
                sum += pixels[x * SAMPLE_SIZE + y] * cosU * cos((2 * y + 1) * v * PI / (2 * SAMPLE_SIZE))
            }
        }
        val cu = if (u == 0) sqrt(1.0 / SAMPLE_SIZE) else sqrt(2.0 / SAMPLE_SIZE)
        val cv = if (v == 0) sqrt(1.0 / SAMPLE_SIZE) else sqrt(2.0 / SAMPLE_SIZE)
        return cu * cv * sum
    }
}
