package cr.ac.una.glifo.data.datasource.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteTopicCache private constructor(
    private val prefs: SharedPreferences?
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    constructor() : this(null)

    private val memory = ConcurrentHashMap<Long, Long>()
    private val recorded = ConcurrentHashMap.newKeySet<Long>()
    private val _changes = MutableStateFlow(0L)
    val changes: Flow<Long> = _changes.asStateFlow()

    init {
        prefs?.all?.forEach { (key, value) ->
            val noteId = key.removePrefix(KEY_PREFIX).toLongOrNull()
            val topicId = (value as? Number)?.toLong()
            if (noteId != null) {
                recorded.add(noteId)
                if (topicId != null && topicId > 0L) {
                    memory[noteId] = topicId
                }
            }
        }
    }

    fun hasRecord(localNoteId: Long): Boolean = localNoteId in recorded

    fun getTopicId(localNoteId: Long): Long? = memory[localNoteId]

    fun setTopicId(localNoteId: Long, topicId: Long?) {
        val validTopicId = topicId?.takeIf { it > 0L }
        val previous = memory[localNoteId]
        val wasRecorded = !recorded.add(localNoteId)
        if (validTopicId != null) {
            memory[localNoteId] = validTopicId
            prefs?.edit()?.putLong("$KEY_PREFIX$localNoteId", validTopicId)?.apply()
        } else {
            memory.remove(localNoteId)
            prefs?.edit()?.putLong("$KEY_PREFIX$localNoteId", NO_TOPIC)?.apply()
        }
        if (!wasRecorded || previous != validTopicId) {
            _changes.update { it + 1L }
        }
    }

    fun remove(localNoteId: Long) {
        memory.remove(localNoteId)
        recorded.remove(localNoteId)
        prefs?.edit()?.remove("$KEY_PREFIX$localNoteId")?.apply()
        _changes.update { it + 1L }
    }

    private companion object {
        const val PREFS_NAME = "glifo_note_topics"
        const val KEY_PREFIX = "note_"
        const val NO_TOPIC = -1L
    }
}
