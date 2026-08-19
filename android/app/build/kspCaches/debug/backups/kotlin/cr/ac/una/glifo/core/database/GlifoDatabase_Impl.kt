package cr.ac.una.glifo.core.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import cr.ac.una.glifo.core.database.dao.CourseDao
import cr.ac.una.glifo.core.database.dao.CourseDao_Impl
import cr.ac.una.glifo.core.database.dao.NoteDao
import cr.ac.una.glifo.core.database.dao.NoteDao_Impl
import cr.ac.una.glifo.core.database.dao.SyncQueueDao
import cr.ac.una.glifo.core.database.dao.SyncQueueDao_Impl
import cr.ac.una.glifo.core.database.dao.UserDao
import cr.ac.una.glifo.core.database.dao.UserDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class GlifoDatabase_Impl : GlifoDatabase() {
  private val _userDao: Lazy<UserDao> = lazy {
    UserDao_Impl(this)
  }

  private val _courseDao: Lazy<CourseDao> = lazy {
    CourseDao_Impl(this)
  }

  private val _noteDao: Lazy<NoteDao> = lazy {
    NoteDao_Impl(this)
  }

  private val _syncQueueDao: Lazy<SyncQueueDao> = lazy {
    SyncQueueDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2, "d768d09f4e25489e9a4ccc5e94b8a6fd", "eb3b6c70817c6befbf96bcf06052b227") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER NOT NULL, `email` TEXT NOT NULL, `firstName` TEXT, `lastName` TEXT, `token` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `courses` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `code` TEXT NOT NULL, `joinCode` TEXT NOT NULL, `emoji` TEXT, `syllabusStatus` TEXT, `isSynced` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER NOT NULL, `user_id` INTEGER NOT NULL, `course_id` INTEGER NOT NULL, `syllabus_topic_id` INTEGER, `class_date` TEXT NOT NULL, `title` TEXT NOT NULL, `status` TEXT NOT NULL, `content` TEXT, `content_generated_at` TEXT, `created_at` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `note_pages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `note_id` INTEGER NOT NULL, `page_index` INTEGER NOT NULL, `perceptual_hash` TEXT NOT NULL, `storage_uri` TEXT NOT NULL, `level_reached` TEXT NOT NULL, `overall_confidence` REAL NOT NULL, `quality_metrics` TEXT, `regions` TEXT, `processed_at` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `study_items` (`id` INTEGER NOT NULL, `course_id` INTEGER NOT NULL, `syllabus_topic_id` INTEGER NOT NULL, `kind` TEXT NOT NULL, `payload` TEXT NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sync_queue` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entity_type` TEXT NOT NULL, `idempotency_key` TEXT NOT NULL, `payload` TEXT NOT NULL, `attempts` INTEGER NOT NULL, `last_error` TEXT, `status` TEXT NOT NULL, `created_at` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd768d09f4e25489e9a4ccc5e94b8a6fd')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `users`")
        connection.execSQL("DROP TABLE IF EXISTS `courses`")
        connection.execSQL("DROP TABLE IF EXISTS `notes`")
        connection.execSQL("DROP TABLE IF EXISTS `note_pages`")
        connection.execSQL("DROP TABLE IF EXISTS `study_items`")
        connection.execSQL("DROP TABLE IF EXISTS `sync_queue`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsUsers: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsUsers.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("email", TableInfo.Column("email", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("firstName", TableInfo.Column("firstName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("lastName", TableInfo.Column("lastName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("token", TableInfo.Column("token", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysUsers: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesUsers: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoUsers: TableInfo = TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers)
        val _existingUsers: TableInfo = read(connection, "users")
        if (!_infoUsers.equals(_existingUsers)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |users(cr.ac.una.glifo.core.database.entity.UserEntity).
              | Expected:
              |""".trimMargin() + _infoUsers + """
              |
              | Found:
              |""".trimMargin() + _existingUsers)
        }
        val _columnsCourses: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCourses.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("code", TableInfo.Column("code", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("joinCode", TableInfo.Column("joinCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("emoji", TableInfo.Column("emoji", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("syllabusStatus", TableInfo.Column("syllabusStatus", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCourses.put("isSynced", TableInfo.Column("isSynced", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCourses: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCourses: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCourses: TableInfo = TableInfo("courses", _columnsCourses, _foreignKeysCourses, _indicesCourses)
        val _existingCourses: TableInfo = read(connection, "courses")
        if (!_infoCourses.equals(_existingCourses)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |courses(cr.ac.una.glifo.core.database.entity.CourseEntity).
              | Expected:
              |""".trimMargin() + _infoCourses + """
              |
              | Found:
              |""".trimMargin() + _existingCourses)
        }
        val _columnsNotes: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsNotes.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("user_id", TableInfo.Column("user_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("course_id", TableInfo.Column("course_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("syllabus_topic_id", TableInfo.Column("syllabus_topic_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("class_date", TableInfo.Column("class_date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("status", TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("content", TableInfo.Column("content", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("content_generated_at", TableInfo.Column("content_generated_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("created_at", TableInfo.Column("created_at", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysNotes: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesNotes: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoNotes: TableInfo = TableInfo("notes", _columnsNotes, _foreignKeysNotes, _indicesNotes)
        val _existingNotes: TableInfo = read(connection, "notes")
        if (!_infoNotes.equals(_existingNotes)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |notes(cr.ac.una.glifo.core.database.entity.NoteEntity).
              | Expected:
              |""".trimMargin() + _infoNotes + """
              |
              | Found:
              |""".trimMargin() + _existingNotes)
        }
        val _columnsNotePages: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsNotePages.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotePages.put("note_id", TableInfo.Column("note_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotePages.put("page_index", TableInfo.Column("page_index", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotePages.put("perceptual_hash", TableInfo.Column("perceptual_hash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotePages.put("storage_uri", TableInfo.Column("storage_uri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotePages.put("level_reached", TableInfo.Column("level_reached", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotePages.put("overall_confidence", TableInfo.Column("overall_confidence", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotePages.put("quality_metrics", TableInfo.Column("quality_metrics", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotePages.put("regions", TableInfo.Column("regions", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotePages.put("processed_at", TableInfo.Column("processed_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysNotePages: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesNotePages: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoNotePages: TableInfo = TableInfo("note_pages", _columnsNotePages, _foreignKeysNotePages, _indicesNotePages)
        val _existingNotePages: TableInfo = read(connection, "note_pages")
        if (!_infoNotePages.equals(_existingNotePages)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |note_pages(cr.ac.una.glifo.core.database.entity.NotePageEntity).
              | Expected:
              |""".trimMargin() + _infoNotePages + """
              |
              | Found:
              |""".trimMargin() + _existingNotePages)
        }
        val _columnsStudyItems: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsStudyItems.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudyItems.put("course_id", TableInfo.Column("course_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudyItems.put("syllabus_topic_id", TableInfo.Column("syllabus_topic_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudyItems.put("kind", TableInfo.Column("kind", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudyItems.put("payload", TableInfo.Column("payload", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudyItems.put("created_at", TableInfo.Column("created_at", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysStudyItems: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesStudyItems: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoStudyItems: TableInfo = TableInfo("study_items", _columnsStudyItems, _foreignKeysStudyItems, _indicesStudyItems)
        val _existingStudyItems: TableInfo = read(connection, "study_items")
        if (!_infoStudyItems.equals(_existingStudyItems)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |study_items(cr.ac.una.glifo.core.database.entity.StudyItemEntity).
              | Expected:
              |""".trimMargin() + _infoStudyItems + """
              |
              | Found:
              |""".trimMargin() + _existingStudyItems)
        }
        val _columnsSyncQueue: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSyncQueue.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSyncQueue.put("entity_type", TableInfo.Column("entity_type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSyncQueue.put("idempotency_key", TableInfo.Column("idempotency_key", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSyncQueue.put("payload", TableInfo.Column("payload", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSyncQueue.put("attempts", TableInfo.Column("attempts", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSyncQueue.put("last_error", TableInfo.Column("last_error", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSyncQueue.put("status", TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSyncQueue.put("created_at", TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSyncQueue: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSyncQueue: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSyncQueue: TableInfo = TableInfo("sync_queue", _columnsSyncQueue, _foreignKeysSyncQueue, _indicesSyncQueue)
        val _existingSyncQueue: TableInfo = read(connection, "sync_queue")
        if (!_infoSyncQueue.equals(_existingSyncQueue)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |sync_queue(cr.ac.una.glifo.core.database.entity.SyncQueueEntity).
              | Expected:
              |""".trimMargin() + _infoSyncQueue + """
              |
              | Found:
              |""".trimMargin() + _existingSyncQueue)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "users", "courses", "notes", "note_pages", "study_items", "sync_queue")
  }

  public override fun clearAllTables() {
    super.performClear(false, "users", "courses", "notes", "note_pages", "study_items", "sync_queue")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(UserDao::class, UserDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CourseDao::class, CourseDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(NoteDao::class, NoteDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(SyncQueueDao::class, SyncQueueDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun userDao(): UserDao = _userDao.value

  public override fun courseDao(): CourseDao = _courseDao.value

  public override fun noteDao(): NoteDao = _noteDao.value

  public override fun syncQueueDao(): SyncQueueDao = _syncQueueDao.value
}
