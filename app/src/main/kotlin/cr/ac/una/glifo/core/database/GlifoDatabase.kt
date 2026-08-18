package cr.ac.una.glifo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cr.ac.una.glifo.core.database.converter.Converters
import cr.ac.una.glifo.core.database.dao.CourseDao
import cr.ac.una.glifo.core.database.dao.UserDao
import cr.ac.una.glifo.core.database.entity.CourseEntity
import cr.ac.una.glifo.core.database.entity.UserEntity
import cr.ac.una.glifo.core.database.entity.NoteEntity
import cr.ac.una.glifo.core.database.entity.NotePageEntity
import cr.ac.una.glifo.core.database.entity.StudyItemEntity
import cr.ac.una.glifo.core.database.entity.SyncQueueEntity
import cr.ac.una.glifo.core.database.dao.NoteDao
import cr.ac.una.glifo.core.database.dao.SyncQueueDao

@Database(
    entities = [
        UserEntity::class, 
        CourseEntity::class, 
        NoteEntity::class, 
        NotePageEntity::class, 
        StudyItemEntity::class, 
        SyncQueueEntity::class
    ], 
    version = 2
)
@TypeConverters(Converters::class)
abstract class GlifoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courseDao(): CourseDao
    abstract fun noteDao(): NoteDao
    abstract fun syncQueueDao(): SyncQueueDao
}
