package com.example.guild.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavController

@Composable
fun ProfileDropdownMenu(
    navController: NavController,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { onDismiss() }
    ) {
        DropdownMenuItem(text = { Text("Profile") }, onClick = {
            navController.navigate("profile")
            onDismiss()
        })
        DropdownMenuItem(text = { Text("Friends") }, onClick = {
            navController.navigate("friends")
            onDismiss()
        })
        DropdownMenuItem(text = { Text("Settings") }, onClick = {
            navController.navigate("settings")
            onDismiss()
        })

    }
}

//package com.example.guild.ui.components
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.navigation.NavController
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.AccountCircle
//
//@Composable
//fun ProfileDropdownMenu(navController: NavController) {
//    var expanded by remember { mutableStateOf(false) }
//
//    Box(modifier = Modifier.wrapContentSize()) {
//        IconButton(onClick = { expanded = true }) {
//            Icon(Icons.Filled.AccountCircle, contentDescription = "Profile Menu")
//        }
//
//        DropdownMenu(
//            expanded = expanded,
//            onDismissRequest = { expanded = false }
//        ) {
//            DropdownMenuItem(text = { Text("Profile") }, onClick = {
//                expanded = false
//                navController.navigate("profile")
//            })
//            DropdownMenuItem(text = { Text("Friends") }, onClick = {
//                expanded = false
//                navController.navigate("friends")
//            })
//            DropdownMenuItem(text = { Text("Settings hehe") }, onClick = {
//                expanded = false
//                navController.navigate("settings")
//            })
//        }
//    }
//}
