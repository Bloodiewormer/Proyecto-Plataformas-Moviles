package cr.ac.una.glifo.core.database.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import cr.ac.una.glifo.core.database.entity.SyncQueueEntity
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class SyncQueueDao_Impl(
  __db: RoomDatabase,
) : SyncQueueDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSyncQueueEntity: EntityInsertAdapter<SyncQueueEntity>

  private val __updateAdapterOfSyncQueueEntity: EntityDeleteOrUpdateAdapter<SyncQueueEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfSyncQueueEntity = object : EntityInsertAdapter<SyncQueueEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `sync_queue` (`id`,`entity_type`,`idempotency_key`,`payload`,`attempts`,`last_error`,`status`,`created_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SyncQueueEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.entityType)
        statement.bindText(3, entity.idempotencyKey)
        statement.bindText(4, entity.payload)
        statement.bindLong(5, entity.attempts.toLong())
        val _tmpLastError: String? = entity.lastError
        if (_tmpLastError == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpLastError)
        }
        statement.bindText(7, entity.status)
        statement.bindLong(8, entity.createdAt)
      }
    }
    this.__updateAdapterOfSyncQueueEntity = object : EntityDeleteOrUpdateAdapter<SyncQueueEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `sync_queue` SET `id` = ?,`entity_type` = ?,`idempotency_key` = ?,`payload` = ?,`attempts` = ?,`last_error` = ?,`status` = ?,`created_at` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SyncQueueEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.entityType)
        statement.bindText(3, entity.idempotencyKey)
        statement.bindText(4, entity.payload)
        statement.bindLong(5, entity.attempts.toLong())
        val _tmpLastError: String? = entity.lastError
        if (_tmpLastError == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpLastError)
        }
        statement.bindText(7, entity.status)
        statement.bindLong(8, entity.createdAt)
        statement.bindLong(9, entity.id)
      }
    }
  }

  public override suspend fun insert(operation: SyncQueueEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSyncQueueEntity.insert(_connection, operation)
  }

  public override suspend fun update(operation: SyncQueueEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfSyncQueueEntity.handle(_connection, operation)
  }

  public override suspend fun getPendingOperations(): List<SyncQueueEntity> {
    val _sql: String = "SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfEntityType: Int = getColumnIndexOrThrow(_stmt, "entity_type")
        val _columnIndexOfIdempotencyKey: Int = getColumnIndexOrThrow(_stmt, "idempotency_key")
        val _columnIndexOfPayload: Int = getColumnIndexOrThrow(_stmt, "payload")
        val _columnIndexOfAttempts: Int = getColumnIndexOrThrow(_stmt, "attempts")
        val _columnIndexOfLastError: Int = getColumnIndexOrThrow(_stmt, "last_error")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<SyncQueueEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SyncQueueEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpEntityType: String
          _tmpEntityType = _stmt.getText(_columnIndexOfEntityType)
          val _tmpIdempotencyKey: String
          _tmpIdempotencyKey = _stmt.getText(_columnIndexOfIdempotencyKey)
          val _tmpPayload: String
          _tmpPayload = _stmt.getText(_columnIndexOfPayload)
          val _tmpAttempts: Int
          _tmpAttempts = _stmt.getLong(_columnIndexOfAttempts).toInt()
          val _tmpLastError: String?
          if (_stmt.isNull(_columnIndexOfLastError)) {
            _tmpLastError = null
          } else {
            _tmpLastError = _stmt.getText(_columnIndexOfLastError)
          }
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = SyncQueueEntity(_tmpId,_tmpEntityType,_tmpIdempotencyKey,_tmpPayload,_tmpAttempts,_tmpLastError,_tmpStatus,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearDoneOperations() {
    val _sql: String = "DELETE FROM sync_queue WHERE status = 'DONE'"
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
