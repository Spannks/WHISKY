package app.whiskyjournal.ui.tasting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.whiskyjournal.AppContainer
import app.whiskyjournal.data.model.BrandContact
import app.whiskyjournal.data.model.Event
import app.whiskyjournal.data.model.FlavourHit
import app.whiskyjournal.data.model.FlavourResponse
import app.whiskyjournal.data.model.NewTasting
import app.whiskyjournal.data.model.Phase
import app.whiskyjournal.data.model.TastingFlavourInput
import app.whiskyjournal.data.model.WhiskyDetail
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class FlavourChoice(
    val termId: Int,
    val term: String,
    val category: String,
    val phase: Phase,
    val response: FlavourResponse = FlavourResponse.UNANSWERED,
    val intensity: Int? = null,
    val suggested: Boolean = true,
)

class NewTastingViewModel(private val container: AppContainer, private val whiskyId: String) : ViewModel() {

    var whisky by mutableStateOf<WhiskyDetail?>(null)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set

    /** Suggested chips (from the catalogue's tasting notes) plus anything the user adds. */
    val flavours = mutableStateListOf<FlavourChoice>()

    var tastedAt by mutableStateOf(Instant.now())
    var locationName by mutableStateOf("")
    var rating by mutableStateOf<Int?>(null)
    var wouldBuy by mutableStateOf(false)
    var notes by mutableStateOf("")

    var events by mutableStateOf<List<Event>>(emptyList())
        private set
    var contacts by mutableStateOf<List<BrandContact>>(emptyList())
        private set
    var eventId by mutableStateOf<String?>(null)
    var contactId by mutableStateOf<String?>(null)

    var addPhase by mutableStateOf(Phase.PALATE)
    var flavourQuery by mutableStateOf("")
        private set
    var flavourHits by mutableStateOf<List<FlavourHit>>(emptyList())
        private set
    private var flavourJob: Job? = null

    var saving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            runCatching { coroutineScope {
                val w = async { container.catalogue.whisky(whiskyId) }
                val s = async { container.catalogue.flavourSuggestions(whiskyId) }
                val e = async { container.tastings.events() }
                val c = async { container.tastings.contacts() }
                whisky = w.await()
                flavours.addAll(s.await().map {
                    FlavourChoice(it.termId, it.term, it.category, Phase.of(it.phase))
                })
                events = e.await()
                contacts = c.await()
            } }.onFailure { loadError = it.message }
        }
    }

    fun cycle(choice: FlavourChoice) {
        val i = flavours.indexOf(choice).takeIf { it >= 0 } ?: return
        val next = choice.response.next()
        flavours[i] = choice.copy(
            response = next,
            intensity = if (next == FlavourResponse.CONFIRMED) choice.intensity ?: 2 else null,
        )
    }

    fun setIntensity(choice: FlavourChoice, intensity: Int) {
        val i = flavours.indexOf(choice).takeIf { it >= 0 } ?: return
        flavours[i] = choice.copy(intensity = intensity)
    }

    fun onFlavourQuery(q: String) {
        flavourQuery = q
        flavourJob?.cancel()
        if (q.isBlank()) { flavourHits = emptyList(); return }
        flavourJob = viewModelScope.launch {
            delay(200)
            flavourHits = runCatching { container.catalogue.searchFlavours(q) }.getOrDefault(emptyList())
        }
    }

    /** Adds a flavour the user tasted that wasn't suggested; it's confirmed straight away. */
    fun addFlavour(hit: FlavourHit) {
        val existing = flavours.indexOfFirst { it.termId == hit.termId && it.phase == addPhase }
        if (existing >= 0) {
            flavours[existing] = flavours[existing].copy(response = FlavourResponse.CONFIRMED, intensity = 2)
        } else {
            flavours.add(
                FlavourChoice(hit.termId, hit.term, hit.category, addPhase,
                    FlavourResponse.CONFIRMED, intensity = 2, suggested = false)
            )
        }
        flavourQuery = ""
        flavourHits = emptyList()
    }

    /** Keeps the time of day, changes the date (DatePicker gives UTC midnight millis). */
    fun setDate(utcMillis: Long) {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(utcMillis).atZone(ZoneId.of("UTC")).toLocalDate()
        val time = tastedAt.atZone(zone).toLocalTime()
        tastedAt = LocalDate.from(date).atTime(time).atZone(zone).toInstant()
    }

    fun createEvent(name: String, venue: String) = viewModelScope.launch {
        runCatching { container.tastings.addEvent(name, venue) }
            .onSuccess { events = listOf(it) + events; eventId = it.id }
            .onFailure { saveError = "Couldn't add event: ${it.message}" }
    }

    fun createContact(name: String, brand: String, role: String) = viewModelScope.launch {
        runCatching { container.tastings.addContact(name, brand, role) }
            .onSuccess { contacts = (contacts + it).sortedBy { c -> c.name }; contactId = it.id }
            .onFailure { saveError = "Couldn't add contact: ${it.message}" }
    }

    fun save(onSaved: () -> Unit) {
        if (saving) return
        saving = true
        saveError = null
        viewModelScope.launch {
            val tasting = NewTasting(
                whiskyId = whiskyId,
                tastedAt = tastedAt.toString(),
                locationName = locationName.trim().ifBlank { null },
                eventId = eventId,
                brandContactId = contactId,
                rating = rating,
                wouldBuy = wouldBuy,
                personalNotes = notes.trim().ifBlank { null },
                flavours = flavours.filter { it.response != FlavourResponse.UNANSWERED }.map {
                    TastingFlavourInput(
                        termId = it.termId,
                        phase = it.phase.key,
                        response = it.response.key!!,
                        intensity = it.intensity,
                        wasSuggested = it.suggested,
                    )
                },
            )
            runCatching { container.tastings.save(tasting) }
                .onSuccess { onSaved() }
                .onFailure { saveError = "Couldn't save: ${it.message}" }
            saving = false
        }
    }
}
