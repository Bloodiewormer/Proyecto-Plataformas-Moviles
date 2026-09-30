package cr.ac.una.glifo.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cr.ac.una.glifo.core.ui.component.ActiveCourseUi
import cr.ac.una.glifo.core.ui.component.COURSE_SWITCHER_ROUTE
import cr.ac.una.glifo.core.ui.component.LocalActiveCourse
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.presentation.ui.screen.AdminCoursesScreen
import cr.ac.una.glifo.presentation.ui.screen.AdminRolesScreen
import cr.ac.una.glifo.presentation.ui.screen.AdminUserDetailScreen
import cr.ac.una.glifo.presentation.ui.screen.AdminUsersScreen
import cr.ac.una.glifo.presentation.ui.screen.ForgotPasswordScreen
import cr.ac.una.glifo.presentation.ui.screen.LoginScreen
import cr.ac.una.glifo.presentation.ui.screen.RegisterScreen
import cr.ac.una.glifo.presentation.ui.screen.SplashScreen
import cr.ac.una.glifo.presentation.ui.screen.CaptureProcessingScreen
import cr.ac.una.glifo.presentation.ui.screen.CaptureReviewScreen
import cr.ac.una.glifo.presentation.ui.screen.CaptureScreen
import cr.ac.una.glifo.presentation.ui.screen.CourseDetailScreen
import cr.ac.una.glifo.presentation.ui.screen.CoursesScreen
import cr.ac.una.glifo.presentation.ui.screen.JoinCourseScreen
import cr.ac.una.glifo.presentation.ui.screen.CoverageDeltaScreen
import cr.ac.una.glifo.presentation.ui.screen.CoverageScreen
import cr.ac.una.glifo.presentation.ui.screen.TopicDetailScreen
import cr.ac.una.glifo.presentation.ui.screen.AdminHomeScreen
import cr.ac.una.glifo.presentation.ui.screen.HomeScreen
import cr.ac.una.glifo.presentation.ui.screen.TeacherHomeScreen
import cr.ac.una.glifo.presentation.ui.screen.*
import cr.ac.una.glifo.presentation.ui.screen.ProfileScreen
import cr.ac.una.glifo.presentation.ui.screen.SettingsScreen
import cr.ac.una.glifo.presentation.ui.screen.FlashcardScreen
import cr.ac.una.glifo.presentation.ui.screen.QuizResultScreen
import cr.ac.una.glifo.presentation.ui.screen.QuizScreen
import cr.ac.una.glifo.presentation.ui.screen.StudyScreen
import cr.ac.una.glifo.presentation.ui.screen.AiCostScreen
import cr.ac.una.glifo.presentation.ui.screen.SyncQueueScreen
import cr.ac.una.glifo.presentation.ui.screen.*
import cr.ac.una.glifo.presentation.viewmodel.ActiveCourseViewModel
import cr.ac.una.glifo.presentation.viewmodel.SessionViewModel

/**
 * The drawer (`GlifoDrawerContent`) hands back short internal ids ("notes", "syllabus",
 * "gaps"…), not real nav routes — some need a courseId, some don't share their string
 * with the actual route path at all. Translating them here in one place means every
 * screen's drawer just works, instead of each screen re-deriving the mapping (or, as
 * before, passing the raw id straight into `navController.navigate` and silently
 * failing to match any destination).
 */
