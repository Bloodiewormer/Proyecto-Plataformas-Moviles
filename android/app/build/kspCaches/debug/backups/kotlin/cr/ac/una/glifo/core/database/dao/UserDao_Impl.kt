package cr.ac.una.glifo.core.database.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import cr.ac.una.glifo.core.database.entity.UserEntity
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class UserDao_Impl(
  __db: RoomDatabase,
) : UserDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfUserEntity: EntityInsertAdapter<UserEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfUserEntity = object : EntityInsertAdapter<UserEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `users` (`id`,`email`,`firstName`,`lastName`,`token`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: UserEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.email)
        val _tmpFirstName: String? = entity.firstName
        if (_tmpFirstName == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpFirstName)
        }
        val _tmpLastName: String? = entity.lastName
        if (_tmpLastName == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpLastName)
        }
        val _tmpToken: String? = entity.token
        if (_tmpToken == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpToken)
        }
      }
    }
  }

  public override suspend fun insert(user: UserEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfUserEntity.insert(_connection, user)
  }

  public override suspend fun getById(id: Long): UserEntity? {
    val _sql: String = "SELECT * FROM users WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfEmail: Int = getColumnIndexOrThrow(_stmt, "email")
        val _columnIndexOfFirstName: Int = getColumnIndexOrThrow(_stmt, "firstName")
        val _columnIndexOfLastName: Int = getColumnIndexOrThrow(_stmt, "lastName")
        val _columnIndexOfToken: Int = getColumnIndexOrThrow(_stmt, "token")
        val _result: UserEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpEmail: String
          _tmpEmail = _stmt.getText(_columnIndexOfEmail)
          val _tmpFirstName: String?
          if (_stmt.isNull(_columnIndexOfFirstName)) {
            _tmpFirstName = null
          } else {
            _tmpFirstName = _stmt.getText(_columnIndexOfFirstName)
          }
          val _tmpLastName: String?
          if (_stmt.isNull(_columnIndexOfLastName)) {
            _tmpLastName = null
          } else {
            _tmpLastName = _stmt.getText(_columnIndexOfLastName)
          }
          val _tmpToken: String?
          if (_stmt.isNull(_columnIndexOfToken)) {
            _tmpToken = null
          } else {
            _tmpToken = _stmt.getText(_columnIndexOfToken)
          }
          _result = UserEntity(_tmpId,_tmpEmail,_tmpFirstName,_tmpLastName,_tmpToken)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getLoggedInUser(): Flow<UserEntity?> {
    val _sql: String = "SELECT * FROM users LIMIT 1"
    return createFlow(__db, false, arrayOf("users")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfEmail: Int = getColumnIndexOrThrow(_stmt, "email")
        val _columnIndexOfFirstName: Int = getColumnIndexOrThrow(_stmt, "firstName")
        val _columnIndexOfLastName: Int = getColumnIndexOrThrow(_stmt, "lastName")
        val _columnIndexOfToken: Int = getColumnIndexOrThrow(_stmt, "token")
        val _result: UserEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpEmail: String
          _tmpEmail = _stmt.getText(_columnIndexOfEmail)
          val _tmpFirstName: String?
          if (_stmt.isNull(_columnIndexOfFirstName)) {
            _tmpFirstName = null
          } else {
            _tmpFirstName = _stmt.getText(_columnIndexOfFirstName)
          }
          val _tmpLastName: String?
          if (_stmt.isNull(_columnIndexOfLastName)) {
            _tmpLastName = null
          } else {
            _tmpLastName = _stmt.getText(_columnIndexOfLastName)
          }
          val _tmpToken: String?
          if (_stmt.isNull(_columnIndexOfToken)) {
            _tmpToken = null
          } else {
            _tmpToken = _stmt.getText(_columnIndexOfToken)
          }
          _result = UserEntity(_tmpId,_tmpEmail,_tmpFirstName,_tmpLastName,_tmpToken)
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
    val _sql: String = "DELETE FROM users"
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
