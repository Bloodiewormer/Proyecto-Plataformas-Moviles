package cr.ac.una.glifo.core.opencv

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * Pipeline N0: Preprocesamiento de imágenes usando el SDK Nativo de OpenCV.
 * Implementa las operaciones requeridas en el Issue #9.
 */
object ImagePreprocessor {

    /**
     * Aplica el flujo completo de preprocesamiento (Pipeline N0) a un Bitmap.
     */
    fun processPipelineN0(inputBitmap: Bitmap): Bitmap {
        // 1. Convertir Bitmap de Android a Matriz de OpenCV (Mat)
        val src = Mat()
        Utils.bitmapToMat(inputBitmap, src)

        // 2. Corrección de Inclinación (Deskew)
        val deskewed = deskew(src)

        // 3. Ajuste Dinámico de Contraste Local (CLAHE)
        val claheApplied = applyCLAHE(deskewed)

        // 4. Convertir la matriz procesada de vuelta a Bitmap
        val outBitmap = Bitmap.createBitmap(claheApplied.cols(), claheApplied.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(claheApplied, outBitmap)

        // LIBERACIÓN DE MEMORIA NATIVA (Crítico en C++/OpenCV)
        src.release()
        deskewed.release()
        claheApplied.release()

        return outBitmap
    }

    /**
     * Calcula y corrige el ángulo de inclinación del texto en la imagen.
     * Incluye compuertas de seguridad para no rotar erróneamente en ángulos espurios (> 15°).
     */
    private fun deskew(src: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        
        Core.bitwise_not(gray, gray)
        val thresh = Mat()
        Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
        
        val coords = Mat()
        Core.findNonZero(thresh, coords)
        
        if (coords.total() == 0L) {
             gray.release()
             thresh.release()
             coords.release()
             return src.clone()
        }
        
        val points = MatOfPoint2f()
        coords.convertTo(points, CvType.CV_32FC2)
        val box = Imgproc.minAreaRect(points)
        
        var angle = box.angle
        if (box.size.width < box.size.height) {
            if (angle > 45.0) {
                angle -= 90.0
            }
        } else {
            if (angle < -45.0) {
                angle += 90.0
            } else if (angle > 45.0) {
                angle -= 90.0
            }
        }
        
        gray.release()
        thresh.release()
        coords.release()
        points.release()

        // Si la inclinación es menor a 0.5° (despreciable) o mayor a 15° (falso positivo de bordes/sombras),
        // no aplicamos rotación para no desorientar o degradar la imagen para ML Kit.
        if (kotlin.math.abs(angle) < 0.5 || kotlin.math.abs(angle) > 15.0) {
            return src.clone()
        }
        
        val center = box.center
        val rotMat = Imgproc.getRotationMatrix2D(center, angle, 1.0)
        
        val rotated = Mat()
        Imgproc.warpAffine(src, rotated, rotMat, src.size(), Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE)
        rotMat.release()
        
        return rotated
    }

    /**
     * Contrast Limited Adaptive Histogram Equalization (CLAHE).
     * Aplica un suavizado previo suave para evitar amplificar grano o renglones del cuaderno.
     */
    private fun applyCLAHE(src: Mat): Mat {
        val gray = Mat()
        if (src.channels() >= 3) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        } else {
            src.copyTo(gray)
        }
        
        val smoothed = Mat()
        Imgproc.GaussianBlur(gray, smoothed, Size(3.0, 3.0), 0.0)
        gray.release()

        val clahe = Imgproc.createCLAHE(1.8, Size(8.0, 8.0))
        val dst = Mat()
        clahe.apply(smoothed, dst)
        smoothed.release()
        
        clahe.collectGarbage()
        
        val colorDst = Mat()
        Imgproc.cvtColor(dst, colorDst, Imgproc.COLOR_GRAY2RGBA)
        dst.release()
        
        return colorDst
    }
}
