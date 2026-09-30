package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.LocalCourseContext

interface CourseContextRepository {

    /**
     * Pide el glosario al servidor y actualiza la copia local solo si cambió la versión.
     * Sin conexión devuelve la copia local (source = Cache); si tampoco hay copia, falla.
     */
    suspend fun refresh(courseId: Long): Result<LocalCourseContext>

    /** Solo la copia local, sin red: es lo que usa el pipeline al procesar una captura. */
    suspend fun getCached(courseId: Long): LocalCourseContext?
}
