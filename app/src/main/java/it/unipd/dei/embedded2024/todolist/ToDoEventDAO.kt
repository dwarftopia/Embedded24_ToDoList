package it.unipd.dei.embedded2024.todolist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ToDoEventDAO {
    @Query("SELECT * FROM todoevent")
    fun getAll() : List<ToDoEvent>
    @Query("SELECT * FROM todoevent")
    fun getAllLiveData() : Flow<List<ToDoEvent>>
    @Query("SELECT * from todoevent WHERE timestamp BETWEEN (:date) AND (:date + 24*60*60*1000 - 1)")
    fun getByDate(date: Long) : List<ToDoEvent>
    @Query("SELECT * from todoevent WHERE timestamp BETWEEN (:startDate) AND (:endDate)")
    fun getByDateRange(startDate: Long, endDate: Long) : List<ToDoEvent>
    @Query("SELECT COUNT(*) FROM todoevent")
    fun getCount() : Int
    @Query("UPDATE todoevent SET completed = (:completed) WHERE id = (:id)")
    fun setCompletedById(id: Int, completed: Boolean)
    @Insert
    fun insert(vararg toDoEvent: ToDoEvent)
    @Delete
    fun delete(toDoEvent: ToDoEvent)

}