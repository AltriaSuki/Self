package com.example.self.feature_todo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.self.core.model.Priority
import com.example.self.core.model.Todo
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel = viewModel(
        factory = TodoViewModel.Factory
    )
) {
    val todos by viewModel.todos.collectAsState()
    val activeTodos by viewModel.activeTodos.collectAsState()
    val completedTodos by viewModel.completedTodos.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    
    val displayedTodos = when (selectedFilter) {
        TodoFilter.ALL -> todos
        TodoFilter.ACTIVE -> activeTodos
        TodoFilter.COMPLETED -> completedTodos
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待办事项") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加待办")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter tabs
            FilterTabs(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) },
                activeCount = activeTodos.size,
                completedCount = completedTodos.size
            )
            
            if (displayedTodos.isEmpty()) {
                EmptyTodoPlaceholder(selectedFilter)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedTodos, key = { it.id }) { todo ->
                        TodoItem(
                            todo = todo,
                            onToggleComplete = { viewModel.toggleTodoCompletion(todo) },
                            onEdit = { editingTodo = todo },
                            onDelete = { viewModel.deleteTodo(todo) }
                        )
                    }
                }
            }
        }
    }
    
    // Add Dialog
    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description, priority, category, dueDate ->
                viewModel.addTodo(title, description, priority, category, dueDate)
                showAddDialog = false
            }
        )
    }
    
    // Edit Dialog
    editingTodo?.let { todo ->
        EditTodoDialog(
            todo = todo,
            onDismiss = { editingTodo = null },
            onConfirm = { updatedTodo ->
                viewModel.updateTodo(updatedTodo)
                editingTodo = null
            }
        )
    }
}

@Composable
fun FilterTabs(
    selectedFilter: TodoFilter,
    onFilterSelected: (TodoFilter) -> Unit,
    activeCount: Int,
    completedCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == TodoFilter.ALL,
            onClick = { onFilterSelected(TodoFilter.ALL) },
            label = { Text("全部") },
            leadingIcon = if (selectedFilter == TodoFilter.ALL) {
                { Icon(Icons.Default.Done, contentDescription = null, Modifier.size(18.dp)) }
            } else null
        )
        FilterChip(
            selected = selectedFilter == TodoFilter.ACTIVE,
            onClick = { onFilterSelected(TodoFilter.ACTIVE) },
            label = { Text("进行中 ($activeCount)") },
            leadingIcon = if (selectedFilter == TodoFilter.ACTIVE) {
                { Icon(Icons.Default.Done, contentDescription = null, Modifier.size(18.dp)) }
            } else null
        )
        FilterChip(
            selected = selectedFilter == TodoFilter.COMPLETED,
            onClick = { onFilterSelected(TodoFilter.COMPLETED) },
            label = { Text("已完成 ($completedCount)") },
            leadingIcon = if (selectedFilter == TodoFilter.COMPLETED) {
                { Icon(Icons.Default.Done, contentDescription = null, Modifier.size(18.dp)) }
            } else null
        )
    }
}

@Composable
fun TodoItem(
    todo: Todo,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val priorityColor = when (todo.priority) {
        Priority.URGENT -> Color(0xFFE53935)
        Priority.HIGH -> Color(0xFFFF9800)
        Priority.NORMAL -> Color(0xFF4CAF50)
        Priority.LOW -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(priorityColor, RoundedCornerShape(2.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Checkbox
            Checkbox(
                checked = todo.completed,
                onCheckedChange = { onToggleComplete() }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
                    color = if (todo.completed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (todo.description.isNotEmpty()) {
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (todo.category.isNotEmpty()) {
                        AssistChip(
                            onClick = { },
                            label = { Text(todo.category, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    
                    todo.dueDate?.let { date ->
                        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                        Text(
                            text = "📅 ${dateFormat.format(Date(date))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (date < System.currentTimeMillis() && !todo.completed) 
                                Color(0xFFE53935) else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            
            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTodoPlaceholder(filter: TodoFilter) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = when (filter) {
                    TodoFilter.ALL -> Icons.Outlined.Assignment
                    TodoFilter.ACTIVE -> Icons.Outlined.Pending
                    TodoFilter.COMPLETED -> Icons.Outlined.TaskAlt
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (filter) {
                    TodoFilter.ALL -> "暂无待办事项"
                    TodoFilter.ACTIVE -> "没有进行中的任务"
                    TodoFilter.COMPLETED -> "还没有完成的任务"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Priority, String, Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var category by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加待办") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Priority selector
                Text("优先级", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { 
                                Text(
                                    when (p) {
                                        Priority.LOW -> "低"
                                        Priority.NORMAL -> "普通"
                                        Priority.HIGH -> "高"
                                        Priority.URGENT -> "紧急"
                                    }
                                ) 
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), description.trim(), priority, category.trim(), dueDate)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTodoDialog(
    todo: Todo,
    onDismiss: () -> Unit,
    onConfirm: (Todo) -> Unit
) {
    var title by remember { mutableStateOf(todo.title) }
    var description by remember { mutableStateOf(todo.description) }
    var priority by remember { mutableStateOf(todo.priority) }
    var category by remember { mutableStateOf(todo.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑待办") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("优先级", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { 
                                Text(
                                    when (p) {
                                        Priority.LOW -> "低"
                                        Priority.NORMAL -> "普通"
                                        Priority.HIGH -> "高"
                                        Priority.URGENT -> "紧急"
                                    }
                                ) 
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                        onConfirm(todo.copy(
                            title = title.trim(),
                            description = description.trim(),
                            priority = priority,
                            category = category.trim()
                        ))
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
