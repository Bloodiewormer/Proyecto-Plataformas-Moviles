package cr.ac.una.glifo.data.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cr.ac.una.glifo.data.datasource.local.dao.LocalCourseContextDao
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.local.dao.ProcessedPageDao
import cr.ac.una.glifo.data.datasource.local.dao.StudyItemDao
import cr.ac.una.glifo.data.datasource.local.dao.SyncOperationDao
import cr.ac.una.glifo.data.datasource.local.entity.FragmentCorrectionEntity
import cr.ac.una.glifo.data.datasource.local.entity.LocalCourseContextEntity
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.data.datasource.local.entity.ProcessedPageEntity
import cr.ac.una.glifo.data.datasource.local.entity.StudyItemEntity
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity

/**
 * Base local de Glifo (Room, D-28).
 *
 * - v1 (#27, #28): contexto del curso y páginas ya procesadas para deduplicar.
 * - v2 (#25, #26): apuntes y páginas (caché de lectura + capturas offline), correcciones
 *   manuales y la cola de sincronización (outbox).
 * - v3 (#44): caché offline de ítems de estudio (`study_items`) mediante migración real 2 → 3.
 */
@Database(
    entities = [
        LocalCourseContextEntity::class,
        ProcessedPageEntity::class,
        NoteEntity::class,
        NotePageEntity::class,
        FragmentCorrectionEntity::class,
        SyncOperationEntity::class,
        StudyItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GlifoDatabase : RoomDatabase() {
    abstract fun localCourseContextDao(): LocalCourseContextDao
    abstract fun processedPageDao(): ProcessedPageDao
    abstract fun noteDao(): NoteDao
    abstract fun syncOperationDao(): SyncOperationDao
    abstract fun studyItemDao(): StudyItemDao

    companion object {
        /**
         * Migración incremental 2 → 3 (#44): crea únicamente la tabla `study_items` y su índice
         * por `course_id` sin borrar datos existentes.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_items` (
                        `id` INTEGER NOT NULL,
                        `course_id` INTEGER NOT NULL,
                        `syllabus_topic_id` INTEGER NOT NULL,
                        `kind` TEXT NOT NULL,
                        `payload_json` TEXT NOT NULL,
                        `created_at` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_study_items_course_id` ON `study_items` (`course_id`)"
                )
            }
        }
    }
}
