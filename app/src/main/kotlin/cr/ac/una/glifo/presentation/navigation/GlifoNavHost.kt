package cr.ac.una.glifo.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import cr.ac.una.glifo.presentation.viewmodel.SessionViewModel

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
    // Obtained once, Activity-scoped: SessionRepositoryImpl backing it is a Hilt
    // @Singleton, so every route below shares the same session state without
    // threading it through nav arguments.
    val sessionViewModel: SessionViewModel = hiltViewModel()

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
            val context = LocalContext.current
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromActivity(
                context as android.app.Activity,
                cr.ac.una.glifo.presentation.viewmodel.ViewModelFactoryEntryPoint::class.java
            )
            val factory = cr.ac.una.glifo.presentation.viewmodel.LoginViewModelFactory(
                entryPoint.authRepository(),
                entryPoint.sessionRepository()
            )
            val viewModel: cr.ac.una.glifo.presentation.viewmodel.LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

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
                },
                viewModel = viewModel
            )
        }

        composable(Screen.Register.route) {
            val context = LocalContext.current
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromActivity(
                context as android.app.Activity,
                cr.ac.una.glifo.presentation.viewmodel.ViewModelFactoryEntryPoint::class.java
            )
            val factory = cr.ac.una.glifo.presentation.viewmodel.RegisterViewModelFactory(
                entryPoint.authRepository(),
                entryPoint.sessionRepository()
            )
            val viewModel: cr.ac.una.glifo.presentation.viewmodel.RegisterViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                viewModel = viewModel
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
            val context = LocalContext.current
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromActivity(
                context as android.app.Activity,
                cr.ac.una.glifo.presentation.viewmodel.ViewModelFactoryEntryPoint::class.java
            )
            val factory = cr.ac.una.glifo.presentation.viewmodel.HomeViewModelFactory(
                entryPoint.courseRepository(),
                entryPoint.noteRepository(),
                entryPoint.syncRepository()
            )
            val viewModel: cr.ac.una.glifo.presentation.viewmodel.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

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
                    sessionViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }

        composable(Screen.HomeTeacher.route) {
            val context = LocalContext.current
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromActivity(
                context as android.app.Activity,
                cr.ac.una.glifo.presentation.viewmodel.ViewModelFactoryEntryPoint::class.java
            )
            val factory = cr.ac.una.glifo.presentation.viewmodel.TeacherHomeViewModelFactory(
                entryPoint.courseRepository(),
                entryPoint.teacherRepository()
            )
            val viewModel: cr.ac.una.glifo.presentation.viewmodel.TeacherHomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

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
                },
                viewModel = viewModel
            )
        }

        composable(Screen.HomeAdmin.route) {
            val context = LocalContext.current
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromActivity(
                context as android.app.Activity,
                cr.ac.una.glifo.presentation.viewmodel.ViewModelFactoryEntryPoint::class.java
            )
            val factory = cr.ac.una.glifo.presentation.viewmodel.AdminHomeViewModelFactory(
                entryPoint.adminRepository(),
                entryPoint.courseRepository(),
                entryPoint.syncRepository()
            )
            val viewModel: cr.ac.una.glifo.presentation.viewmodel.AdminHomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

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
                },
                viewModel = viewModel
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
                    sessionViewModel.logout()
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
                onProcessAnyway = { navController.navigate(Screen.CaptureProcessing.createRoute(photoPath)) }
            )
        }

        composable(
            route = Screen.CaptureProcessing.route,
            arguments = listOf(navArgument("photoPath") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            CaptureProcessingScreen(
                photoPath = backStackEntry.arguments?.getString("photoPath"),
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
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
                onNavigateDrawer = { route -> navigateFromDrawer(navController, route) },
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
