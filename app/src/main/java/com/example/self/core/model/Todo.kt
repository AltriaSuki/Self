package com.example.self.core.model

data class Todo(
    val id: Long,
    val title: String,
    val completed: Boolean=false
)