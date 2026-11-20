package cr.ac.una.glifo.course

import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.course.service.sortedHierarchically
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Pruebas unitarias para el ordenamiento jerárquico de temas del temario.
 */
class SyllabusTopicOrderTest {

    private fun topic(
        id: Long,
        code: String,
        orderIndex: Int,
        parent: SyllabusTopic? = null
    ) = SyllabusTopic().apply {
        this.id = id
        this.code = code
        this.title = "Tema $code"
        this.orderIndex = orderIndex
        this.parent = parent
    }

    @Test
    fun `ordena padres e hijos en profundidad cuando orderIndex se intercala entre dos unidades`() {
        val u1 = topic(1L, "U1", orderIndex = 1)
        val u11 = topic(2L, "U1.1", orderIndex = 1, parent = u1)
        val u12 = topic(3L, "U1.2", orderIndex = 2, parent = u1)
        val u13 = topic(4L, "U1.3", orderIndex = 3, parent = u1)

        val u2 = topic(5L, "U2", orderIndex = 2)
        val u21 = topic(6L, "U2.1", orderIndex = 1, parent = u2)
        val u22 = topic(7L, "U2.2", orderIndex = 2, parent = u2)
        val u23 = topic(8L, "U2.3", orderIndex = 3, parent = u2)

        val interleaved = listOf(u1, u11, u21, u12, u2, u22, u13, u23)

        val ordered = interleaved.sortedHierarchically()

        assertEquals(
            listOf("U1", "U1.1", "U1.2", "U1.3", "U2", "U2.1", "U2.2", "U2.3"),
            ordered.map { it.code }
        )
    }

    @Test
    fun `soporta tres niveles anidados en recorrido profundidad primero`() {
        val u1 = topic(1L, "U1", orderIndex = 1)
        val u11 = topic(2L, "U1.1", orderIndex = 1, parent = u1)
        val u111 = topic(3L, "U1.1.1", orderIndex = 1, parent = u11)
        val u112 = topic(4L, "U1.1.2", orderIndex = 2, parent = u11)
        val u12 = topic(5L, "U1.2", orderIndex = 2, parent = u1)
        val u121 = topic(6L, "U1.2.1", orderIndex = 1, parent = u12)

        val u2 = topic(7L, "U2", orderIndex = 2)
        val u21 = topic(8L, "U2.1", orderIndex = 1, parent = u2)
        val u211 = topic(9L, "U2.1.1", orderIndex = 1, parent = u21)

        val flat = listOf(u211, u112, u2, u121, u11, u21, u1, u12, u111)

        val ordered = flat.sortedHierarchically()

        assertEquals(
            listOf("U1", "U1.1", "U1.1.1", "U1.1.2", "U1.2", "U1.2.1", "U2", "U2.1", "U2.1.1"),
            ordered.map { it.code }
        )
    }

    @Test
    fun `desempata por id cuando orderIndex coincide entre raices o hermanos`() {
        val rootLate = topic(20L, "R20", orderIndex = 1)
        val rootEarly = topic(10L, "R10", orderIndex = 1)
        val childLate = topic(30L, "C30", orderIndex = 2, parent = rootEarly)
        val childEarly = topic(25L, "C25", orderIndex = 2, parent = rootEarly)

        val ordered = listOf(rootLate, childLate, rootEarly, childEarly).sortedHierarchically()

        assertEquals(listOf(10L, 25L, 30L, 20L), ordered.map { it.id })
    }

    @Test
    fun `mantiene huerfanos cuyo padre no esta en la lista al final de forma determinista`() {
        val missingA = topic(901L, "MISSING-A", orderIndex = 1)
        val missingB = topic(902L, "MISSING-B", orderIndex = 2)

        val root = topic(1L, "U1", orderIndex = 2)
        val rootChild = topic(2L, "U1.1", orderIndex = 1, parent = root)

        val orphanB = topic(50L, "ORPHAN-B", orderIndex = 2, parent = missingA)
        val orphanA = topic(60L, "ORPHAN-A", orderIndex = 1, parent = missingB)
        val orphanC = topic(40L, "ORPHAN-C", orderIndex = 2, parent = missingA)
        val orphanAChild = topic(61L, "ORPHAN-A.1", orderIndex = 1, parent = orphanA)

        val ordered = listOf(orphanB, rootChild, orphanA, root, orphanAChild, orphanC).sortedHierarchically()

        assertEquals(
            listOf("U1", "U1.1", "ORPHAN-A", "ORPHAN-A.1", "ORPHAN-C", "ORPHAN-B"),
            ordered.map { it.code }
        )
    }

    @Test
    fun `lista vacia y de un elemento conservan su contenido`() {
        assertEquals(emptyList(), emptyList<SyllabusTopic>().sortedHierarchically())

        val single = topic(1L, "U1", orderIndex = 1)
        assertEquals(listOf(single), listOf(single).sortedHierarchically())
    }
}
