package com.example.guild.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guild.auth.AuthViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.navigation.NavController

@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current

    var about by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Available") }
    var mobile by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Other") }
    var dob by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(Color(0xFFBB86FC)) }

    LaunchedEffect(authViewModel.currentUserId) {
        authViewModel.loadProfile(
            onLoaded = {
                about = it["about"] as? String ?: ""
                status = it["status"] as? String ?: "Available"
                mobile = it["mobile"] as? String ?: ""
                gender = it["gender"] as? String ?: "Other"
                dob = it["dob"] as? String ?: ""
                username = it["username"] as? String ?: ""
            },
            onError = {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(color = color, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = username.take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = username, style = MaterialTheme.typography.titleLarge, color = Color.White)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = about,
            onValueChange = { about = it },
            label = { Text("About", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        DropdownField(
            label = "Status",
            options = listOf("Available", "Busy", "Offline", "AFK"),
            selected = status,
            onSelected = { status = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = it },
            label = { Text("Mobile Number", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        DropdownField(
            label = "Gender",
            options = listOf("Male", "Female", "Other"),
            selected = gender,
            onSelected = { gender = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            label = { Text("Date of Birth (DD/MM/YYYY)", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                authViewModel.saveProfile(
                    about, status, mobile, gender, dob,
                    onSuccess = {
                        Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
        ) {
            Text("Save", color = Color.White)
        }
    }
}

@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text(label, color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray)
                }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF2C2C2C))
        ) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it, color = Color.White) },
                    onClick = {
                        onSelected(it)
                        expanded = false
                    }
                )
            }
        }
    }
}
