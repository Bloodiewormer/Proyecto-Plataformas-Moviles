package cr.ac.una.glifo.data.di

import android.content.Context
import androidx.room.Room
import cr.ac.una.glifo.data.datasource.local.GlifoDatabase
import cr.ac.una.glifo.data.datasource.local.dao.LocalCourseContextDao
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.local.dao.ProcessedPageDao
import cr.ac.una.glifo.data.datasource.local.dao.StudyItemDao
import cr.ac.una.glifo.data.datasource.local.dao.SyncOperationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * v1 solo tenía cachés que se vuelven a descargar o recalcular (glosario, páginas ya
     * procesadas), así que pasar a v2 borrando esas tablas no pierde nada del estudiante.
     * A partir de v2 hay capturas sin sincronizar: de v2 a v3 se usa [GlifoDatabase.MIGRATION_2_3]
     * para crear solo la tabla `study_items` sin borrar datos existentes.
     */
    @Provides
    @Singleton
    fun provideGlifoDatabase(@ApplicationContext context: Context): GlifoDatabase =
        Room.databaseBuilder(context, GlifoDatabase::class.java, "glifo.db")
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
            .addMigrations(GlifoDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideLocalCourseContextDao(database: GlifoDatabase): LocalCourseContextDao =
        database.localCourseContextDao()

    @Provides
    fun provideProcessedPageDao(database: GlifoDatabase): ProcessedPageDao =
        database.processedPageDao()

    @Provides
    fun provideNoteDao(database: GlifoDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideSyncOperationDao(database: GlifoDatabase): SyncOperationDao = database.syncOperationDao()

    @Provides
    fun provideStudyItemDao(database: GlifoDatabase): StudyItemDao = database.studyItemDao()
}
