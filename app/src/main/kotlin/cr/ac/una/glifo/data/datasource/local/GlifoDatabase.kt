package cr.ac.una.glifo.data.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase
import cr.ac.una.glifo.data.datasource.local.dao.LocalCourseContextDao
import cr.ac.una.glifo.data.datasource.local.dao.ProcessedPageDao
import cr.ac.una.glifo.data.datasource.local.entity.LocalCourseContextEntity
import cr.ac.una.glifo.data.datasource.local.entity.ProcessedPageEntity

/**
 * Base local de Glifo (Room). Arranca con lo que necesita el pipeline offline: el contexto
 * del curso (#27) y las páginas ya procesadas para deduplicar (#28). El resto del modelado
 * que quedó en `_deferred_module5/` (notas, outbox) entra aquí cuando se retome.
 */
@Database(
    entities = [
        LocalCourseContextEntity::class,
        ProcessedPageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GlifoDatabase : RoomDatabase() {
    abstract fun localCourseContextDao(): LocalCourseContextDao
    abstract fun processedPageDao(): ProcessedPageDao
}
