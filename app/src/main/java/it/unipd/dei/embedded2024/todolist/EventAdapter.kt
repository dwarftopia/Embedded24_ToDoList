package it.unipd.dei.embedded2024.todolist

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import java.text.SimpleDateFormat
import java.util.Locale


/**
 * EventAdapter è una classe derivata da ListAdapter che si occupa di fornire gli elementi da
 * visualizzare all'interno della RecyclerView e di popolarli con i dati forniti attraverso il metodo
 * submitList
 */
class EventAdapter :
	ListAdapter<ToDoEvent,EventAdapter.EventViewHolder>(EVENT_COMPARATOR){

		var callback: ViewHolderClickListener? = null

	/**
	 * ViewHolderClickListener è un'interfaccia con la quale è possibile notificare alla RecyclerView
	 * eventuali modifiche allo stato della CheckBox
	 */
	interface ViewHolderClickListener{
		fun onCheckedChange(position: Int)
	}

	/**
	 * EventViewHolder è una classe annidata che funge da wrapper per la view che contiene il singolo
	 * elemento della lista
	 */
	inner class EventViewHolder(itemView: View, callback: ViewHolderClickListener?) :ViewHolder(itemView), View.OnCreateContextMenuListener{

		/**
		 * Inizializzazione dei widget appartenenti ad un singolo elemento della lista
		 */
		init {
			val eventCompletedCheckBox: CheckBox = itemView.findViewById(R.id.event_completed)
			val cardView: CardView = itemView.findViewById(R.id.dialog_card_view)

			/* Mette la cardView in ascolto per un eventuale tocco prolungato */
			cardView.setOnCreateContextMenuListener(this)

			/* Mette la checkBox il ascolto per un eventuale click e quando esso si verifica
			* lo notifica alla recyclerView inviandone la posizione */
			eventCompletedCheckBox.setOnClickListener { buttonView ->
				val isChecked = (buttonView as CheckBox).isChecked
				eventCompletedCheckBox.isChecked = isChecked
				callback?.onCheckedChange(adapterPosition)
			}

		}

		/**
		 * Alla creazione del menu aggiunge ad esso un'opzione con la scritta 'Elimina'
		 */
		override fun onCreateContextMenu(
			menu: ContextMenu,
			v: View?,
			menuInfo: ContextMenu.ContextMenuInfo?
		) {
			menu.add(this.adapterPosition,1,0,R.string.delete)
		}

	}

	/**
	 * Restituisce l'oggetto di tipo ToDoEvent alla posizione position della lista
	 */
	fun getEvent(position: Int): ToDoEvent{
		return getItem(position)
	}


	/**
	 * Alla creazione dell'EventViewHolder gli vengono passati due parametri:
	 * view che rappresenta il singolo oggetto della lista
	 * callback che è un'istanza dell'interfaccia ViewHolderClickListener
	 */
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
		val view: View = LayoutInflater.from(parent.context)
			.inflate(R.layout.event_item, parent, false)
		return EventViewHolder(view,callback)
	}

	/**
	 * Descrive in che modo devono essere associati widget e dati
	 */
	override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
		val current = getItem(position)
		holder.itemView.findViewById<TextView>(R.id.event_title).text = current.description
		val date = holder.itemView.findViewById<TextView>(R.id.event_date)
		date.text = dateFormat.format(current.timestamp)
		holder.itemView.findViewById<TextView>(R.id.event_time).text = timeFormat.format(current.timestamp)
		holder.itemView.findViewById<CheckBox>(R.id.event_completed).isChecked = current.completed
	}

	companion object {

		/* timeFormat e dateFormat sono due oggetti che permettono di convertire un timestamp (Long)
		* nei formati indicati dal pattern (string) passato come primo parametro */
		val timeFormat : SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.ITALY)
		val dateFormat : SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)

		/* Oggetto necessario ad individuare le differenze una nuova versione della lista e quella precedente*/
		private val EVENT_COMPARATOR = object : DiffUtil.ItemCallback<ToDoEvent>() {
			override fun areItemsTheSame(oldItem: ToDoEvent, newItem: ToDoEvent): Boolean {
				return oldItem.id == newItem.id
			}

			override fun areContentsTheSame(oldItem: ToDoEvent, newItem: ToDoEvent): Boolean {
				return oldItem == newItem
			}
		}
	}

}