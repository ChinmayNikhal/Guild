package com.example.guild

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.guild.ui.navigation.AppNavHost
import com.example.guild.ui.theme.GuildTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GuildTheme {
                val auth = FirebaseAuth.getInstance()
                val isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

                AppNavHost(startDestination = if (isLoggedIn) "main" else "login")
            }
        }
    }
}
