package cr.ac.una.glifo.data.repository

import android.content.Context
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.data.datasource.local.GlifoDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalUserDataCleaner private constructor(
    private val database: GlifoDatabase?,
    private val activeCourseStore: ActiveCourseStore?,
    private val sessionCacheRegistry: SessionCacheRegistry?,
    private val pagesRootDir: File?,
    private val ioDispatcher: CoroutineDispatcher
) {

    @Inject
    constructor(
        database: GlifoDatabase,
        activeCourseStore: ActiveCourseStore,
        sessionCacheRegistry: SessionCacheRegistry,
        @ApplicationContext appContext: Context
    ) : this(
        database = database,
        activeCourseStore = activeCourseStore,
        sessionCacheRegistry = sessionCacheRegistry,
        pagesRootDir = File(appContext.filesDir, PAGES_DIR),
        ioDispatcher = Dispatchers.IO
    )

    internal constructor(
        database: GlifoDatabase? = null,
        activeCourseStore: ActiveCourseStore? = null,
        sessionCacheRegistry: SessionCacheRegistry? = null,
        pagesRootDir: File? = null,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        @Suppress("UNUSED_PARAMETER") forTest: Unit = Unit
    ) : this(
        database = database,
        activeCourseStore = activeCourseStore,
        sessionCacheRegistry = sessionCacheRegistry,
        pagesRootDir = pagesRootDir,
        ioDispatcher = ioDispatcher
    )

    suspend fun clearUserData() {
        activeCourseStore?.clear()
        sessionCacheRegistry?.clearAll()
        withContext(ioDispatcher) {
            database?.clearAllTables()
            pagesRootDir?.takeIf { it.exists() }?.deleteRecursively()
        }
    }

    private companion object {
        const val PAGES_DIR = "pages"
    }
}
