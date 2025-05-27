package com.example.guild.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.guild.auth.AuthViewModel
import com.example.guild.chatResources.ChatScreen
import com.example.guild.chatResources.ChatViewModel
import com.example.guild.groupResources.GroupViewModel
import com.example.guild.ui.*

@Composable
fun AppNavHost(startDestination: String) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(navController, authViewModel)
        }
        composable("signup") {
            SignupScreen(navController, authViewModel, onSignupSuccess = {
                navController.navigate("login") {
                    popUpTo("signup") { inclusive = true }
                }
            })
        }
        composable("main") {
            MainScreen(
                navController = navController,
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                onOpenChat = { chatId, otherUserId, otherUsername ->
                    navController.navigate("chat/$chatId/$otherUserId/$otherUsername")
                }
            )
        }

        composable("profile") {
            ProfileScreen(navController, authViewModel)
        }
        composable("friends") {
            FriendsScreen(authViewModel)
        }
        composable("settings") {
            SettingsScreen(navController)
        }

        composable(
            route = "chat/{chatId}/{otherUserId}/{otherUsername}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("otherUsername") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: return@composable
            val otherUsername = backStackEntry.arguments?.getString("otherUsername") ?: "Unknown"

            ChatScreen(
                chatId = chatId,
                otherUserId = otherUserId,
                username = otherUsername,
                chatViewModel = chatViewModel
            )
        }

    }
}

