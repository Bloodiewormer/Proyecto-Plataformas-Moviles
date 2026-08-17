package cr.ac.una.glifo.di

import android.content.Context
import androidx.room.Room
import cr.ac.una.glifo.core.database.GlifoDatabase
import cr.ac.una.glifo.core.database.dao.CourseDao
import cr.ac.una.glifo.core.database.dao.UserDao
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
    fun provideGlifoDatabase(@ApplicationContext context: Context): GlifoDatabase {
        return Room.databaseBuilder(
            context,
            GlifoDatabase::class.java,
            "glifo_db"
        ).build()
    }

    @Provides
    fun provideUserDao(database: GlifoDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideCourseDao(database: GlifoDatabase): CourseDao {
        return database.courseDao()
    }
}
