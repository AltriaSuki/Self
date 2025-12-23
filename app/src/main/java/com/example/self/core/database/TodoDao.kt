package com.example.self.core.database

import androidx.room.*
import com.example.self.core.model.Todo
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY priority DESC, dueDate ASC, createdAt DESC")
    fun getAllTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE completed = 0 ORDER BY priority DESC, dueDate ASC")
    fun getActiveTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE completed = 1 ORDER BY updatedAt DESC")
    fun getCompletedTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE category = :category ORDER BY priority DESC")
    fun getTodosByCategory(category: String): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: Long): Todo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: Todo): Long

    @Update
    suspend fun updateTodo(todo: Todo)

    @Delete
    suspend fun deleteTodo(todo: Todo)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: Long)

    @Query("UPDATE todos SET completed = :completed, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTodoCompletion(id: Long, completed: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT DISTINCT category FROM todos WHERE category != '' ORDER BY category")
    fun getAllCategories(): Flow<List<String>>
}
