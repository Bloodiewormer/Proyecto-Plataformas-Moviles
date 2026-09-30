package cr.ac.una.glifo.data.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase
import cr.ac.una.glifo.data.datasource.local.dao.LocalCourseContextDao
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.local.dao.ProcessedPageDao
import cr.ac.una.glifo.data.datasource.local.dao.SyncOperationDao
import cr.ac.una.glifo.data.datasource.local.entity.FragmentCorrectionEntity
import cr.ac.una.glifo.data.datasource.local.entity.LocalCourseContextEntity
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.data.datasource.local.entity.ProcessedPageEntity
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity

/**
 * Base local de Glifo (Room, D-28).
 *
 * - v1 (#27, #28): contexto del curso y páginas ya procesadas para deduplicar.
 * - v2 (#25, #26): apuntes y páginas (caché de lectura + capturas offline), correcciones
 *   manuales y la cola de sincronización (outbox).
 */
@Database(
    entities = [
        LocalCourseContextEntity::class,
        ProcessedPageEntity::class,
        NoteEntity::class,
        NotePageEntity::class,
        FragmentCorrectionEntity::class,
        SyncOperationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GlifoDatabase : RoomDatabase() {
    abstract fun localCourseContextDao(): LocalCourseContextDao
    abstract fun processedPageDao(): ProcessedPageDao
    abstract fun noteDao(): NoteDao
    abstract fun syncOperationDao(): SyncOperationDao
}
