package com.example.self.feature_note


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.self.core.model.Todo
class TodoViewModel: ViewModel() {
    private val _todos= MutableStateFlow<List<Todo>>(emptyList())
    val todos:StateFlow<List<Todo>> =_todos

    fun addTodo(title:String){
        val newTodo=Todo(
            id=System.currentTimeMillis(),
            title=title
        )
        _todos.value += newTodo
    }

}