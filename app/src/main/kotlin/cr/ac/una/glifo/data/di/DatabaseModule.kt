package cr.ac.una.glifo.data.di

import android.content.Context
import androidx.room.Room
import cr.ac.una.glifo.data.datasource.local.GlifoDatabase
import cr.ac.una.glifo.data.datasource.local.dao.LocalCourseContextDao
import cr.ac.una.glifo.data.datasource.local.dao.ProcessedPageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGlifoDatabase(@ApplicationContext context: Context): GlifoDatabase =
        Room.databaseBuilder(context, GlifoDatabase::class.java, "glifo.db").build()

    @Provides
    fun provideLocalCourseContextDao(database: GlifoDatabase): LocalCourseContextDao =
        database.localCourseContextDao()

    @Provides
    fun provideProcessedPageDao(database: GlifoDatabase): ProcessedPageDao =
        database.processedPageDao()
}
