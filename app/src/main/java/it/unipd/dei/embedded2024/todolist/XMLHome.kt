package it.unipd.dei.embedded2024.todolist

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


/**
 * XMLHome è la classe che rappresenta l'Activity per la parte di app gestita attraverso un approccio
 * misto tra dichiarativo e programmatico usando file XML
 */
class XMLHome : AppCompatActivity() {

    private val eventViewModel: EventViewModel by viewModels {
        EventViewModelFactory((application as EventApplication).repository)
    }

    private var allEvents = ArrayList<ToDoEvent>()
    private val selectedDayEventList = ArrayList<ToDoEvent>()
    private lateinit var currentDateRecyclerView: RecyclerView
    private lateinit var currentDateAdapter: EventAdapter
    private lateinit var calendar: Calendar
    private lateinit var calendarView: CalendarView
    private lateinit var emptyListTextView: TextView
    private lateinit var floatingActionButton : FloatingActionButton
    private lateinit var backButton : ImageButton
    private lateinit var upcomingEventBtn : ImageButton
    private lateinit var calendarButton : ImageButton
    private lateinit var eventTextView : TextView
    private lateinit var toggleGroup : MaterialButtonToggleGroup
    private lateinit var dayButton : MaterialButton
    private lateinit var weekButton: MaterialButton
    private lateinit var monthButton: MaterialButton
    private lateinit var dialog : AlertDialog
    private var dismiss : Boolean = false


    /* Variabili di stato */

    /*  true quando il calendario è visibile
     *  false altrimenti
     */
    private var isCalendar : Boolean = true

    /*  offsetMillisWeek quando viene premuto il pulsante weekButton,
     *  offsetMillisMonth quando viene premuto il pulsante monthButton
     *  offestMillisDay altrimenti
     */
    private var offset : Long = 0

    /* true quando è aperta la finestra di dialogo relativa all'aggiunta degli eventi
    *  false altrimenti
    */
    private var isAdd : Boolean = false

    /* salva l'ora inserita nel timePicker */
    private var hourAdd : Int = Calendar.HOUR_OF_DAY

    /* salva i minuti inseriti nel timePicker */
    private var minuteAdd : Int = Calendar.MINUTE

