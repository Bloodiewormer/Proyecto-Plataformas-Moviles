package cr.ac.una.glifo.core.push

object SyncNotificationFormatter {

    fun formatBodyEs(
        count: Int,
        noteTitle: String? = null,
        courseName: String? = null
    ): String {
        val safeCount = count.coerceAtLeast(1)
        return when {
            safeCount == 1 && !noteTitle.isNullOrBlank() && !courseName.isNullOrBlank() ->
                "Apunte \"$noteTitle\" subido y procesado en $courseName"
            safeCount == 1 && !noteTitle.isNullOrBlank() ->
                "Apunte \"$noteTitle\" subido y procesado"
            safeCount == 1 && !courseName.isNullOrBlank() ->
                "1 apunte subido y procesado en $courseName"
            safeCount == 1 ->
                "1 apunte subido y procesado"
            !courseName.isNullOrBlank() ->
                "$safeCount apuntes subidos y procesados en $courseName"
            else ->
                "$safeCount apuntes subidos y procesados"
        }
    }

    fun formatBodyEn(
        count: Int,
        noteTitle: String? = null,
        courseName: String? = null
    ): String {
        val safeCount = count.coerceAtLeast(1)
        return when {
            safeCount == 1 && !noteTitle.isNullOrBlank() && !courseName.isNullOrBlank() ->
                "Note \"$noteTitle\" uploaded and processed in $courseName"
            safeCount == 1 && !noteTitle.isNullOrBlank() ->
                "Note \"$noteTitle\" uploaded and processed"
            safeCount == 1 && !courseName.isNullOrBlank() ->
                "1 note uploaded and processed in $courseName"
            safeCount == 1 ->
                "1 note uploaded and processed"
            !courseName.isNullOrBlank() ->
                "$safeCount notes uploaded and processed in $courseName"
            else ->
                "$safeCount notes uploaded and processed"
        }
    }
}
