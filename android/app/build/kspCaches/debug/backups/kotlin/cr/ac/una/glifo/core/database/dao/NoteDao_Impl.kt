package cr.ac.una.glifo.core.database.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import cr.ac.una.glifo.core.database.entity.NoteEntity
import cr.ac.una.glifo.core.database.entity.NotePageEntity
import javax.`annotation`.processing.Generated
import kotlin.Float
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
public class NoteDao_Impl(
  __db: RoomDatabase,
) : NoteDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfNoteEntity: EntityInsertAdapter<NoteEntity>

  private val __insertAdapterOfNotePageEntity: EntityInsertAdapter<NotePageEntity>

  private val __updateAdapterOfNoteEntity: EntityDeleteOrUpdateAdapter<NoteEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfNoteEntity = object : EntityInsertAdapter<NoteEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `notes` (`id`,`user_id`,`course_id`,`syllabus_topic_id`,`class_date`,`title`,`status`,`content`,`content_generated_at`,`created_at`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: NoteEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.userId)
        statement.bindLong(3, entity.courseId)
        val _tmpSyllabusTopicId: Long? = entity.syllabusTopicId
        if (_tmpSyllabusTopicId == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmpSyllabusTopicId)
        }
        statement.bindText(5, entity.classDate)
        statement.bindText(6, entity.title)
        statement.bindText(7, entity.status)
        val _tmpContent: String? = entity.content
        if (_tmpContent == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpContent)
        }
        val _tmpContentGeneratedAt: String? = entity.contentGeneratedAt
        if (_tmpContentGeneratedAt == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpContentGeneratedAt)
        }
        statement.bindText(10, entity.createdAt)
      }
    }
    this.__insertAdapterOfNotePageEntity = object : EntityInsertAdapter<NotePageEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `note_pages` (`id`,`note_id`,`page_index`,`perceptual_hash`,`storage_uri`,`level_reached`,`overall_confidence`,`quality_metrics`,`regions`,`processed_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: NotePageEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.noteId)
        statement.bindLong(3, entity.pageIndex.toLong())
        statement.bindText(4, entity.perceptualHash)
        statement.bindText(5, entity.storageUri)
        statement.bindText(6, entity.levelReached)
        statement.bindDouble(7, entity.overallConfidence.toDouble())
        val _tmpQualityMetrics: String? = entity.qualityMetrics
        if (_tmpQualityMetrics == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpQualityMetrics)
        }
        val _tmpRegions: String? = entity.regions
        if (_tmpRegions == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpRegions)
        }
        val _tmpProcessedAt: String? = entity.processedAt
        if (_tmpProcessedAt == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpProcessedAt)
        }
      }
    }
    this.__updateAdapterOfNoteEntity = object : EntityDeleteOrUpdateAdapter<NoteEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `notes` SET `id` = ?,`user_id` = ?,`course_id` = ?,`syllabus_topic_id` = ?,`class_date` = ?,`title` = ?,`status` = ?,`content` = ?,`content_generated_at` = ?,`created_at` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: NoteEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.userId)
        statement.bindLong(3, entity.courseId)
        val _tmpSyllabusTopicId: Long? = entity.syllabusTopicId
        if (_tmpSyllabusTopicId == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmpSyllabusTopicId)
        }
        statement.bindText(5, entity.classDate)
        statement.bindText(6, entity.title)
        statement.bindText(7, entity.status)
        val _tmpContent: String? = entity.content
        if (_tmpContent == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpContent)
        }
        val _tmpContentGeneratedAt: String? = entity.contentGeneratedAt
        if (_tmpContentGeneratedAt == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpContentGeneratedAt)
        }
        statement.bindText(10, entity.createdAt)
        statement.bindLong(11, entity.id)
      }
    }
  }

  public override suspend fun insertNote(note: NoteEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfNoteEntity.insert(_connection, note)
  }

  public override suspend fun insertNotePage(page: NotePageEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfNotePageEntity.insert(_connection, page)
  }

  public override suspend fun updateNote(note: NoteEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfNoteEntity.handle(_connection, note)
  }

  public override suspend fun getNotesByCourse(userId: Long, courseId: Long): List<NoteEntity> {
    val _sql: String = "SELECT * FROM notes WHERE user_id = ? AND course_id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, userId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, courseId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "user_id")
        val _columnIndexOfCourseId: Int = getColumnIndexOrThrow(_stmt, "course_id")
        val _columnIndexOfSyllabusTopicId: Int = getColumnIndexOrThrow(_stmt, "syllabus_topic_id")
        val _columnIndexOfClassDate: Int = getColumnIndexOrThrow(_stmt, "class_date")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfContent: Int = getColumnIndexOrThrow(_stmt, "content")
        val _columnIndexOfContentGeneratedAt: Int = getColumnIndexOrThrow(_stmt, "content_generated_at")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<NoteEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: NoteEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpUserId: Long
          _tmpUserId = _stmt.getLong(_columnIndexOfUserId)
          val _tmpCourseId: Long
          _tmpCourseId = _stmt.getLong(_columnIndexOfCourseId)
          val _tmpSyllabusTopicId: Long?
          if (_stmt.isNull(_columnIndexOfSyllabusTopicId)) {
            _tmpSyllabusTopicId = null
          } else {
            _tmpSyllabusTopicId = _stmt.getLong(_columnIndexOfSyllabusTopicId)
          }
          val _tmpClassDate: String
          _tmpClassDate = _stmt.getText(_columnIndexOfClassDate)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpContent: String?
          if (_stmt.isNull(_columnIndexOfContent)) {
            _tmpContent = null
          } else {
            _tmpContent = _stmt.getText(_columnIndexOfContent)
          }
          val _tmpContentGeneratedAt: String?
          if (_stmt.isNull(_columnIndexOfContentGeneratedAt)) {
            _tmpContentGeneratedAt = null
          } else {
            _tmpContentGeneratedAt = _stmt.getText(_columnIndexOfContentGeneratedAt)
          }
          val _tmpCreatedAt: String
          _tmpCreatedAt = _stmt.getText(_columnIndexOfCreatedAt)
          _item = NoteEntity(_tmpId,_tmpUserId,_tmpCourseId,_tmpSyllabusTopicId,_tmpClassDate,_tmpTitle,_tmpStatus,_tmpContent,_tmpContentGeneratedAt,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPagesForNote(noteId: Long): List<NotePageEntity> {
    val _sql: String = "SELECT * FROM note_pages WHERE note_id = ? ORDER BY page_index ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, noteId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfNoteId: Int = getColumnIndexOrThrow(_stmt, "note_id")
        val _columnIndexOfPageIndex: Int = getColumnIndexOrThrow(_stmt, "page_index")
        val _columnIndexOfPerceptualHash: Int = getColumnIndexOrThrow(_stmt, "perceptual_hash")
        val _columnIndexOfStorageUri: Int = getColumnIndexOrThrow(_stmt, "storage_uri")
        val _columnIndexOfLevelReached: Int = getColumnIndexOrThrow(_stmt, "level_reached")
        val _columnIndexOfOverallConfidence: Int = getColumnIndexOrThrow(_stmt, "overall_confidence")
        val _columnIndexOfQualityMetrics: Int = getColumnIndexOrThrow(_stmt, "quality_metrics")
        val _columnIndexOfRegions: Int = getColumnIndexOrThrow(_stmt, "regions")
        val _columnIndexOfProcessedAt: Int = getColumnIndexOrThrow(_stmt, "processed_at")
        val _result: MutableList<NotePageEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: NotePageEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpNoteId: Long
          _tmpNoteId = _stmt.getLong(_columnIndexOfNoteId)
          val _tmpPageIndex: Int
          _tmpPageIndex = _stmt.getLong(_columnIndexOfPageIndex).toInt()
          val _tmpPerceptualHash: String
          _tmpPerceptualHash = _stmt.getText(_columnIndexOfPerceptualHash)
          val _tmpStorageUri: String
          _tmpStorageUri = _stmt.getText(_columnIndexOfStorageUri)
          val _tmpLevelReached: String
          _tmpLevelReached = _stmt.getText(_columnIndexOfLevelReached)
          val _tmpOverallConfidence: Float
          _tmpOverallConfidence = _stmt.getDouble(_columnIndexOfOverallConfidence).toFloat()
          val _tmpQualityMetrics: String?
          if (_stmt.isNull(_columnIndexOfQualityMetrics)) {
            _tmpQualityMetrics = null
          } else {
            _tmpQualityMetrics = _stmt.getText(_columnIndexOfQualityMetrics)
          }
          val _tmpRegions: String?
          if (_stmt.isNull(_columnIndexOfRegions)) {
            _tmpRegions = null
          } else {
            _tmpRegions = _stmt.getText(_columnIndexOfRegions)
          }
          val _tmpProcessedAt: String?
          if (_stmt.isNull(_columnIndexOfProcessedAt)) {
            _tmpProcessedAt = null
          } else {
            _tmpProcessedAt = _stmt.getText(_columnIndexOfProcessedAt)
          }
          _item = NotePageEntity(_tmpId,_tmpNoteId,_tmpPageIndex,_tmpPerceptualHash,_tmpStorageUri,_tmpLevelReached,_tmpOverallConfidence,_tmpQualityMetrics,_tmpRegions,_tmpProcessedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
