package cr.ac.una.glifo.domain.model

sealed interface TopicPickerItem {
    data class UnitHeader(val topic: SyllabusTopic) : TopicPickerItem
    data class TopicOption(val topic: SyllabusTopic, val indented: Boolean) : TopicPickerItem
}

fun SyllabusTopic.displayLabel(): String {
    val cleanCode = code.trim()
    val cleanTitle = title.trim()
    return when {
        cleanCode.isEmpty() -> cleanTitle
        cleanTitle.startsWith(cleanCode) -> cleanTitle
        else -> "$cleanCode $cleanTitle".trim()
    }
}

fun SyllabusTopic.unitHeaderLabel(): String {
    val cleanCode = code.trim()
    val cleanTitle = title.trim()
    return when {
        cleanCode.isEmpty() -> cleanTitle
        cleanTitle.startsWith(cleanCode) -> cleanTitle
        else -> "$cleanCode · $cleanTitle"
    }
}

private val TOPIC_ORDER = compareBy<SyllabusTopic>({ it.orderIndex }, { it.id })

fun buildTopicPickerItems(topics: List<SyllabusTopic>): List<TopicPickerItem> {
    if (topics.isEmpty()) return emptyList()

    val ids = topics.mapTo(HashSet(topics.size)) { it.id }
    val childrenByParent = HashMap<Long, MutableList<SyllabusTopic>>()
    val roots = ArrayList<SyllabusTopic>()
    val orphans = ArrayList<SyllabusTopic>()

    for (topic in topics) {
        val parentId = topic.parentId
        when {
            parentId == null -> roots.add(topic)
            parentId in ids && parentId != topic.id ->
                childrenByParent.getOrPut(parentId) { ArrayList() }.add(topic)
            else -> orphans.add(topic)
        }
    }

    roots.sortWith(TOPIC_ORDER)
    orphans.sortWith(TOPIC_ORDER)
    childrenByParent.values.forEach { it.sortWith(TOPIC_ORDER) }

    val items = ArrayList<TopicPickerItem>(topics.size)
    val visitedIds = HashSet<Long>(topics.size)

    fun visit(node: SyllabusTopic, indented: Boolean) {
        if (!visitedIds.add(node.id)) return
        val children = childrenByParent[node.id].orEmpty()
        if (children.isNotEmpty()) {
            items.add(TopicPickerItem.UnitHeader(node))
            children.forEach { child -> visit(child, indented = true) }
        } else {
            items.add(TopicPickerItem.TopicOption(node, indented = indented))
        }
    }

    roots.forEach { visit(it, indented = false) }
    orphans.forEach { visit(it, indented = false) }

    // Ensure cycles do not drop any topic from the picker.
    if (visitedIds.size < topics.size) {
        topics.filter { it.id !in visitedIds }
            .sortedWith(TOPIC_ORDER)
            .forEach { visit(it, indented = false) }
    }

    return items
}
