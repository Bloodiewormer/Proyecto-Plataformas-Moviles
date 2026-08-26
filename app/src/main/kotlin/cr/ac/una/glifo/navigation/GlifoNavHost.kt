package cr.ac.una.glifo.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cr.ac.una.glifo.core.data.session.SessionManager
import cr.ac.una.glifo.core.ui.component.GlifoRole
import cr.ac.una.glifo.feature.admin.presentation.AdminCoursesScreen
import cr.ac.una.glifo.feature.admin.presentation.AdminRolesScreen
import cr.ac.una.glifo.feature.admin.presentation.AdminUserDetailScreen
import cr.ac.una.glifo.feature.admin.presentation.AdminUsersScreen
import cr.ac.una.glifo.feature.auth.presentation.ForgotPasswordScreen
import cr.ac.una.glifo.feature.auth.presentation.LoginScreen
import cr.ac.una.glifo.feature.auth.presentation.RegisterScreen
import cr.ac.una.glifo.feature.auth.presentation.SplashScreen
import cr.ac.una.glifo.feature.capture.presentation.CaptureProcessingScreen
import cr.ac.una.glifo.feature.capture.presentation.CaptureReviewScreen
import cr.ac.una.glifo.feature.capture.presentation.CaptureScreen
import cr.ac.una.glifo.feature.course.presentation.CourseDetailScreen
import cr.ac.una.glifo.feature.course.presentation.CoursesScreen
import cr.ac.una.glifo.feature.course.presentation.JoinCourseScreen
import cr.ac.una.glifo.feature.coverage.presentation.CoverageDeltaScreen
import cr.ac.una.glifo.feature.coverage.presentation.CoverageScreen
import cr.ac.una.glifo.feature.coverage.presentation.TopicDetailScreen
import cr.ac.una.glifo.feature.home.presentation.AdminHomeScreen
import cr.ac.una.glifo.feature.home.presentation.HomeScreen
import cr.ac.una.glifo.feature.home.presentation.TeacherHomeScreen
import cr.ac.una.glifo.feature.note.presentation.*
import cr.ac.una.glifo.feature.profile.presentation.ProfileScreen
import cr.ac.una.glifo.feature.settings.presentation.SettingsScreen
import cr.ac.una.glifo.feature.study.presentation.FlashcardScreen
import cr.ac.una.glifo.feature.study.presentation.QuizResultScreen
import cr.ac.una.glifo.feature.study.presentation.QuizScreen
import cr.ac.una.glifo.feature.study.presentation.StudyScreen
import cr.ac.una.glifo.feature.sync.presentation.AiCostScreen
import cr.ac.una.glifo.feature.sync.presentation.SyncQueueScreen
import cr.ac.una.glifo.feature.teacher.presentation.*

/**
 * The drawer (`GlifoDrawerContent`) hands back short internal ids ("notes", "syllabus",
 * "gaps"…), not real nav routes — some need a courseId, some don't share their string
 * with the actual route path at all. Translating them here in one place means every
 * screen's drawer just works, instead of each screen re-deriving the mapping (or, as
 * before, passing the raw id straight into `navController.navigate` and silently
 * failing to match any destination).
 */
