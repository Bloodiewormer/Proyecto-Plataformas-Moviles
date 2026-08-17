package cr.ac.una.glifo.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay

@Composable
fun GlifoNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(navController = navController, startDestination = startDestination) {
        
        composable(Screen.Splash.route) {
            LaunchedEffect(Unit) {
                delay(2000)
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            PlaceholderScreen("Splash")
        }
        
        composable(Screen.Login.route) {
            PlaceholderScreen("Login", onAction = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        
        composable(Screen.Register.route) {
            PlaceholderScreen("Register", onAction = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Register.route) { inclusive = true }
                }
            })
        }
        
        composable(Screen.Home.route) {
            PlaceholderScreen("Home")
        }
        
        composable(
            route = Screen.CourseDetail.route,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: 0L
            PlaceholderScreen("CourseDetail ($courseId)", onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.NoteList.route,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: 0L
            PlaceholderScreen("NoteList ($courseId)", onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            PlaceholderScreen("NoteDetail ($noteId)", onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Capture.route,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: 0L
            PlaceholderScreen("Capture ($courseId)", onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.StudyHub.route,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: 0L
            PlaceholderScreen("StudyHub ($courseId)", onBack = { navController.popBackStack() })
        }

        composable(Screen.FlashcardSession.route) {
            PlaceholderScreen("FlashcardSession", onBack = { navController.popBackStack() })
        }

        composable(Screen.QuizSession.route) {
            PlaceholderScreen("QuizSession", onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Coverage.route,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: 0L
            PlaceholderScreen("Coverage ($courseId)", onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            PlaceholderScreen("Settings", onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            PlaceholderScreen("Profile", onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun PlaceholderScreen(name: String, onBack: (() -> Unit)? = null, onAction: (() -> Unit)? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = name)
            if (onAction != null) {
                Button(onClick = onAction) {
                    Text("Proceed")
                }
            }
            if (onBack != null) {
                Button(onClick = onBack) {
                    Text("Go Back")
                }
            }
        }
    }
}
