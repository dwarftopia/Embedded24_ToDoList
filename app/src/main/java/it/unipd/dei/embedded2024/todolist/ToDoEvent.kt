package it.unipd.dei.embedded2024.todolist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ToDoEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "completed", defaultValue = "0")
    val completed: Boolean
)
