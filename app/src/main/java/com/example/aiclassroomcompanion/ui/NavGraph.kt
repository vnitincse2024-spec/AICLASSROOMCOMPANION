package com.example.aiclassroomcompanion.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.aiclassroomcompanion.ui.screens.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Lectures : Screen("lectures")
    object Notes : Screen("notes/{transcription}") {
        fun createRoute(transcription: String) = "notes/$transcription"
    }
    object Summary : Screen("summary/{transcription}") {
        fun createRoute(transcription: String) = "summary/$transcription"
    }
    object Profile : Screen("profile")
    object Recording : Screen("recording")
    object Flashcards : Screen("flashcards/{transcription}") {
        fun createRoute(transcription: String) = "flashcards/$transcription"
    }
    object Quiz : Screen("quiz/{transcription}") {
        fun createRoute(transcription: String) = "quiz/$transcription"
    }
    object Chat : Screen("chat")
}

@Composable
fun AppNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Lectures.route) {
            LecturesScreen(navController)
        }
        composable(
            route = Screen.Notes.route,
            arguments = listOf(navArgument("transcription") { type = NavType.StringType })
        ) { backStackEntry ->
            val transcription = backStackEntry.arguments?.getString("transcription") ?: ""
            NotesScreen(navController, transcription = transcription)
        }
        composable(
            route = Screen.Summary.route,
            arguments = listOf(navArgument("transcription") { type = NavType.StringType })
        ) { backStackEntry ->
            val transcription = backStackEntry.arguments?.getString("transcription") ?: ""
            SummaryScreen(navController, transcription = transcription)
        }
        composable(
            route = Screen.Flashcards.route,
            arguments = listOf(navArgument("transcription") { type = NavType.StringType })
        ) { backStackEntry ->
            val transcription = backStackEntry.arguments?.getString("transcription") ?: ""
            FlashcardsScreen(navController, transcription = transcription)
        }
        composable(
            route = Screen.Quiz.route,
            arguments = listOf(navArgument("transcription") { type = NavType.StringType })
        ) { backStackEntry ->
            val transcription = backStackEntry.arguments?.getString("transcription") ?: ""
            QuizScreen(navController, transcription = transcription)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
        composable(Screen.Recording.route) {
            RecordingScreen(navController)
        }
        composable(Screen.Chat.route) {
            ChatScreen(navController)
        }
    }
}
