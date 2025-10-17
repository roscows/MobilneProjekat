package com.example.ironlink.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Profile") }, // <-- IZMENA
            selected = currentRoute == "main",
            onClick = { navController.navigate("main") { popUpTo(0) } },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.LocationOn, contentDescription = "Map") },
            label = { Text("Map") },
            selected = currentRoute == "map",
            onClick = { navController.navigate("map") { popUpTo(0) } },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.List, contentDescription = "Activities") },
            label = { Text("Activities") }, // <-- IZMENA
            selected = currentRoute == "activities", // <-- IZMENA
            onClick = { navController.navigate("activities") { popUpTo(0) } }, // <-- IZMENA
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Star, contentDescription = "Leaderboard") },
            label = { Text("Leaderboard") },
            selected = currentRoute == "leaderboard",
            onClick = { navController.navigate("leaderboard") { popUpTo(0) } },
        )
    }
}

@Composable
fun getCurrentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}