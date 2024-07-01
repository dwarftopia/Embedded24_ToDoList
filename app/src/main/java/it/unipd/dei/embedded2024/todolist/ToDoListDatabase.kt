package it.unipd.dei.embedded2024.todolist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ToDoEvent::class], version = 3, exportSchema = false)
abstract class ToDoListDatabase : RoomDatabase() {
    abstract fun toDoEventDAO() : ToDoEventDAO

    companion object {
        private var INSTANCE: ToDoListDatabase? = null
        fun getDatabase(context: Context): ToDoListDatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = Room.databaseBuilder(
                        context,
                        ToDoListDatabase::class.java,
                        "todolist.db")
                        .build()
                }
            }
            return INSTANCE!!
        }
    }
}