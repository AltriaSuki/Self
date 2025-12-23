package com.example.self.feature_todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.self.core.database.AppDatabase
import com.example.self.core.model.Priority
import com.example.self.core.model.Todo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                TodoViewModel(application)
            }
        }
    }
    private val todoDao = AppDatabase.getDatabase(application).todoDao()
    
    val todos: StateFlow<List<Todo>> = todoDao.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val activeTodos: StateFlow<List<Todo>> = todoDao.getActiveTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val completedTodos: StateFlow<List<Todo>> = todoDao.getCompletedTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val categories: StateFlow<List<String>> = todoDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _selectedFilter = MutableStateFlow(TodoFilter.ALL)
    val selectedFilter: StateFlow<TodoFilter> = _selectedFilter.asStateFlow()

    fun addTodo(
        title: String,
        description: String = "",
        priority: Priority = Priority.NORMAL,
        category: String = "",
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            val todo = Todo(
                title = title,
                description = description,
                priority = priority,
                category = category,
                dueDate = dueDate
            )
            todoDao.insertTodo(todo)
        }
    }

    fun updateTodo(todo: Todo) {
        viewModelScope.launch {
            todoDao.updateTodo(todo.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            todoDao.deleteTodo(todo)
        }
    }

    fun toggleTodoCompletion(todo: Todo) {
        viewModelScope.launch {
            todoDao.updateTodoCompletion(todo.id, !todo.completed)
        }
    }

    fun setFilter(filter: TodoFilter) {
        _selectedFilter.value = filter
    }
}

enum class TodoFilter {
    ALL,        // 全部
    ACTIVE,     // 进行中
    COMPLETED   // 已完成
}
