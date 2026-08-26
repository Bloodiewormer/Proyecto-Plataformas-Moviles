package cr.ac.una.glifo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cr.ac.una.glifo.core.ui.component.GlifoRole
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

@Composable
fun GlifoNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // ── A: Acceso ────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
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
                onLoginStudentSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onLoginTeacherSuccess = {
                    navController.navigate(Screen.HomeTeacher.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onLoginAdminSuccess = {
                    navController.navigate(Screen.HomeAdmin.route) {
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
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HomeTeacher.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.HomeAdmin.route) {
            AdminHomeScreen(
                onNavigateToUsers = { navController.navigate(Screen.AdminUsers.route) },
                onNavigateToCourses = { navController.navigate(Screen.TeacherCourses.route) },
                onNavigateToRoles = { navController.navigate(Screen.AdminRoles.route) },
                onNavigateToAiCost = { navController.navigate(Screen.AiCost.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
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
                onNavigateDrawer = { route -> navController.navigate(route) },
                onLogout = {
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
                onCapturePhoto = { navController.navigate(Screen.CaptureReview.route) }
            )
        }

        composable(Screen.CaptureReview.route) {
            CaptureReviewScreen(
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
                onNavigateDrawer = { route -> navController.navigate(route) },
                onLogout = {
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
                onNavigateToCorrect = { navController.navigate(Screen.CorrectFragment.route) },
                onNavigateToFormula = { navController.navigate(Screen.FormulaViewer.route) },
                onNavigateToProvenance = { navController.navigate(Screen.RegionProvenance.route) }
            )
        }

        composable(Screen.CorrectFragment.route) {
            CorrectFragmentScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveCorrection = { navController.popBackStack() }
            )
        }

        composable(Screen.FormulaViewer.route) {
            FormulaViewerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCorrect = { navController.navigate(Screen.CorrectFragment.route) }
            )
        }

        composable(Screen.RegionProvenance.route) {
            RegionProvenanceScreen(
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
                onNavigateToTopicDetail = { navController.navigate(Screen.TopicDetail.route) },
                onNavigateToDelta = { navController.navigate(Screen.CoverageDelta.route) },
                onNavigateDrawer = { route -> navController.navigate(route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.TopicDetail.route) {
            TopicDetailScreen(
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
                onNavigateDrawer = { route -> navController.navigate(route) },
                onLogout = {
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
                onNavigateDrawer = { route -> navController.navigate(route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AiCost.route) {
            AiCostScreen(
                role = GlifoRole.Student,
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateDrawer = { route -> navController.navigate(route) },
                onLogout = {
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
                onNavigateDrawer = { route -> navController.navigate(route) },
                onLogout = {
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
                onNavigateDrawer = { route -> navController.navigate(route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HomeTeacher.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.TeacherGaps.route) {
            TeacherGapsScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToTopicDetail = { navController.navigate(Screen.TopicDetail.route) },
                onNavigateDrawer = { route -> navController.navigate(route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HomeTeacher.route) { inclusive = true }
                    }
                }
            )
        }

        // ── J: Admin ──────────────────────────────────────────────────────────
        composable(Screen.AdminUsers.route) {
            AdminUsersScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateToUserDetail = { userName -> navController.navigate(Screen.AdminUserDetail.route) },
                onNavigateDrawer = { route -> navController.navigate(route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.HomeAdmin.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminUserDetail.route) {
            AdminUserDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminRoles.route) {
            AdminRolesScreen(
                onNavigateBackOrMenu = { navController.popBackStack() },
                onNavigateDrawer = { route -> navController.navigate(route) },
                onLogout = {
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
                onNavigateToCourses = { navController.navigate(Screen.Courses.route) },
                onNavigateToAiCost = { navController.navigate(Screen.AiCost.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