private fun navigateFromDrawer(navController: NavHostController, route: String, activeCourseId: Long?) {
    val target = when (route) {
        "home" -> Screen.Home.route
        "home_teacher" -> Screen.HomeTeacher.route
        "home_admin" -> Screen.HomeAdmin.route
        "courses" -> Screen.Courses.route
        COURSE_SWITCHER_ROUTE -> Screen.CourseSwitcher.route
        "notes" -> activeCourseId?.let(Screen.NoteList::createRoute) ?: Screen.Courses.route
        "coverage" -> activeCourseId?.let(Screen.Coverage::createRoute) ?: Screen.Courses.route
        "study" -> activeCourseId?.let(Screen.StudyHub::createRoute) ?: Screen.Courses.route
        "sync" -> Screen.SyncQueue.route
        "ai_cost" -> Screen.AiCost.route
        "teacher_courses" -> Screen.TeacherCourses.route
        "syllabus" -> Screen.TeacherSyllabus.route
        "glossary" -> Screen.TeacherGlossary.route
        "gaps" -> Screen.TeacherGaps.route
        "admin_users" -> Screen.AdminUsers.route
        "admin_roles" -> Screen.AdminRoles.route
        "admin_courses" -> Screen.AdminCourses.route
        else -> route
    }
    navController.navigate(target) { launchSingleTop = true }
}

