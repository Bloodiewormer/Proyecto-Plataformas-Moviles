package cr.ac.una.glifo.course.service

import cr.ac.una.glifo.course.entity.SyllabusTopic
import java.util.Collections
import java.util.IdentityHashMap

private val TOPIC_ORDER_COMPARATOR = compareBy<SyllabusTopic>({ it.orderIndex }, { it.id })

/**
 * Ordena una lista plana de temas del temario en recorrido preorden (profundidad primero):
 * cada tema padre seguido recursivamente por sus hijos, ordenando hermanos por `orderIndex`
 * y luego `id`. Los huérfanos cuyo padre no está en la lista quedan al final de forma determinista.
 */
fun List<SyllabusTopic>.sortedHierarchically(): List<SyllabusTopic> {
    if (size <= 1) return this.toList()

    val ids = mapTo(HashSet(size)) { it.id }
    val childrenByParent = HashMap<Long, MutableList<SyllabusTopic>>()
    val roots = ArrayList<SyllabusTopic>()
    val orphans = ArrayList<SyllabusTopic>()

    for (topic in this) {
        val parentId = topic.parent?.id
        when {
            parentId == null -> roots.add(topic)
            parentId in ids && parentId != topic.id ->
                childrenByParent.getOrPut(parentId) { ArrayList() }.add(topic)
            else -> orphans.add(topic)
        }
    }

    roots.sortWith(TOPIC_ORDER_COMPARATOR)
    orphans.sortWith(TOPIC_ORDER_COMPARATOR)
    childrenByParent.values.forEach { it.sortWith(TOPIC_ORDER_COMPARATOR) }

    val ordered = ArrayList<SyllabusTopic>(size)
    val visited = Collections.newSetFromMap(IdentityHashMap<SyllabusTopic, Boolean>())

    fun visit(node: SyllabusTopic) {
        if (!visited.add(node)) return
        ordered.add(node)
        childrenByParent[node.id]?.forEach(::visit)
    }

    roots.forEach(::visit)
    orphans.forEach(::visit)

    // Respaldo determinista si hubiera ciclos en parent_id para no omitir nodos.
    if (ordered.size < size) {
        this.filter { it !in visited }
            .sortedWith(TOPIC_ORDER_COMPARATOR)
            .forEach(::visit)
    }

    return ordered
}
