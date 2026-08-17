package cr.ac.una.glifo.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // This module is ready for @Provides methods (e.g. for Retrofit or Room)
}
