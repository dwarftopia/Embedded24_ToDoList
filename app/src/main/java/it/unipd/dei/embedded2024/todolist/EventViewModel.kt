package it.unipd.dei.embedded2024.todolist

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * EventViewModel permette l'interazione tra le activity e la repository del database
 */
class EventViewModel(private val repository: EventRepository): ViewModel(){

	/*
	* allEvents attraverso l'uso di LiveData riceve gli aggiornamenti dei dati contenuti nel database,
	* questo permette di aggiornare la UI solo quando avvengono delle modifiche
	* */
	val allEvents: LiveData<List<ToDoEvent>> = repository.allEvents.asLiveData()


	/*
	* Lancia una coroutine in modo non-bloccante per inserire i dati nel database
	* */
	fun insert(event: ToDoEvent) = viewModelScope.launch {
		withContext(Dispatchers.IO) {
			repository.insert(event)
		}
	}

	/*
	* Lancia una coroutine in modo non-bloccante per rimuovere i dati dal database
	* */
	fun delete (event: ToDoEvent) = viewModelScope.launch {
		withContext(Dispatchers.IO) {
			repository.delete(event)
		}
	}

	/*
	* Lancia una coroutine in modo non-bloccante per aggiornare il valore di un elemento nel database
	* */
	fun updateChecked(event: ToDoEvent, value: Boolean) = viewModelScope.launch {
		withContext(Dispatchers.IO){
			repository.updateChecked(event,value)
		}
	}
}

class EventViewModelFactory(private val repository: EventRepository):ViewModelProvider.Factory{
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if(modelClass.isAssignableFrom(EventViewModel::class.java)){
			@Suppress("UNCHECKED_CAST")
			return EventViewModel(repository) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}