package com.example.guild.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Settings Page", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            auth.signOut()
            Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
            navController.navigate("login") {
                popUpTo("main") { inclusive = true }
            }
        }) {
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}