@Composable
fun GlifoNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    // Obtained once, Activity-scoped: SessionRepositoryImpl backing it is a Hilt
    // @Singleton, so every route below shares the same session state without
    // threading it through nav arguments.
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val activeCourseViewModel: ActiveCourseViewModel = hiltViewModel()
    val activeCourseId by activeCourseViewModel.activeCourseId.collectAsState()

    // Las rutas por curso usan el curso activo; si todavía no hay ninguno, se elige en Cursos.
    fun navigateToCourseRoute(route: (Long) -> String) {
        navController.navigate(activeCourseId?.let(route) ?: Screen.Courses.route)
    }

    // A single, consistent motion for every screen transition in the app instead of
    // an instant cut — kept subtle (short duration, small horizontal offset).
    val activeCourse by activeCourseViewModel.activeCourse.collectAsState()
    val activeCourseUi = ActiveCourseUi(
        name = activeCourse?.name,
        onChangeCourse = { navController.navigate(Screen.CourseSwitcher.route) { launchSingleTop = true } }
    )

    // El menú lateral de cualquier pantalla lee de aquí el curso activo y cómo cambiarlo.
    CompositionLocalProvider(LocalActiveCourse provides activeCourseUi) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 10 } },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 10 } }
    ) {

        // ── A: Acceso ────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onLoginSuccess = { role ->
                    val homeRoute = when (role) {
                        GlifoRole.Student -> Screen.Home.route
                        GlifoRole.Teacher -> Screen.HomeTeacher.route
                        GlifoRole.Admin -> Screen.HomeAdmin.route
                    }
                    navController.navigate(homeRoute) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onSendLink = { navController.popBackStack() }
            )
        }

        // ── B: Inicio ────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCourse = { courseId ->
                    navController.navigate(Screen.CourseDetail.createRoute(courseId))
                },
                onNavigateToCourses = { navController.navigate(Screen.Courses.route) },
                onNavigateToNotes = { navigateToCourseRoute { Screen.NoteList.createRoute(it) } },
                onNavigateToCoverage = { navigateToCourseRoute { Screen.Coverage.createRoute(it) } },
                onNavigateToStudy = { navigateToCourseRoute { Screen.StudyHub.createRoute(it) } },
                onNavigateToSync = { navController.navigate(Screen.SyncQueue.route) },
                onNavigateToAiCost = { navController.navigate(Screen.AiCost.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToCapture = { navigateToCourseRoute { Screen.Capture.createRoute(it) } },
                onNavigateToFlashcards = { navController.navigate(Screen.FlashcardSession.route) },
                onNavigateToQuiz = { navController.navigate(Screen.QuizSession.route) },
                onNavigateToDelta = { navController.navigate(Screen.CoverageDelta.route) },
                onNavigateToNoteDetail = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.HomeTeacher.route) {
            TeacherHomeScreen(
                onNavigateToCourses = { navController.navigate(Screen.TeacherCourses.route) },
                onNavigateToSyllabus = { navController.navigate(Screen.TeacherSyllabus.route) },
                onNavigateToGlossary = { navController.navigate(Screen.TeacherGlossary.route) },
                onNavigateToGaps = { navController.navigate(Screen.TeacherGaps.route) },
                onNavigateToAiCost = { navController.navigate(Screen.AiCost.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HomeTeacher.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.HomeAdmin.route) {
            AdminHomeScreen(
                onNavigateToUsers = { navController.navigate(Screen.AdminUsers.route) },
                onNavigateToCourses = { navController.navigate(Screen.AdminCourses.route) },
                onNavigateToRoles = { navController.navigate(Screen.AdminRoles.route) },
                onNavigateToAiCost = { navController.navigate(Screen.AiCost.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HomeAdmin.route) { inclusive = true }
                    }
                }
            )
        }

        // ── C: Cursos ────────────────────────────────────────────────────────
        composable(Screen.Courses.route) {
            CoursesScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToCourseDetail = { courseId -> navController.navigate(Screen.CourseDetail.createRoute(courseId)) },
                onNavigateToJoinCourse = { navController.navigate(Screen.JoinCourse.route) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, activeCourseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CourseSwitcher.route) {
            CourseSwitcherScreen(
                onNavigateBack = { navController.popBackStack() },
                onJoinCourse = { navController.navigate(Screen.JoinCourse.route) },
                onCreateCourse = { navController.navigate(Screen.TeacherNewCourse.route) }
            )
        }

        composable(Screen.JoinCourse.route) {
            JoinCourseScreen(
                onNavigateBack = { navController.popBackStack() },
                onJoinSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Courses.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.CourseDetail.route,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: 1L
            CourseDetailScreen(
                courseId = courseId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNotes = { navController.navigate(Screen.NoteList.createRoute(courseId)) },
                onNavigateToCoverage = { navController.navigate(Screen.Coverage.createRoute(courseId)) },
                onNavigateToStudy = { navController.navigate(Screen.StudyHub.createRoute(courseId)) },
                onNavigateToDelta = { navController.navigate(Screen.CoverageDelta.route) },
                onNavigateToCapture = { navController.navigate(Screen.Capture.createRoute(courseId)) },
                onNavigateToSync = { navController.navigate(Screen.SyncQueue.route) },
                onNavigateToNoteDetail = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) }
            )
        }

        // ── D: Captura ────────────────────────────────────────────────────────
        composable(
            route = Screen.Capture.route,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: 1L
            CaptureScreen(
                courseId = courseId,
                onNavigateBack = { navController.popBackStack() },
                onCapturePhoto = { photoPath ->
                    navController.navigate(Screen.CaptureReview.createRoute(photoPath, courseId))
                }
            )
        }

        composable(
            route = Screen.CaptureReview.route,
            arguments = captureArguments()
        ) { backStackEntry ->
            val photoPath = backStackEntry.arguments?.getString("photoPath")
            val courseId = backStackEntry.captureCourseId()
            CaptureReviewScreen(
                photoPath = photoPath,
                onNavigateBack = { navController.popBackStack() },
                onRetakePhoto = { navController.popBackStack() },
                onProcessAnyway = { navController.navigate(Screen.CaptureProcessing.createRoute(photoPath, courseId)) }
            )
        }

        composable(
            route = Screen.CaptureProcessing.route,
            arguments = captureArguments()
        ) { backStackEntry ->
            CaptureProcessingScreen(
                photoPath = backStackEntry.arguments?.getString("photoPath"),
                courseId = backStackEntry.captureCourseId(),
                onNavigateBack = { navController.popBackStack() },
                // La captura ya quedó guardada como apunte (#25): se abre ese apunte, no uno fijo.
                onViewResults = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId)) {
                        popUpTo(Screen.Capture.route) { inclusive = true }
                    }
                }
            )
        }

        // ── E: Apuntes ────────────────────────────────────────────────────────
        composable(
            route = Screen.NoteList.route,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            // Notas, cobertura y estudio siguen al curso activo: si se cambia de curso estando aquí,
            // la pantalla pasa al nuevo en vez de quedarse con el de la ruta.
            val courseId = activeCourseId ?: backStackEntry.arguments?.getLong("courseId") ?: return@composable
            NotesScreen(
                courseId = courseId,
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToNoteDetail = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) },
                onNavigateToCapture = { navController.navigate(Screen.Capture.createRoute(courseId)) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, courseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            NoteDetailScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCorrect = { fragment -> navController.navigate(Screen.CorrectFragment.createRoute(noteId, fragment)) },
                onNavigateToFormula = { fragment -> navController.navigate(Screen.FormulaViewer.createRoute(noteId, fragment)) },
                onNavigateToProvenance = { fragment -> navController.navigate(Screen.RegionProvenance.createRoute(noteId, fragment)) }
            )
        }

        composable(
            route = Screen.CorrectFragment.route,
            arguments = noteFragmentArguments()
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            CorrectFragmentScreen(
                noteId = noteId,
                fragmentId = backStackEntry.arguments?.getString("fragment"),
                onNavigateBack = { navController.popBackStack() },
                onSaveCorrection = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FormulaViewer.route,
            arguments = noteFragmentArguments()
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            FormulaViewerScreen(
                noteId = noteId,
                fragmentId = backStackEntry.arguments?.getString("fragment"),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCorrect = { fragment -> navController.navigate(Screen.CorrectFragment.createRoute(noteId, fragment)) }
            )
        }

        composable(
            route = Screen.RegionProvenance.route,
            arguments = noteFragmentArguments()
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            RegionProvenanceScreen(
                noteId = noteId,
                fragmentId = backStackEntry.arguments?.getString("fragment"),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── F: Cobertura ──────────────────────────────────────────────────────
        composable(
            route = Screen.Coverage.route,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            // Notas, cobertura y estudio siguen al curso activo: si se cambia de curso estando aquí,
            // la pantalla pasa al nuevo en vez de quedarse con el de la ruta.
            val courseId = activeCourseId ?: backStackEntry.arguments?.getLong("courseId") ?: return@composable
            CoverageScreen(
                courseId = courseId,
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToTopicDetail = { topicName -> navController.navigate(Screen.TopicDetail.createRoute(topicName)) },
                onNavigateToDelta = { navController.navigate(Screen.CoverageDelta.route) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, courseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.TopicDetail.route,
            arguments = listOf(navArgument("topicName") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicName = backStackEntry.arguments?.getString("topicName") ?: ""
            TopicDetailScreen(
                topicName = topicName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStudy = { navigateToCourseRoute { Screen.StudyHub.createRoute(it) } },
                onNavigateToNoteDetail = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) }
            )
        }

        composable(Screen.CoverageDelta.route) {
            CoverageDeltaScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNoteDetail = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) }
            )
        }

        // ── G: Estudio ────────────────────────────────────────────────────────
        composable(
            route = Screen.StudyHub.route,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            // Notas, cobertura y estudio siguen al curso activo: si se cambia de curso estando aquí,
            // la pantalla pasa al nuevo en vez de quedarse con el de la ruta.
            val courseId = activeCourseId ?: backStackEntry.arguments?.getLong("courseId") ?: return@composable
            StudyScreen(
                courseId = courseId,
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToFlashcards = { navController.navigate(Screen.FlashcardSession.route) },
                onNavigateToQuiz = { navController.navigate(Screen.QuizSession.route) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, courseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.FlashcardSession.route) {
            FlashcardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.QuizSession.route) {
            QuizScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResult = { navController.navigate(Screen.QuizResult.route) }
            )
        }

        composable(Screen.QuizResult.route) {
            QuizResultScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCoverage = { navigateToCourseRoute { Screen.Coverage.createRoute(it) } },
                onNavigateToGlossary = { navController.navigate(Screen.TeacherGlossary.route) }
            )
        }

        // ── H: Sync e IA ──────────────────────────────────────────────────────
        composable(Screen.SyncQueue.route) {
            SyncQueueScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToNoteDetail = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, activeCourseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AiCost.route) {
            val user by sessionViewModel.currentUser.collectAsState()
            AiCostScreen(
                role = user?.role ?: GlifoRole.Student,
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, activeCourseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // ── I: Docente ────────────────────────────────────────────────────────
        composable(Screen.TeacherCourses.route) {
            TeacherCoursesScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToNewCourse = { navController.navigate(Screen.TeacherNewCourse.route) },
                onNavigateToSyllabus = { navController.navigate(Screen.TeacherSyllabus.route) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, activeCourseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HomeTeacher.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.TeacherNewCourse.route) {
            TeacherNewCourseScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateSuccess = { navController.navigate(Screen.HomeTeacher.route) }
            )
        }

        composable(Screen.TeacherSyllabus.route) {
            TeacherSyllabusScreen(
                onNavigateBack = { navController.popBackStack() },
                onPublishSuccess = { navController.navigate(Screen.HomeTeacher.route) }
            )
        }

        composable(Screen.TeacherGlossary.route) {
            TeacherGlossaryScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, activeCourseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HomeTeacher.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.TeacherGaps.route) {
            TeacherGapsScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToTopicDetail = { topicName -> navController.navigate(Screen.TopicDetail.createRoute(topicName)) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, activeCourseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HomeTeacher.route) { inclusive = true }
                    }
                }
            )
        }

        // ── J: Admin ──────────────────────────────────────────────────────────
        composable(Screen.AdminCourses.route) {
            AdminCoursesScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToCourseDetail = { courseId -> navController.navigate(Screen.CourseDetail.createRoute(courseId)) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, activeCourseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HomeAdmin.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminUsers.route) {
            AdminUsersScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToUserDetail = { userName -> navController.navigate(Screen.AdminUserDetail.createRoute(userName)) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, activeCourseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HomeAdmin.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.AdminUserDetail.route,
            arguments = listOf(navArgument("userName") { type = NavType.StringType })
        ) { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName") ?: "David González"
            AdminUserDetailScreen(
                userName = userName,
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminRoles.route) {
            AdminRolesScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, activeCourseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HomeAdmin.route) { inclusive = true }
                    }
                }
            )
        }

        // ── K: Perfil y Ajustes ───────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSync = { navController.navigate(Screen.SyncQueue.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            val user by sessionViewModel.currentUser.collectAsState()
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChangePassword = { navController.navigate(Screen.ForgotPassword.route) },
                onNavigateToCourses = {
                    val coursesRoute = when (user?.role) {
                        GlifoRole.Teacher -> Screen.TeacherCourses.route
                        GlifoRole.Admin -> Screen.AdminCourses.route
                        else -> Screen.Courses.route
                    }
                    navController.navigate(coursesRoute)
                },
                onNavigateToAiCost = { navController.navigate(Screen.AiCost.route) },
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
    }
}

/** Argumentos compartidos por las rutas de revision y procesamiento de una captura. */
private fun captureArguments() = listOf(
    navArgument("photoPath") {
        type = NavType.StringType
        nullable = true
        defaultValue = null
    },
    // Navigation no admite Long nullable: -1 significa "captura sin curso".
    navArgument("courseId") {
        type = NavType.LongType
        defaultValue = NO_COURSE
    }
)

private const val NO_COURSE = -1L

private fun NavBackStackEntry.captureCourseId(): Long? =
    arguments?.getLong("courseId", NO_COURSE)?.takeIf { it != NO_COURSE }

/** noteId obligatorio + el fragmento opcional que se tocó en el detalle del apunte. */
private fun noteFragmentArguments() = listOf(
    navArgument("noteId") { type = NavType.LongType },
    navArgument("fragment") {
        type = NavType.StringType
        nullable = true
        defaultValue = null
    }
)
