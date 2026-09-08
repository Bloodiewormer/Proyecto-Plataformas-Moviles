package cr.ac.una.glifo.data.di

import cr.ac.una.glifo.data.repository.AdminRepositoryImpl
import cr.ac.una.glifo.data.repository.AiCostRepositoryImpl
import cr.ac.una.glifo.data.repository.AuthRepositoryImpl
import cr.ac.una.glifo.data.repository.CaptureRepositoryImpl
import cr.ac.una.glifo.data.repository.CourseRepositoryImpl
import cr.ac.una.glifo.data.repository.CoverageRepositoryImpl
import cr.ac.una.glifo.data.repository.NoteRepositoryImpl
import cr.ac.una.glifo.data.repository.SessionRepositoryImpl
import cr.ac.una.glifo.data.repository.StudyRepositoryImpl
import cr.ac.una.glifo.data.repository.SyncRepositoryImpl
import cr.ac.una.glifo.data.repository.TeacherRepositoryImpl
import cr.ac.una.glifo.domain.repository.AdminRepository
import cr.ac.una.glifo.domain.repository.AiCostRepository
import cr.ac.una.glifo.domain.repository.AuthRepository
import cr.ac.una.glifo.domain.repository.CaptureRepository
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.CoverageRepository
import cr.ac.una.glifo.domain.repository.NoteRepository
import cr.ac.una.glifo.domain.repository.SessionRepository
import cr.ac.una.glifo.domain.repository.StudyRepository
import cr.ac.una.glifo.domain.repository.SyncRepository
import cr.ac.una.glifo.domain.repository.TeacherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds every domain repository interface to its (currently dummy-backed) implementation.
 * ViewModels inject the `domain.repository.*` interfaces only — never a `data.repository.*`
 * class directly — per the dependency rule in Glifo_Arquitectura_Estandares.md §1.2.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    @Singleton
    abstract fun bindCoverageRepository(impl: CoverageRepositoryImpl): CoverageRepository

    @Binds
    @Singleton
    abstract fun bindStudyRepository(impl: StudyRepositoryImpl): StudyRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindAiCostRepository(impl: AiCostRepositoryImpl): AiCostRepository

    @Binds
    @Singleton
    abstract fun bindTeacherRepository(impl: TeacherRepositoryImpl): TeacherRepository

    @Binds
    @Singleton
    abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository

    @Binds
    @Singleton
    abstract fun bindCaptureRepository(impl: CaptureRepositoryImpl): CaptureRepository
}
