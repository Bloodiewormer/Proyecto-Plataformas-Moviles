package cr.ac.una.glifo.core.push

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncNotificationFormatterTest {

    @Test
    fun `formats Spanish single note with title and course`() {
        val message = SyncNotificationFormatter.formatBodyEs(
            count = 1,
            noteTitle = "Clase 01 - Vectores",
            courseName = "Álgebra Lineal"
        )
        assertEquals("Apunte \"Clase 01 - Vectores\" subido y procesado en Álgebra Lineal", message)
    }

    @Test
    fun `formats Spanish single note with title only`() {
        val message = SyncNotificationFormatter.formatBodyEs(
            count = 1,
            noteTitle = "Clase 01 - Vectores"
        )
        assertEquals("Apunte \"Clase 01 - Vectores\" subido y procesado", message)
    }

    @Test
    fun `formats Spanish single note with course only`() {
        val message = SyncNotificationFormatter.formatBodyEs(
            count = 1,
            courseName = "Física II"
        )
        assertEquals("1 apunte subido y procesado en Física II", message)
    }

    @Test
    fun `formats Spanish single note without extra metadata`() {
        val message = SyncNotificationFormatter.formatBodyEs(count = 1)
        assertEquals("1 apunte subido y procesado", message)
    }

    @Test
    fun `formats Spanish multiple notes with course`() {
        val message = SyncNotificationFormatter.formatBodyEs(
            count = 3,
            courseName = "Física II"
        )
        assertEquals("3 apuntes subidos y procesados en Física II", message)
    }

    @Test
    fun `formats Spanish multiple notes without course`() {
        val message = SyncNotificationFormatter.formatBodyEs(count = 4)
        assertEquals("4 apuntes subidos y procesados", message)
    }

    @Test
    fun `formats English single and multiple notes`() {
        val single = SyncNotificationFormatter.formatBodyEn(
            count = 1,
            noteTitle = "Lecture 1",
            courseName = "Calculus"
        )
        val multiple = SyncNotificationFormatter.formatBodyEn(
            count = 2,
            courseName = "Calculus"
        )

        assertEquals("Note \"Lecture 1\" uploaded and processed in Calculus", single)
        assertEquals("2 notes uploaded and processed in Calculus", multiple)
    }
}
