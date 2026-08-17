package cr.ac.una.glifo.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    
    data object CourseDetail : Screen("course/{courseId}") {
        fun createRoute(courseId: Long) = "course/$courseId"
    }
    
    data object NoteList : Screen("notes/{courseId}") {
        fun createRoute(courseId: Long) = "notes/$courseId"
    }
    
    data object NoteDetail : Screen("note/{noteId}") {
        fun createRoute(noteId: Long) = "note/$noteId"
    }
    
    data object Capture : Screen("capture/{courseId}") {
        fun createRoute(courseId: Long) = "capture/$courseId"
    }
    
    data object StudyHub : Screen("study_hub/{courseId}") {
        fun createRoute(courseId: Long) = "study_hub/$courseId"
    }
    
    data object FlashcardSession : Screen("flashcards")
    data object QuizSession : Screen("quiz")
    
    data object Coverage : Screen("coverage/{courseId}") {
        fun createRoute(courseId: Long) = "coverage/$courseId"
    }
    
    data object Settings : Screen("settings")
    data object Profile : Screen("profile")
}
