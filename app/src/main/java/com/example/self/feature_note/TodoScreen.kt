package com.example.self.feature_note

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun TodoScreen(
    viewModel: TodoViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val todos by viewModel.todos.collectAsState()
    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {

        Row {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入 Todo") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (input.isNotBlank()) {
                    viewModel.addTodo(input)
                    input = ""
                }
            }) {
                Text("添加")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(todos) { todo ->
                Text(
                    text = "• ${todo.title}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}