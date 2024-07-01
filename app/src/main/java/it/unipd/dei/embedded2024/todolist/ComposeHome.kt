package it.unipd.dei.embedded2024.todolist

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.unipd.dei.embedded2024.todolist.ui.theme.ToDoListTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import java.util.Locale
import java.util.TimeZone

/** ComposeHome è la classe che rappresenta l'activity realizzata tramite Jetpack Compose */
class ComposeHome : ComponentActivity() {

    /** Classe ausiliaria che associa ad ogni ToDoEvent lo stato aperto/chiuso del DropDownMenu associato */
    data class ToDoEventWithDropdown(
        val event: ToDoEvent,
        val dropDownExpanded: MutableState<Boolean>
    )

    /* Lista di tutti gli eventi ottenuti dal ViewModel  */
    private var allEvents = ArrayList<ToDoEvent>()
    /* Lista degli eventi prossimi alla data selezionata;
    *   l'utilizzo di SnapshotStateList consente alle composable che ne dipendono di reagire a cambiamenti della lista */
    private val upcomingEventsList = SnapshotStateList<ToDoEventWithDropdown>()
    private lateinit var activityContext: Context

    /* ViewModel da cui si ottengono i dati del database locale */
    private val eventViewModel: EventViewModel by viewModels {
        EventViewModelFactory((application as EventApplication).repository)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        activityContext = this

        setContent {
            ToDoListTheme {
                /* Motivi per l'uso di rememberSaveable:
                *   1) consente di rendere visibile il valore al suo interno a Compose (e quindi alla ricomposizione)
                *   2) rememberSaveable salva automaticamente il valore contenuto nel Bundle che è quindi
                *      recuperato automaticamente al ripristino dello stato dell'activity, al contrario di remember */

                /* Flag mutabile che rappresenta lo stato aperto/chiuso del dialog per l'aggiunta di un nuovo evento */
                val addEventDialogOpen = rememberSaveable { mutableStateOf(false) }
                /* Flag mutabile che determina quale schermata è attualmente visibile:
                *   true = visualizzazione a calendario
                *   false = visualizzazione a liste */
                val upcomingEventsViewOpen = rememberSaveable { mutableStateOf(false) }
                /* Variabile di stato per il DatePicker, inizialmente impostata alla data attuale */
                val datePickerState: DatePickerState = rememberDatePickerState(calendar.timeInMillis)
                /* Flag per l'orientamento dello schermo;
                *   poiché al cambio di orientamento onCreate è eseguita di nuovo, si aggiorna anche la flag */
                val isLandscape = when (LocalConfiguration.current.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> { true }
                    else -> { false }
                }
                /* Variabile di stato che contiene il range attuale per il quale considerare gli eventi prossimi */
                val upcomingEventsRange = rememberSaveable { mutableLongStateOf(rangeMillisDay) }

                /* Imposta un observer che reagisce ai cambiamenti nella lista di eventi del ViewModel;
                *   nello specifico, cancella e riottiene la lista di eventi locale all'activity e aggiorna
                *   la lista di eventi prossimi secondo l'attuale valore di upcomingEventsRange */
                eventViewModel.allEvents.observe(this) { items ->
                    allEvents.clear()
                    allEvents.addAll(items.sortedBy { it.timestamp })

                    updateUpcomingEventsList(
                        datePickerState.selectedDateMillis!!,
                        upcomingEventsRange.longValue
                    )
                }

                /* Organizzatore di layout Material Design; ha come parametri gli elementi propri del layout Material
                *   e come contenuto il resto dello schermo */
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    /* Barra di navigazione in cima allo schermo */
                    topBar = {
                        TopAppBar(
                            /* Titolo app */
                            title = {
                                Text(resources.getString(R.string.app_name))
                            },
                            /* Bottone per tornare alla schermata iniziale */
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_home_black_30dp),
                                        contentDescription = "Back to initial"
                                    )
                                }
                            },
                            actions = {
                                /* Bottone per passare dalla visualizzazione a calendario a quella a lista e viceversa;
                                *   l'icona del bottone cambia a seconda di quale delle due è aperta al momento */
                                val iconPainter = if(!upcomingEventsViewOpen.value) painterResource(R.drawable.ic_checklist_rtl_white_30dp)
                                    else painterResource(R.drawable.ic_calendar_month_white_30dp)

                                IconButton(onClick = { upcomingEventsViewOpen.value = !upcomingEventsViewOpen.value }) {
                                    Icon(
                                        painter = iconPainter,
                                        contentDescription = getString(R.string.back_to_list)
                                    )
                                }
                            }
                        )
                    },
                    /* Floating action button che apre il dialog per l'inserimento di un nuovo evento */
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { addEventDialogOpen.value = true },
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add event")
                        }
                    }
                ) { innerPadding ->
                    /* Mostra la visualizzazione a calendario se la flag è false */
                    if(!upcomingEventsViewOpen.value) {
                        /* Mostra la versione della schermata corrispondente all'orientamento dello schermo */
                        if(isLandscape) {
                            MainViewLandscape(
                                innerPadding = innerPadding,
                                datePickerState = datePickerState
                            )
                        } else {
                            MainViewPortrait(
                                innerPadding = innerPadding,
                                datePickerState = datePickerState
                            )
                        }

                        /* Dialog di aggiunta eventi */
                        AddEventDialog(
                            addEventDialogOpen = addEventDialogOpen,
                            datePickerState = datePickerState
                        )

                        /* Monitora lo stato del DatePicker e chiama la funzione di aggiornamento
                        *   della lista di prossimi eventi quando la data selezionata cambia */
                        LaunchedEffect(datePickerState.selectedDateMillis) {
                            updateUpcomingEventsList(datePickerState.selectedDateMillis!!, rangeMillisDay)
                        }
                    } else {
                        /* Mostra la visualizzazione a liste se la flag è true */
                        UpcomingEventsView(
                            innerPadding,
                            datePickerState.selectedDateMillis!!,
                            upcomingEventsRange
                        )
                    }
                }
            }
        }
    }

    /** Composable che emette la visualizzazione a calendario se l'orientamento è verticale
    *   Parametri:  - innerPadding: padding definito da Scaffold
    *               - datePickerState: variabile di stato del DatePicker */
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    private fun MainViewPortrait(
        innerPadding: PaddingValues,
        datePickerState: DatePickerState
    ) {
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            /* DatePicker principale */
            DatePicker(
                state = datePickerState,
                title = null,
                headline = null,
                showModeToggle = false
            )
            Text(
                getString(R.string.current_date_events_header),
                style = MaterialTheme.typography.titleLarge
            )

            /* Lista di elementi da visualizzare (come RecyclerView) */
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.70f),
                horizontalAlignment = Alignment.CenterHorizontally,
                userScrollEnabled = true
            ) {
                /* Se la lista è vuota si visualizza solo un messaggio che indica che è vuota */
                item {
                    if (upcomingEventsList.isEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            getString(R.string.empty_events_list_label),
                            Modifier.standardModifierChain(
                                horizontalPadding = 20.dp,
                                verticalPadding = 12.dp,
                                maxWidthPercentage = 0.9f
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                /* Lista di elementi da visualizzare (come RecyclerView) */
                items(upcomingEventsList.size) { i ->
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier.wrapContentSize(),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Card(
                            Modifier
                                .standardModifierChain(
                                    horizontalPadding = 10.dp,
                                    maxWidthPercentage = 0.95f
                                )
                                /* Apre il DropDown in seguito a un long press della Card */
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = {
                                        upcomingEventsList[i].dropDownExpanded.value = true
                                    }
                                )
                        ) {
                            Row (
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                /* Label che visualizzano descrizione e ora dell'evento */
                                Column(Modifier.fillMaxWidth(0.70f)) {
                                    Text(
                                        upcomingEventsList[i].event.description,
                                        Modifier.standardModifierChain(
                                            horizontalPadding = 12.dp,
                                            verticalPadding = 10.dp
                                        ),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        timeFormat.format(upcomingEventsList[i].event.timestamp),
                                        Modifier.standardModifierChain(
                                            horizontalPadding = 12.dp,
                                            verticalPadding = 10.dp
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                /* Checkbox per segnare un evento come completato o non */
                                Checkbox(
                                    checked = upcomingEventsList[i].event.completed,
                                    onCheckedChange = { value ->
                                        eventViewModel.updateChecked(upcomingEventsList[i].event, value)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        /* DropDown relativo all'elemento corrente */
                        DropDownDelete(upcomingEventsList[i])
                    }
                }
            }
        }
    }

    /** Composable che emette la visualizzazione a calendario se l'orientamento è orizzontale
    *   Parametri:
    *       - innerPadding: padding definito da Scaffold
    *       - datePickerState: variabile di stato del DatePicker */
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    private fun MainViewLandscape(
        innerPadding: PaddingValues,
        datePickerState: DatePickerState
    ) {
        Row(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) {
            /* DatePicker principale, con dei modifiers aggiuntivi per ovviare a problemi di ridimensionamento in landscape */
            DatePicker(
                state = datePickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .aspectRatio(1.1f)
                    .scale(0.90f)
            )

            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    getString(R.string.current_date_events_header),
                    style = MaterialTheme.typography.titleLarge
                )

                /* Lista di elementi da visualizzare (come RecyclerView) */
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.70f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    userScrollEnabled = true
                ) {
                    /* Se la lista è vuota si visualizza solo un messaggio che indica che è vuota */
                    item {
                        if (upcomingEventsList.isEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                getString(R.string.empty_events_list_label),
                                Modifier.standardModifierChain(
                                    horizontalPadding = 12.dp,
                                    verticalPadding = 10.dp,
                                    maxWidthPercentage = 0.9f
                                ),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    /* Lista di elementi da visualizzare (come RecyclerView) */
                    items(upcomingEventsList.size) { i ->
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier.wrapContentSize(),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Card(
                                Modifier
                                    .standardModifierChain(
                                        horizontalPadding = 10.dp,
                                        maxWidthPercentage = 0.9f
                                    )
                                    /* Apre il DropDown in seguito a un long press della Card */
                                    .combinedClickable(
                                        onClick = { },
                                        onLongClick = {
                                            upcomingEventsList[i].dropDownExpanded.value = true
                                        }
                                    )
                            ) {
                                Row (
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    /* Label che visualizzano descrizione e ora dell'evento */
                                    Column(Modifier.fillMaxWidth(0.70f)) {
                                        Text(
                                            upcomingEventsList[i].event.description,
                                            Modifier.standardModifierChain(
                                                horizontalPadding = 10.dp,
                                                verticalPadding = 8.dp
                                            ),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            timeFormat.format(upcomingEventsList[i].event.timestamp),
                                            Modifier.standardModifierChain(
                                                horizontalPadding = 10.dp,
                                                verticalPadding = 8.dp
                                            ),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    /* Checkbox per segnare un evento come completato o non */
                                    Checkbox(
                                        checked = upcomingEventsList[i].event.completed,
                                        onCheckedChange = { value ->
                                            eventViewModel.updateChecked(upcomingEventsList[i].event, value)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            /* DropDown relativo all'elemento corrente */
                            DropDownDelete(upcomingEventsList[i])
                        }
                    }
                }
            }
        }
    }

    /** Composable che emette il dialog di aggiunta eventi
    *   Parametri:
    *       - addEventDialogOpen: flag di apertura del dialog
    *       - datePickerState: variabile di stato del DatePicker */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddEventDialog(
        addEventDialogOpen: MutableState<Boolean>,
        datePickerState: DatePickerState,
    ) {
        /* Variabile mutabile che rappresenta il testo inserito nella casella di testo */
        val eventText = rememberSaveable { mutableStateOf("") }
        /* Variabile di stato per il TimePicker (impostata manualmente in formato 24 ore) */
        val timePickerState : TimePickerState = rememberTimePickerState(is24Hour = true)

        /* Mostra il dialog solo se la flag è true */
        if(addEventDialogOpen.value) {
            AlertDialog(
                /* Disabilita chiudere il dialog premendo al di fuori di esso*/
                onDismissRequest = { },
                confirmButton = {
                    /* Bottone di conferma */
                    TextButton(onClick = {
                        /* Se non è stato inserito testo ne viene assegnato uno di default */
                        if(eventText.value == "") {
                            eventText.value = getString(R.string.new_event)
                        }
                        /* Genera il timestamp per il nuovo evento tenendo in considerazione il fuso orario */
                        val timezoneOffset = TimeZone.getDefault().getOffset(calendar.timeInMillis)
                        val timestamp = getTimestamp(
                            datePickerState.selectedDateMillis!! - timezoneOffset,
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        val newEvent = ToDoEvent(
                            0,
                            timestamp,
                            eventText.value,
                            false
                        )
                        /* Inserisce il nuovo evento nel ViewModel e poi chiude il dialog*/
                        eventViewModel.insert(newEvent)
                        addEventDialogOpen.value = false
                        eventText.value = ""
                    }) {
                        Text(getString(R.string.confirm_button_text))
                    }
                },
                dismissButton = {
                    /* Bottone di chiusura del dialog, con Toast aggiuntivo per confermare l'annullamento dell'inserimento*/
                    TextButton(onClick = {
                        addEventDialogOpen.value = false
                        Toast.makeText(
                            activityContext,
                            getString(R.string.event_cancelled),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text(getString(R.string.close_button_text))
                    }
                },
                title = {
                    Text(getString(R.string.add_new_event_header))
                },
                /* Contenuto del dialog */
                text = {
                    Column {
                        /* Campo di input per l'ora */
                        TimeInput(state = timePickerState)
                        /* Campo di input per il testo */
                        TextField(
                            value = eventText.value,
                            /* Viene modificata eventText solo se il numero di caratteri è inferiore al massimo definito */
                            onValueChange = {
                                if(eventText.value.length <= maxEventTextLength)
                                    eventText.value = it
                            },
                            /* Testo placeholder a testo vuoto */
                            placeholder = {
                                Text(getString(R.string.new_event_description_placeholder))
                            },
                            minLines = 3
                        )
                    }
                }
            )
        }
    }

    /** Composable che emette la visualizzazione a lista
    *   Parametri:
    *       - innerPadding: padding definito da Scaffold
    *       - startingDateMillis: data di partenza in millisecondi per gli eventi prossimi
    *       - upcomingEventsRange: range attuale per gli eventi prossimi */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun UpcomingEventsView(
        innerPadding: PaddingValues,
        startingDateMillis: Long,
        upcomingEventsRange: MutableState<Long>
    ) {
        /* Definizione delle variabili di stato per i colori dei tre bottoni */
        val defaultButtonColor = ButtonDefaults.buttonColors().containerColor
        val selectedButtonColor = Color(69, 140, 228)
        val dayButtonColor = rememberSaveable { mutableIntStateOf(selectedButtonColor.toArgb()) }
        val weekButtonColor = rememberSaveable { mutableIntStateOf(defaultButtonColor.toArgb()) }
        val monthButtonColor = rememberSaveable { mutableIntStateOf(defaultButtonColor.toArgb()) }

        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            /* Raggruppamento dei tre bottoni */
            Row(
                Modifier
                    .fillMaxWidth(0.8f)
                    .padding(top = 15.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                /* Bottone per selezionare cone range solo la data selezionata */
                Button(
                    /* Aggiorna la lista di eventi e i colori dei bottoni per indicare che questo è selezionato */
                    onClick = {
                        upcomingEventsRange.value = rangeMillisDay
                        updateUpcomingEventsList(
                            startingDateMillis,
                            upcomingEventsRange.value
                        )
                        dayButtonColor.intValue = selectedButtonColor.toArgb()
                        weekButtonColor.intValue = defaultButtonColor.toArgb()
                        monthButtonColor.intValue = defaultButtonColor.toArgb()
                    },
                    border = BorderStroke(1.dp, Color.White),
                    shape = RoundedCornerShape(topStart = 100f, bottomStart = 100f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(dayButtonColor.intValue))
                ) {
                    Text(dateFormat.format(startingDateMillis))
                }
                /* Bottone per selezionare cone range una settimana data selezionata */
                Button(
                    /* Aggiorna la lista di eventi e i colori dei bottoni per indicare che questo è selezionato */
                    onClick = {
                        upcomingEventsRange.value = rangeMillisWeek
                        updateUpcomingEventsList(
                            startingDateMillis,
                            upcomingEventsRange.value
                        )
                        dayButtonColor.intValue = defaultButtonColor.toArgb()
                        weekButtonColor.intValue = selectedButtonColor.toArgb()
                        monthButtonColor.intValue = defaultButtonColor.toArgb()
                    },
                    border = BorderStroke(1.dp, Color.White),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(weekButtonColor.intValue))
                ) {
                    Text(getString(R.string.week))
                }
                /* Bottone per selezionare cone range un mese dalla data selezionata */
                Button(
                    /* Aggiorna la lista di eventi e i colori dei bottoni per indicare che questo è selezionato */
                    onClick = {
                        upcomingEventsRange.value = rangeMillisMonth
                        updateUpcomingEventsList(
                            startingDateMillis,
                            upcomingEventsRange.value
                        )
                        dayButtonColor.intValue = defaultButtonColor.toArgb()
                        weekButtonColor.intValue = defaultButtonColor.toArgb()
                        monthButtonColor.intValue = selectedButtonColor.toArgb()
                    },
                    border = BorderStroke(1.dp, Color.White),
                    shape = RoundedCornerShape(topEnd = 100f, bottomEnd = 100f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(monthButtonColor.intValue))
                ) {
                    Text(getString(R.string.month))
                }
            }

            /* Lista di elementi da visualizzare (come RecyclerView) */
            LazyColumn(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                userScrollEnabled = true
            ) {
                /* Se la lista è vuota si visualizza solo un messaggio che indica che è vuota */
                item {
                    if (upcomingEventsList.isEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            getString(R.string.empty_events_list_label),
                            Modifier.standardModifierChain(
                                horizontalPadding = 12.dp,
                                verticalPadding = 10.dp,
                                maxWidthPercentage = 0.9f
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                /* Per ogni elemento della lista viene generata una Card */
                items(upcomingEventsList.size) { i ->
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier.wrapContentSize(),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Card(
                            Modifier
                                .standardModifierChain(
                                    horizontalPadding = 10.dp,
                                    maxWidthPercentage = 0.9f
                                )
                                /* Apre il DropDown in seguito a un long press della Card */
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = {
                                        upcomingEventsList[i].dropDownExpanded.value = true
                                    }
                                )
                        ) {
                            Row (
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                /* Label che visualizzano descrizione, data e ora dell'evento */
                                Column(Modifier.fillMaxWidth(0.70f)) {
                                    Text(
                                        upcomingEventsList[i].event.description,
                                        Modifier.standardModifierChain(
                                            horizontalPadding = 10.dp,
                                            verticalPadding = 8.dp
                                        ),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        dateFormat.format(upcomingEventsList[i].event.timestamp),
                                        Modifier.standardModifierChain(
                                            horizontalPadding = 10.dp,
                                            verticalPadding = 8.dp
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        timeFormat.format(upcomingEventsList[i].event.timestamp),
                                        Modifier.standardModifierChain(
                                            horizontalPadding = 10.dp,
                                            verticalPadding = 8.dp
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                /* Checkbox per segnare un evento come completato o non */
                                Checkbox(
                                    checked = upcomingEventsList[i].event.completed,
                                    onCheckedChange = { value ->
                                        /* Aggiorna il campo completed dell'evento nel ViewModel */
                                        eventViewModel.updateChecked(upcomingEventsList[i].event, value)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        /* DropDown relativo all'elemento corrente */
                        DropDownDelete(upcomingEventsList[i])
                    }
                }
            }
        }
    }

    /** Composable che emette un DropDownMenu
    *   Parametri:
    *       - item: evento relativo al DropDownMenu, con stato del menu associato */
    @Composable
    private fun DropDownDelete(item: ToDoEventWithDropdown) {
        DropdownMenu(
            expanded = item.dropDownExpanded.value,
            onDismissRequest = { item.dropDownExpanded.value = false }
        ) {
            /* Elemento del menu */
            DropdownMenuItem(
                text = { Text(getString(R.string.delete)) },
                /* Se cliccato, cancella l'evento associato dal ViewModel e mostra un Toast di conferma */
                onClick = {
                    eventViewModel.delete(item.event)
                    Toast.makeText(
                        activityContext,
                        getString(R.string.event_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    /** Funzione che aggiorna la lista di eventi prossimi
    *   Parametri:
    *       - startingDateMillis: data di inizio della lista in millisecondi
    *       - rangeMillis: estensione in millisecondi dell'intervallo da considerare */
    private fun updateUpcomingEventsList(
        startingDateMillis: Long,
        rangeMillis: Long
    ) {
        /* Svuota la lista */
        upcomingEventsList.clear()
        /* Per ogni evento della lista completa controlla se va inserito nella lista */
        for (event in allEvents) {
            /* Se il range è nullo (si considera solo la data corrente) fa un controllo solo sulla data */
            if(rangeMillis.toInt() == 0) {
                if(dateFormat.format(event.timestamp) == dateFormat.format(startingDateMillis)) {
                    upcomingEventsList.add(
                        ToDoEventWithDropdown(
                            event,
                            mutableStateOf(false)
                        )
                    )
                }
            }
            /* Se il range non è nullo, controlla se l'evento appartiene al range */
            else {
                if (event.timestamp >= startingDateMillis &&
                    (event.timestamp) <= (startingDateMillis + rangeMillis)) {
                    upcomingEventsList.add(
                        ToDoEventWithDropdown(
                            event,
                            mutableStateOf(false)
                        )
                    )
                }
            }
        }
    }

    /** Funzione che genera un timestamp a partire dalla data (in millisecondi), ora e minuto specificati*/
    private fun getTimestamp(
        dateMillis: Long,
        hour: Int = 0,
        minutes: Int = 0
    ) : Long {
        return dateMillis + (hour * 60 + minutes) * 60 * 1000
    }

    /** Funzione per generare una catena di modifiers usata spesso all'intero di questa activity */
    private fun Modifier.standardModifierChain(
        horizontalPadding : Dp = 0.dp,
        verticalPadding : Dp = 0.dp,
        maxWidthPercentage : Float = 1.0f
    ) : Modifier {
        return this.then(
            Modifier
                .padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding
                )
                .fillMaxWidth(maxWidthPercentage)
                .wrapContentHeight()
        )
    }

    companion object {

        /* Istanza del calendario */
        val calendar: Calendar = Calendar.getInstance()

        /* Formati di data e ora statici */
        val timeFormat : SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.ITALY)
        val dateFormat : SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)

        /* Range in millisecondi per gli eventi prossimi */
        private const val weekDays : Long = 7
        private const val monthDays : Long = 30
        const val rangeMillisDay : Long = 0
        const val rangeMillisWeek : Long = weekDays * 24 * 60 * 60 * 1000
        const val rangeMillisMonth : Long = monthDays * 24 * 60 * 60 * 1000

        /* Lunghezza massima del testo di un evento */
        const val maxEventTextLength : Int = 60
    }
}