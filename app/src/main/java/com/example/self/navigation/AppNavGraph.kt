package com.example.self.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.self.feature_music.MusicScreen
import com.example.self.feature_note.NoteDetailScreen
import com.example.self.feature_note.NoteScreen
import com.example.self.feature_pomodoro.PomodoroScreen
import com.example.self.feature_todo.TodoScreen

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Todo : Screen("todo", "待办", Icons.Filled.CheckBox, Icons.Outlined.CheckBox)
    data object Note : Screen("note", "笔记", Icons.Filled.Note, Icons.Outlined.Note)
    data object Pomodoro : Screen("pomodoro", "番茄钟", Icons.Filled.Timer, Icons.Outlined.Timer)
    data object Music : Screen("music", "音乐", Icons.Filled.MusicNote, Icons.Outlined.MusicNote)
}

val bottomNavItems = listOf(
    Screen.Todo,
    Screen.Note,
    Screen.Pomodoro,
    Screen.Music
)

@Composable
fun AppNavGraph(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Determine if we should show bottom bar (hide on detail screens)
    val currentRoute = currentDestination?.route ?: ""
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Todo.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Todo
            composable(Screen.Todo.route) {
                TodoScreen()
            }
            
            // Note list
            composable(Screen.Note.route) {
                NoteScreen(
                    onNoteClick = { noteId ->
                        navController.navigate("note_detail/$noteId")
                    },
                    onAddNote = {
                        navController.navigate("note_detail/new")
                    }
                )
            }
            
            // Note detail (edit/create)
            composable(
                route = "note_detail/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val noteIdArg = backStackEntry.arguments?.getString("noteId")
                val noteId = if (noteIdArg == "new") null else noteIdArg?.toLongOrNull()
                
                NoteDetailScreen(
                    noteId = noteId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Pomodoro
            composable(Screen.Pomodoro.route) {
                PomodoroScreen()
            }
            
            // Music
            composable(Screen.Music.route) {
                MusicScreen()
            }
        }
    }
}

