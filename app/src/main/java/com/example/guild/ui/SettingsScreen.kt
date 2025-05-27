package com.example.guild.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var disappearingEnabled by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Settings Page", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(20.dp))

        // 🔘 Toggle for Disappearing Messages
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Disappearing Messages", modifier = Modifier.weight(1f))
            Switch(
                checked = disappearingEnabled,
                onCheckedChange = { disappearingEnabled = it }
            )
        }

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