private fun navigateFromDrawer(navController: NavHostController, route: String, activeCourseId: Long = 1L) {
    val target = when (route) {
        "home" -> Screen.Home.route
        "home_teacher" -> Screen.HomeTeacher.route
        "home_admin" -> Screen.HomeAdmin.route
        "courses" -> Screen.Courses.route
        "notes" -> Screen.NoteList.createRoute(activeCourseId)
        "coverage" -> Screen.Coverage.createRoute(activeCourseId)
        "study" -> Screen.StudyHub.createRoute(activeCourseId)
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
    // A single, consistent motion for every screen transition in the app instead of
    // an instant cut — kept subtle (short duration, small horizontal offset).
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
                onNavigateToNotes = { navController.navigate(Screen.NoteList.createRoute(1L)) },
                onNavigateToCoverage = { navController.navigate(Screen.Coverage.createRoute(1L)) },
                onNavigateToStudy = { navController.navigate(Screen.StudyHub.createRoute(1L)) },
                onNavigateToSync = { navController.navigate(Screen.SyncQueue.route) },
                onNavigateToAiCost = { navController.navigate(Screen.AiCost.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToCapture = { navController.navigate(Screen.Capture.createRoute(1L)) },
                onNavigateToFlashcards = { navController.navigate(Screen.FlashcardSession.route) },
                onNavigateToQuiz = { navController.navigate(Screen.QuizSession.route) },
                onNavigateToDelta = { navController.navigate(Screen.CoverageDelta.route) },
                onNavigateToNoteDetail = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) },
                onLogout = {
                    SessionManager.logout()
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
                    SessionManager.logout()
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
                    SessionManager.logout()
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    SessionManager.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
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
                    navController.navigate(Screen.CaptureReview.createRoute(photoPath))
                }
            )
        }

        composable(
            route = Screen.CaptureReview.route,
            arguments = listOf(navArgument("photoPath") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val photoPath = backStackEntry.arguments?.getString("photoPath")
            CaptureReviewScreen(
                photoPath = photoPath,
                onNavigateBack = { navController.popBackStack() },
                onRetakePhoto = { navController.popBackStack() },
                onProcessAnyway = { navController.navigate(Screen.CaptureProcessing.route) }
            )
        }

        composable(Screen.CaptureProcessing.route) {
            CaptureProcessingScreen(
                onNavigateBack = { navController.popBackStack() },
                onViewResults = {
                    navController.navigate(Screen.NoteDetail.createRoute(1L)) {
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
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: 1L
            NotesScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToNoteDetail = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) },
                onNavigateToCapture = { navController.navigate(Screen.Capture.createRoute(courseId)) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, courseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    SessionManager.logout()
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
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 1L
            NoteDetailScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCorrect = { navController.navigate(Screen.CorrectFragment.createRoute(noteId)) },
                onNavigateToFormula = { navController.navigate(Screen.FormulaViewer.createRoute(noteId)) },
                onNavigateToProvenance = { navController.navigate(Screen.RegionProvenance.createRoute(noteId)) }
            )
        }

        composable(
            route = Screen.CorrectFragment.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 1L
            CorrectFragmentScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() },
                onSaveCorrection = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FormulaViewer.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 1L
            FormulaViewerScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCorrect = { navController.navigate(Screen.CorrectFragment.createRoute(noteId)) }
            )
        }

        composable(
            route = Screen.RegionProvenance.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 1L
            RegionProvenanceScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── F: Cobertura ──────────────────────────────────────────────────────
        composable(
            route = Screen.Coverage.route,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: 1L
            CoverageScreen(
                courseId = courseId,
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToTopicDetail = { topicName -> navController.navigate(Screen.TopicDetail.createRoute(topicName)) },
                onNavigateToDelta = { navController.navigate(Screen.CoverageDelta.route) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, courseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    SessionManager.logout()
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
                onNavigateToStudy = { navController.navigate(Screen.StudyHub.createRoute(1L)) },
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
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: 1L
            StudyScreen(
                courseId = courseId,
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToFlashcards = { navController.navigate(Screen.FlashcardSession.route) },
                onNavigateToQuiz = { navController.navigate(Screen.QuizSession.route) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route, courseId) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    SessionManager.logout()
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
                onNavigateToCoverage = { navController.navigate(Screen.Coverage.createRoute(1L)) },
                onNavigateToGlossary = { navController.navigate(Screen.TeacherGlossary.route) }
            )
        }

        // ── H: Sync e IA ──────────────────────────────────────────────────────
        composable(Screen.SyncQueue.route) {
            SyncQueueScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToNoteDetail = { noteId -> navController.navigate(Screen.NoteDetail.createRoute(noteId)) },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    SessionManager.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AiCost.route) {
            AiCostScreen(
                role = SessionManager.currentUser?.role ?: GlifoRole.Student,
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    SessionManager.logout()
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    SessionManager.logout()
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    SessionManager.logout()
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    SessionManager.logout()
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    SessionManager.logout()
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    SessionManager.logout()
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    SessionManager.logout()
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
                    SessionManager.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChangePassword = { navController.navigate(Screen.ForgotPassword.route) },
                onNavigateToCourses = {
                    val coursesRoute = when (SessionManager.currentUser?.role) {
                        GlifoRole.Teacher -> Screen.TeacherCourses.route
                        GlifoRole.Admin -> Screen.AdminCourses.route
                        else -> Screen.Courses.route
                    }
                    navController.navigate(coursesRoute)
                },
                onNavigateToAiCost = { navController.navigate(Screen.AiCost.route) },
                onLogout = {
                    SessionManager.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
