package cr.ac.una.glifo.core.database.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import cr.ac.una.glifo.core.database.entity.CourseEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class CourseDao_Impl(
  __db: RoomDatabase,
) : CourseDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCourseEntity: EntityInsertAdapter<CourseEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCourseEntity = object : EntityInsertAdapter<CourseEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `courses` (`id`,`name`,`code`,`joinCode`,`emoji`,`syllabusStatus`,`isSynced`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CourseEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.code)
        statement.bindText(4, entity.joinCode)
        val _tmpEmoji: String? = entity.emoji
        if (_tmpEmoji == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpEmoji)
        }
        val _tmpSyllabusStatus: String? = entity.syllabusStatus
        if (_tmpSyllabusStatus == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpSyllabusStatus)
        }
        val _tmp: Int = if (entity.isSynced) 1 else 0
        statement.bindLong(7, _tmp.toLong())
      }
    }
  }

  public override suspend fun insert(course: CourseEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCourseEntity.insert(_connection, course)
  }

  public override suspend fun insertAll(courses: List<CourseEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCourseEntity.insert(_connection, courses)
  }

  public override fun getAll(): Flow<List<CourseEntity>> {
    val _sql: String = "SELECT * FROM courses"
    return createFlow(__db, false, arrayOf("courses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCode: Int = getColumnIndexOrThrow(_stmt, "code")
        val _columnIndexOfJoinCode: Int = getColumnIndexOrThrow(_stmt, "joinCode")
        val _columnIndexOfEmoji: Int = getColumnIndexOrThrow(_stmt, "emoji")
        val _columnIndexOfSyllabusStatus: Int = getColumnIndexOrThrow(_stmt, "syllabusStatus")
        val _columnIndexOfIsSynced: Int = getColumnIndexOrThrow(_stmt, "isSynced")
        val _result: MutableList<CourseEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CourseEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCode: String
          _tmpCode = _stmt.getText(_columnIndexOfCode)
          val _tmpJoinCode: String
          _tmpJoinCode = _stmt.getText(_columnIndexOfJoinCode)
          val _tmpEmoji: String?
          if (_stmt.isNull(_columnIndexOfEmoji)) {
            _tmpEmoji = null
          } else {
            _tmpEmoji = _stmt.getText(_columnIndexOfEmoji)
          }
          val _tmpSyllabusStatus: String?
          if (_stmt.isNull(_columnIndexOfSyllabusStatus)) {
            _tmpSyllabusStatus = null
          } else {
            _tmpSyllabusStatus = _stmt.getText(_columnIndexOfSyllabusStatus)
          }
          val _tmpIsSynced: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsSynced).toInt()
          _tmpIsSynced = _tmp != 0
          _item = CourseEntity(_tmpId,_tmpName,_tmpCode,_tmpJoinCode,_tmpEmoji,_tmpSyllabusStatus,_tmpIsSynced)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): CourseEntity? {
    val _sql: String = "SELECT * FROM courses WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCode: Int = getColumnIndexOrThrow(_stmt, "code")
        val _columnIndexOfJoinCode: Int = getColumnIndexOrThrow(_stmt, "joinCode")
        val _columnIndexOfEmoji: Int = getColumnIndexOrThrow(_stmt, "emoji")
        val _columnIndexOfSyllabusStatus: Int = getColumnIndexOrThrow(_stmt, "syllabusStatus")
        val _columnIndexOfIsSynced: Int = getColumnIndexOrThrow(_stmt, "isSynced")
        val _result: CourseEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCode: String
          _tmpCode = _stmt.getText(_columnIndexOfCode)
          val _tmpJoinCode: String
          _tmpJoinCode = _stmt.getText(_columnIndexOfJoinCode)
          val _tmpEmoji: String?
          if (_stmt.isNull(_columnIndexOfEmoji)) {
            _tmpEmoji = null
          } else {
            _tmpEmoji = _stmt.getText(_columnIndexOfEmoji)
          }
          val _tmpSyllabusStatus: String?
          if (_stmt.isNull(_columnIndexOfSyllabusStatus)) {
            _tmpSyllabusStatus = null
          } else {
            _tmpSyllabusStatus = _stmt.getText(_columnIndexOfSyllabusStatus)
          }
          val _tmpIsSynced: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsSynced).toInt()
          _tmpIsSynced = _tmp != 0
          _result = CourseEntity(_tmpId,_tmpName,_tmpCode,_tmpJoinCode,_tmpEmoji,_tmpSyllabusStatus,_tmpIsSynced)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM courses"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
