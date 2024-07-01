package it.unipd.dei.embedded2024.todolist

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

/**
 * Dichiara il DAO come una variabile privata nel costruttore.Non viene passato l'intero database
 * perchè è necessario solo l'accesso al DAO
 */
class EventRepository(private val eventDAO: ToDoEventDAO) {

	/* Room esegue le richieste al db in un thread separato.
	* Attraverso l'utilizzo di flow è possibile mettere in ascolto un observer che verrà avvisato
	* al cambiamento dei dati
	* */
	val allEvents: Flow<List<ToDoEvent>> = eventDAO.getAllLiveData()

	/* inserisce l'evento passato come parametro all'interno del db*/
	@WorkerThread
	fun insert (event: ToDoEvent){
		eventDAO.insert(event)
	}

	/* elimina l'elemento passato come parametro dal db*/
	@WorkerThread
	fun delete(event: ToDoEvent){
		eventDAO.delete(event)
	}

	/* modifica l'evento corrispondente a quello passato come primo parametro cambiando il valore del
	* campo completed con quello passato come secondo parametro*/
	@WorkerThread
	fun updateChecked(event: ToDoEvent, value:Boolean){
		eventDAO.setCompletedById(event.id, value)
	}

}