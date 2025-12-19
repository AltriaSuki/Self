package com.example.self.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.self.feature_note.TodoScreen
@Composable
fun AppNavGraph(navController: NavHostController){
    NavHost(navController=navController,
        startDestination = "todo"){
        composable("todo"){
            TodoScreen()
        }
        composable("note"){}
    }



}
