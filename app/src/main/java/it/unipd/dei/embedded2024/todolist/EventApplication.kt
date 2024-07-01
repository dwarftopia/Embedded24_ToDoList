package it.unipd.dei.embedded2024.todolist

import android.app.Application

/**
 * EventApplication deriva da Application quindi viene istanziata prima di qualsiasi altra classe
 * alla creazione del processo
 */
class EventApplication: Application() {

	/* usando lazy, database e repository vengono create solo quando necessario*/
	private val database by lazy { ToDoListDatabase.getDatabase(this) }
	val repository by lazy { EventRepository(database.toDoEventDAO()) }
}