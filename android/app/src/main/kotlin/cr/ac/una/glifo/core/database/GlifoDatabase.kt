package cr.ac.una.glifo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cr.ac.una.glifo.core.database.converter.Converters
import cr.ac.una.glifo.core.database.dao.CourseDao
import cr.ac.una.glifo.core.database.dao.UserDao
import cr.ac.una.glifo.core.database.entity.CourseEntity
import cr.ac.una.glifo.core.database.entity.UserEntity

@Database(entities = [UserEntity::class, CourseEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class GlifoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courseDao(): CourseDao
}