    /* salva il nome dell'evento inserito nella finestra di dialogo */
    private var newEventAdd : String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xmlhome)

        floatingActionButton = findViewById(R.id.floating_button)
        backButton = findViewById(R.id.back)
        upcomingEventBtn = findViewById(R.id.upcoming_events_btn)
        calendarButton = findViewById(R.id.calendar_events_btn)
        eventTextView = findViewById(R.id.text_event)
        calendarView = findViewById(R.id.calendar_view)
        toggleGroup = findViewById(R.id.toggle_group)
        dayButton = findViewById(R.id.day_button)
        weekButton = findViewById(R.id.week_button)
        monthButton = findViewById(R.id.month_button)
        emptyListTextView = findViewById(R.id.empty_list)

        /* assegna a calendar la data attuale */
        calendar = Calendar.getInstance()
        /* imposta il fuso orario a calendar */
        calendar.timeZone = TimeZone.getTimeZone("Europe/Rome")

        /* inizializzazione delle variabili di stato,
        *  se savedInstanceState è null vengono assegnati dei valori di default
        * */
        if (savedInstanceState != null){

            calendar.timeInMillis = savedInstanceState.getLong("current_date")
            calendarView.date = calendar.timeInMillis
            isCalendar = savedInstanceState.getBoolean("is_calendar")
            offset = savedInstanceState.getLong("selected_button")
            isAdd = savedInstanceState.getBoolean("is_add")
            newEventAdd = savedInstanceState.getString("new_event_add").toString()
            hourAdd = savedInstanceState.getInt("hour_add")
            minuteAdd = savedInstanceState.getInt("minute_add")

            setCurrentDateRecyclerView()
        }
        else{
            setCurrentDateRecyclerView()
            calendarView.date = calendar.timeInMillis
            isCalendar = true
            offset = offsetMillisDays
            isAdd = false
            newEventAdd = ""
            hourAdd = calendar.get(Calendar.HOUR_OF_DAY)
            minuteAdd = calendar.get(Calendar.MINUTE)
            setCurrentDateRecyclerView()
        }

        /* Ripristina la lista degli eventi quando il calendario non è visibile*/
        if(!isCalendar) {
            upcomingViewVisible()
            changeListRecycleView(offset)
        }

        /* Ripristina la finestra di dialogo per l'aggiunta dell'evento */
        if(isAdd) {
            startAddDialog()
        }

        /* Assegna un observer alla lista di LiveData contenuta in eventViewModel
        * e aggiorna di conseguenza la lista creata in locale ordinandola per data
        * */
        eventViewModel.allEvents.observe(this) { items ->
            allEvents.clear()
            allEvents.addAll(items)
            allEvents.sortBy { it.timestamp }

            changeListRecycleView(offset)
        }

        /* Cambia la lista degli eventi in base alla data selezionata dall'utente nel calendario */
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            calendar = Calendar.Builder().setDate(year,month,dayOfMonth).build()
            changeListRecycleView(offsetMillisDays)
        }

        /* Quando viene premuto il tasto per tornare indietro termina l'Activity e torna alla home */
        backButton.setOnClickListener {
            finish()
        }

        /* Cambia la visualizzazione della lista e nasconde il calendario*/
        upcomingEventBtn.setOnClickListener { _ ->
            isCalendar = false
            offset = offsetMillisDays
            upcomingViewVisible()
            changeListRecycleView(offsetMillisDays)
        }

        /* Torna alla visualizzazione con il calendario */
        calendarButton.setOnClickListener {
            isCalendar = true
            offset = offsetMillisDays

            calendarViewVisible()
            changeListRecycleView(offsetMillisDays)

        }

        /* Permette di visualizzare gli eventi nel giorno selezionato nel calendario */
        dayButton.setOnClickListener {
            offset = offsetMillisDays
            changeListRecycleView(offsetMillisDays)
        }

        /* Permette di visualizzare gli eventi fino a 7 giorni a partire da quello selezionato nel calendario */
        weekButton.setOnClickListener {
            offset = offsetMillisWeek
            changeListRecycleView(offsetMillisWeek)
        }

        /* Permette di visualizzare gli eventi fino a 30 giorni a partire da quello selezionato nel calendario */
        monthButton.setOnClickListener {
            offset = offsetMillisMonth
            changeListRecycleView(offsetMillisMonth)
        }

        /* Apre l'alert dialog per aggiungere un evento quando viene premuto il floating button*/
        floatingActionButton.setOnClickListener {
            startAddDialog()
        }

    }

    /**
     * Apre l'alert dialog per aggiungere un evento
     */
    private fun startAddDialog() {

        val builder = AlertDialog.Builder(this)

        /* Vengono caricati i layout definiti nell'xml */
        val customLayout : View = layoutInflater.inflate(R.layout.activity_addeventdialog, null)

        val newEventText  = customLayout.findViewById<EditText>(R.id.newEventText)

        val timePicker = customLayout.findViewById<TimePicker>(R.id.newEventTime)

        builder.setView(customLayout)

        /* Salva la variabile di stato corrispondente alla precendente creazione dell'Activity
        * prima di assegnarle il valore true
        * */
        dismiss = isAdd

        isAdd = true

        /* Imposta il formato 24h */
        timePicker.setIs24HourView(true)

        /* Se il dialog è stato aperto in precedenza ripristina i valori impostati dall'utente */
        if(dismiss) {
            timePicker.hour = hourAdd
            timePicker.minute = minuteAdd
            newEventText.setText(newEventAdd)
        }

        timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            hourAdd = hourOfDay
            minuteAdd = minute
        }

        newEventText.doAfterTextChanged {
            newEventAdd = newEventText.text.toString()
        }

        builder.setPositiveButton(getString(R.string.add)) { _: DialogInterface?, _: Int ->
            val hour = timePicker.hour
            val minute = timePicker.minute
            /* CurrentDate estrae la data impostata nel calendario formattandola attraverso date format */
            val currentDate = dateFormat.format(calendarView.date)
            /* dateMillis riconverte currentDate in Long. Questa operazione è necessaria per ottenere il timestamp
            * della data attuale senza tenere conto dell'orario */
            val dateMillis = dateFormat.parse(currentDate)!!.time

            var newEventName = newEventText.text.toString()
            if(newEventName == "") newEventName = getString(R.string.new_event)

            /* crea un oggetto di tipo ToDoEvent con i valori inseriti dell'utente */
            val event = ToDoEvent(
                0,
                getTimestamp(dateMillis,hour,minute),
                newEventName,
                false
            )
            /* inserisce l'evento nel database */
            eventViewModel.insert(event)
            isAdd = false

        }

        /* Annulla l'operazione di inserimento segnalandolo all'utente attraverso un Toast */
        builder.setNegativeButton(getString(R.string.cancel)) { _: DialogInterface?, _ ->
            Toast.makeText(this, getString(R.string.event_cancelled), Toast.LENGTH_SHORT).show()
            isAdd = false
            newEventAdd = ""
            hourAdd = calendar.get(Calendar.HOUR_OF_DAY)
            minuteAdd = calendar.get((Calendar.MINUTE))
            dialog.cancel()
        }

        dialog = builder.create()

        dialog.show()

        /* Impedisce all'Alert dialog di essere chiuso quando l'utente tocca all'esterno di esso */
        dialog.setCanceledOnTouchOutside(false)
    }


    /**
     * Inizializza la RecyclerView che dovrà contenere gli eventi
     */
    private fun setCurrentDateRecyclerView(){
        val off = offset
        currentDateRecyclerView= findViewById(R.id.current_date_recycler_view)
        currentDateAdapter = EventAdapter()

        /* permette alla recyclerview di far comparire un menu di contesto per ogni suo elemento */
        registerForContextMenu(currentDateRecyclerView)

        /* Utilizza l'interfaccia EventAdapter.ViewHolderClickListener per ricevere notifiche nel caso venga
        * premuta la checkbox e modifica il relativo evento all'interno del database
        * */
        currentDateAdapter.callback = object: EventAdapter.ViewHolderClickListener{
            override fun onCheckedChange(position: Int) {
                val modifiedEvent = currentDateAdapter.getEvent(position)
                if (modifiedEvent.completed) {
                    eventViewModel.updateChecked(modifiedEvent, false)
                }
                else
                    eventViewModel.updateChecked(modifiedEvent, true)
            }
        }
        currentDateRecyclerView.adapter = currentDateAdapter
        currentDateRecyclerView.layoutManager = LinearLayoutManager(this)
        changeListRecycleView(off)
    }

    /**
     * Quando viene premuta l'opzione Elimina del menu di contesto rimouve l'elemento corrispondente dal database
     */
    override fun onContextItemSelected(item: MenuItem): Boolean {
        return if(item.title == getString(R.string.delete)){
            eventViewModel.delete(currentDateAdapter.getEvent(item.groupId))
            Toast.makeText(this, getString(R.string.event_deleted), Toast.LENGTH_SHORT).show()
            true
        } else
            super.onContextItemSelected(item)
    }

    /**
     * Salva il valore attuale delle variabili di stato
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("current_date",calendar.timeInMillis)
        outState.putBoolean("is_calendar", isCalendar)
        outState.putLong("selected_button", offset)
        outState.putBoolean("is_add", isAdd)
        if(isAdd) dialog.cancel()
        outState.putString("new_event_add", newEventAdd)
        outState.putInt("hour_add", hourAdd)
        outState.putInt("minute_add", minuteAdd)
    }

    /**
     * Cambia la visibilità dei componenti per visualizzare la lista a schermo intero
     */
    private fun upcomingViewVisible() {
        upcomingEventBtn.visibility = View.GONE
        calendarButton.visibility = View.VISIBLE
        calendarView.visibility = View.GONE
        eventTextView.visibility = View.GONE
        toggleGroup.visibility = View.VISIBLE
    }

    /**
     * Cambia la visibilità dei componenti per visualizzare il calendario e la lista sotto di esso
     */
    private fun calendarViewVisible() {
        calendarButton.visibility = View.GONE
        upcomingEventBtn.visibility = View.VISIBLE
        toggleGroup.visibility = View.GONE
        calendarView.visibility = View.VISIBLE
        eventTextView.visibility = View.VISIBLE
        isCalendar = true
        offset = 0
    }

    /**
     * Aggiorna il contenuto della lista degli eventi in base ai cambiamenti del database e
     * al pulsante premuto (giorno, settimana, mese)
     */
    private fun changeListRecycleView(offsetDays: Long) {

        offset = offsetDays

        /* Salva il colore relativo allo stato abilitato del pulsante dayButton */
        val defaultColor = dayButton.backgroundTintList!!.getColorForState(intArrayOf(android.R.attr.state_enabled), 0)

        /* CurrentDate estrae la data impostata nel calendario formattandola attraverso date format */
        val currentDate = dateFormat.format(calendar.timeInMillis)

        /* Riconverte currentDate in Long e ne assegna il valore a calendarView.date. Questa operazione
        * è necessaria per ottenere il timestamp della data attuale senza tenere conto dell'orario
        */
        calendarView.date = dateFormat.parse(currentDate)!!.time
        selectedDayEventList.clear()
        currentDateRecyclerView.removeAllViews()
        for (event in allEvents) {

            /* Se OffsetDays corrisponde a offsetMillisDays vengono inseriti nella lista solo gli elementi
            * corrispondenti al giorno selezionato nel calendario
            * */
            if(offsetDays == offsetMillisDays) {
                if(dateFormat.format(event.timestamp) == dateFormat.format(calendarView.date)) {
                    selectedDayEventList.add(event)
                }
                /* cambia colore al bottone dayButton se selezionato e reimposta gli altri */
                dayButton.setBackgroundColor(Color.rgb(196,41,191))
                weekButton.setBackgroundColor(defaultColor)
                monthButton.setBackgroundColor(defaultColor)
            }
            else {
                /* Se OffsetDays non corrisponde a offsetMillisDays vengono inseriti nella lista solo gli elementi
                * corrispondenti alla settimana o al mese del giorno selezionato nel calendario.
                * Se OffsetDays è uguale a offsetMillisWeek vengono aggiunti quelli della settimana
                * Se OffsetDays è uguale a offsetMillisMonth vengono aggiunti quelli del mese
                * */
                if (event.timestamp >= calendarView.date &&
                    (event.timestamp) <= ((calendarView.date) + offsetDays)) {
                    selectedDayEventList.add(event)
                }
                /* cambia colore al bottone weekButton se selezionato e reimposta gli altri */
                if (offsetDays == offsetMillisWeek) {
                    weekButton.setBackgroundColor(Color.rgb(196,41,191))
                    dayButton.setBackgroundColor(defaultColor)
                    monthButton.setBackgroundColor(defaultColor)
                }
                /* cambia colore al bottone monthButton se selezionato e reimposta gli altri */
                else if(offsetDays == offsetMillisMonth) {
                    monthButton.setBackgroundColor(Color.rgb(196,41,191))
                    dayButton.setBackgroundColor(defaultColor)
                    weekButton.setBackgroundColor(defaultColor)
                }
            }
        }

        /* Se la lista degli eventi non è vuota rende visibile la recyclerView */
        if (selectedDayEventList.isNotEmpty()){
            currentDateRecyclerView.visibility = View.GONE
            emptyListTextView.visibility = View.GONE
            currentDateRecyclerView.visibility = View.VISIBLE
        }
        /* Se la lista degli eventi è vuota rende visibile la textView che indica l'assenza di eventi */
        else{
            currentDateRecyclerView.visibility = View.GONE
            emptyListTextView.visibility = View.VISIBLE
        }
        /* Invia la lista di eventi all'Adapter */
        currentDateAdapter.submitList(selectedDayEventList)

        /* Imposta il valore del testo di dayButton con la data selezionata nel calendario */
        val date : String = dateFormat.format(calendarView.date).toString()
        dayButton.text = date
    }

    /* Ritorna un Long corrispondente ad un timestamp ottenuto da un valore di partenza a cui vengono aggiunte ore e minuti*/
    private fun getTimestamp(dateMillis: Long, hour: Int = 0, minutes: Int = 0) : Long {
        return dateMillis + ((hour * 60 + minutes) * 60 * 1000)
    }

    override fun onResume()
    {
        super.onResume()
        if(!isCalendar) {
            upcomingViewVisible()
            changeListRecycleView(offset)
        }
        if(isAdd) {
            dialog.cancel()
            startAddDialog()
        }
    }

    override fun onRestart() {
        super.onRestart()
        if(!isCalendar) {
            upcomingViewVisible()
            changeListRecycleView(offset)
        }
        if(isAdd) {
            dialog.cancel()
            startAddDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        if(!isCalendar) {
            upcomingViewVisible()
            changeListRecycleView(offset)
        }
        if(isAdd) {
            dialog.cancel()
            startAddDialog()
        }
    }


    companion object {

        /* Permette di convertire un Long nel formato dd/mm/yyyy */
        val dateFormat : SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
        private const val weekDays : Long = 7
        private const val monthDays : Long = 30

        /* Valore di offset in millisecondi corrispondente al giorno */
        const val offsetMillisDays : Long = 0
        /* Valore di offset in millisecondi corrispondente alla settimana */
        const val offsetMillisWeek : Long = weekDays * 24 * 60 * 60 * 1000
        /* Valore di offset in millisecondi corrispondente al mese */
        const val offsetMillisMonth : Long = monthDays * 24 * 60 * 60 * 1000
    }

}

