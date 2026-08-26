package cr.ac.una.glifo.navigation

sealed class Screen(val route: String) {
    // A — Acceso
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")

    // B — Inicio por rol
    data object Home : Screen("home")
    data object HomeTeacher : Screen("home_teacher")
    data object HomeAdmin : Screen("home_admin")

    // C — Cursos Estudiante
    data object Courses : Screen("courses")
    data object JoinCourse : Screen("join_course")
    data object CourseDetail : Screen("course/{courseId}") {
        fun createRoute(courseId: Long = 1L) = "course/$courseId"
    }

    // D — Captura
    data object Capture : Screen("capture/{courseId}") {
        fun createRoute(courseId: Long = 1L) = "capture/$courseId"
    }
    data object CaptureReview : Screen("capture_review?photoPath={photoPath}") {
        fun createRoute(photoPath: String? = null) =
            if (photoPath != null) "capture_review?photoPath=${java.net.URLEncoder.encode(photoPath, "UTF-8")}"
            else "capture_review"
    }
    data object CaptureProcessing : Screen("capture_processing")

    // E — Apuntes
    data object NoteList : Screen("notes/{courseId}") {
        fun createRoute(courseId: Long = 1L) = "notes/$courseId"
    }
    data object NoteDetail : Screen("note/{noteId}") {
        fun createRoute(noteId: Long = 1L) = "note/$noteId"
    }
    data object CorrectFragment : Screen("correct_fragment/{noteId}") {
        fun createRoute(noteId: Long = 1L) = "correct_fragment/$noteId"
    }
    data object FormulaViewer : Screen("formula_viewer/{noteId}") {
        fun createRoute(noteId: Long = 1L) = "formula_viewer/$noteId"
    }
    data object RegionProvenance : Screen("region_provenance/{noteId}") {
        fun createRoute(noteId: Long = 1L) = "region_provenance/$noteId"
    }

    // F — Cobertura
    data object Coverage : Screen("coverage/{courseId}") {
        fun createRoute(courseId: Long = 1L) = "coverage/$courseId"
    }
    data object TopicDetail : Screen("topic_detail/{topicName}") {
        fun createRoute(topicName: String) = "topic_detail/${java.net.URLEncoder.encode(topicName, "UTF-8")}"
    }
    data object CoverageDelta : Screen("coverage_delta")

    // G — Estudio
    data object StudyHub : Screen("study_hub/{courseId}") {
        fun createRoute(courseId: Long = 1L) = "study_hub/$courseId"
    }
    data object FlashcardSession : Screen("flashcards")
    data object QuizSession : Screen("quiz")
    data object QuizResult : Screen("quiz_result")

    // H — Sync e IA
    data object SyncQueue : Screen("sync")
    data object AiCost : Screen("ai_cost")

    // I — Docente
    data object TeacherCourses : Screen("teacher_courses")
    data object TeacherNewCourse : Screen("teacher_new_course")
    data object TeacherSyllabus : Screen("teacher_syllabus")
    data object TeacherGlossary : Screen("teacher_glossary")
    data object TeacherGaps : Screen("teacher_gaps")

    // J — Administración
    data object AdminUsers : Screen("admin_users")
    data object AdminUserDetail : Screen("admin_user_detail/{userName}") {
        fun createRoute(userName: String) = "admin_user_detail/${java.net.URLEncoder.encode(userName, "UTF-8")}"
    }
    data object AdminRoles : Screen("admin_roles")

    // K — Perfil y Ajustes
    data object Settings : Screen("settings")
    data object Profile : Screen("profile")
}
