package cr.ac.una.glifo.domain.model

private val PAGE_REGEX = Regex("""·\s*p\.?\s*(\d+)""", RegexOption.IGNORE_CASE)
private val SUFFIX_REGEX = Regex("""\s*·\s*(p\.?\s*\d+|apunte|note)\s*$""", RegexOption.IGNORE_CASE)

/**
 * Agrupa las operaciones individuales de la cola de sincronización por apunte.
 *
 * Cada apunte se resume en una única fila representativa donde el estado combinado
 * prioriza los fallos o el trabajo pendiente por sobre las operaciones completadas.
 */
fun groupSyncQueueItems(items: List<SyncQueueItem>): List<SyncQueueItem> {
    if (items.isEmpty()) return emptyList()

    val groupedByNote = LinkedHashMap<Long, MutableList<SyncQueueItem>>()
    for (item in items) {
        groupedByNote.getOrPut(item.noteId) { mutableListOf() }.add(item)
    }

    return groupedByNote.map { (noteId, noteItems) ->
        val pages = mutableSetOf<Int>()
        var baseTitle: String? = null

        for (item in noteItems) {
            val pageNum = PAGE_REGEX.find(item.title)?.groupValues?.get(1)?.toIntOrNull()
            if (pageNum != null) {
                pages.add(pageNum)
            }
            if (baseTitle == null) {
                val stripped = item.title.replace(SUFFIX_REGEX, "").trim()
                if (stripped.isNotBlank()) {
                    baseTitle = stripped
                }
            }
        }
        val finalTitle = baseTitle ?: noteItems.first().title

        val combinedStatus = when {
            noteItems.any { it.status == SyncStatus.Failed } -> SyncStatus.Failed
            noteItems.any { it.status == SyncStatus.Syncing } -> SyncStatus.Syncing
            noteItems.any { it.status == SyncStatus.Queued } -> SyncStatus.Queued
            else -> SyncStatus.Done
        }

        val progress = when (combinedStatus) {
            SyncStatus.Syncing -> {
                val syncing = noteItems.filter { it.status == SyncStatus.Syncing }
                if (syncing.isNotEmpty()) {
                    syncing.map { it.progress }.average().toFloat()
                } else 0.5f
            }
            SyncStatus.Done -> 1f
            else -> 0f
        }

        val maxAttempts = noteItems.maxOfOrNull { it.attempts } ?: 0
        val lastError = noteItems.firstOrNull { it.status == SyncStatus.Failed && !it.lastError.isNullOrBlank() }?.lastError
            ?: noteItems.firstOrNull { !it.lastError.isNullOrBlank() }?.lastError

        val timestamp = noteItems.last().timestamp
        val pageCount = if (pages.isNotEmpty()) pages.size else null

        SyncQueueItem(
            noteId = noteId,
            title = finalTitle,
            timestamp = timestamp,
            status = combinedStatus,
            progress = progress,
            attempts = maxAttempts,
            lastError = lastError,
            pageCount = pageCount
        )
    }
}
