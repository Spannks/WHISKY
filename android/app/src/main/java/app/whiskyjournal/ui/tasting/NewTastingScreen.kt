package app.whiskyjournal.ui.tasting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.whiskyjournal.AppContainer
import app.whiskyjournal.data.model.FlavourResponse
import app.whiskyjournal.data.model.Phase
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTastingScreen(container: AppContainer, whiskyId: String, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm = viewModel(key = "tasting-$whiskyId") { NewTastingViewModel(container, whiskyId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vm.whisky?.name ?: "New tasting") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        if (vm.whisky == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                vm.loadError?.let { Text("Couldn't load: $it") } ?: CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WhiskyHeader(vm)
            FlavourSection(vm)
            HorizontalDivider()
            ContextSection(vm)
            HorizontalDivider()
            VerdictSection(vm)

            vm.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { vm.save(onSaved) },
                enabled = !vm.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (vm.saving) "Saving…" else "Save tasting") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WhiskyHeader(vm: NewTastingViewModel) {
    val w = vm.whisky ?: return
    Text(
        listOfNotNull(
            w.distilleries?.name, w.distilleries?.region,
            w.ageYears?.let { "$it years" }, w.abv?.let { "$it%" },
            if (!w.verified) "unverified" else null,
        ).joinToString(" · "),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------------------
// Flavours: tap a chip to cycle  ○ not answered → ✓ yes → ✕ not for me → ? not sure
// ---------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlavourSection(vm: NewTastingViewModel) {
    Text("What can you taste?", style = MaterialTheme.typography.titleLarge)
    Text(
        if (vm.flavours.isEmpty()) "No tasting notes on file for this one yet — add what you find below."
        else "Tap each flavour: once for yes, twice for not for me, three times for not sure.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Phase.entries.forEach { phase ->
        val inPhase = vm.flavours.filter { it.phase == phase }
        if (inPhase.isNotEmpty()) {
            Text(phase.label, style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                inPhase.forEach { choice ->
                    val icon: Pair<ImageVector, String>? = when (choice.response) {
                        FlavourResponse.CONFIRMED -> Icons.Default.Check to "Yes"
                        FlavourResponse.REJECTED -> Icons.Default.Close to "Not for me"
                        FlavourResponse.UNSURE -> Icons.Default.QuestionMark to "Not sure"
                        FlavourResponse.UNANSWERED -> null
                    }
                    FilterChip(
                        selected = choice.response != FlavourResponse.UNANSWERED,
                        onClick = { vm.cycle(choice) },
                        label = { Text(choice.term) },
                        leadingIcon = if (icon != null) {
                            { Icon(icon.first, icon.second) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (choice.response) {
                                FlavourResponse.REJECTED -> MaterialTheme.colorScheme.errorContainer
                                FlavourResponse.UNSURE -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                        ),
                    )
                }
            }
        }
    }

    // Strength of each confirmed flavour
    val confirmed = vm.flavours.filter { it.response == FlavourResponse.CONFIRMED }
    if (confirmed.isNotEmpty()) {
        Text("How strong?", style = MaterialTheme.typography.titleSmall)
        confirmed.forEach { choice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${choice.term} (${choice.phase.label.lowercase()})", Modifier.weight(1f))
                listOf(1 to "Light", 2 to "Medium", 3 to "Strong").forEach { (level, label) ->
                    FilterChip(
                        selected = choice.intensity == level,
                        onClick = { vm.setIntensity(choice, level) },
                        label = { Text(label) },
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }

    // Add a flavour that wasn't suggested
    Text("Something else?", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Phase.entries.forEach { p ->
            FilterChip(selected = vm.addPhase == p, onClick = { vm.addPhase = p }, label = { Text(p.label) })
        }
    }
    OutlinedTextField(
        value = vm.flavourQuery,
        onValueChange = vm::onFlavourQuery,
        placeholder = { Text("e.g. toffee, smoke, orange") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (vm.flavourHits.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.flavourHits.forEach { hit ->
                SuggestionChip(onClick = { vm.addFlavour(hit) }, label = { Text("${hit.term} · ${hit.category}") })
            }
        }
    }
}

// ---------------------------------------------------------------------------
// When, where, event, brand contact
// ---------------------------------------------------------------------------
private val whenFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContextSection(vm: NewTastingViewModel) {
    var showDate by remember { mutableStateOf(false) }
    var showNewEvent by remember { mutableStateOf(false) }
    var showNewContact by remember { mutableStateOf(false) }

    Text("Where and when", style = MaterialTheme.typography.titleLarge)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(whenFormat.format(vm.tastedAt.atZone(ZoneId.systemDefault())), Modifier.weight(1f))
        TextButton(onClick = { showDate = true }) { Text("Change date") }
    }

    OutlinedTextField(
        value = vm.locationName,
        onValueChange = { vm.locationName = it },
        label = { Text("Where (bar, home, distillery…)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Picker(
        label = "Event",
        noneLabel = "Not at an event",
        options = vm.events.map { it.id to listOfNotNull(it.name, it.venue).joinToString(" · ") },
        selectedId = vm.eventId,
        onSelect = { vm.eventId = it },
        newLabel = "New event…",
        onNew = { showNewEvent = true },
    )

    Picker(
        label = "Brand contact",
        noneLabel = "Didn't speak to anyone",
        options = vm.contacts.map { c -> c.id to listOfNotNull(c.name, c.brandOrCompany, c.role).joinToString(" · ") },
        selectedId = vm.contactId,
        onSelect = { vm.contactId = it },
        newLabel = "New contact…",
        onNew = { showNewContact = true },
    )

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = vm.tastedAt.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(vm::setDate)
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
    if (showNewEvent) {
        TwoOrThreeFieldDialog(
            title = "New event",
            labels = listOf("Event name (e.g. Whisky Live Edinburgh)", "Venue (optional)"),
            onDismiss = { showNewEvent = false },
            onSave = { v -> vm.createEvent(v[0], v[1]); showNewEvent = false },
        )
    }
    if (showNewContact) {
        TwoOrThreeFieldDialog(
            title = "Who did you speak to?",
            labels = listOf("Name", "Brand or company", "Role (e.g. brand ambassador)"),
            onDismiss = { showNewContact = false },
            onSave = { v -> vm.createContact(v[0], v[1], v[2]); showNewContact = false },
            footnote = "Kept private to your account.",
        )
    }
}

@Composable
private fun Picker(
    label: String,
    noneLabel: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    newLabel: String,
    onNew: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(options.firstOrNull { it.first == selectedId }?.second ?: noneLabel, Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text(noneLabel) }, onClick = { onSelect(null); open = false })
                options.forEach { (id, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(id); open = false })
                }
                HorizontalDivider()
                DropdownMenuItem(text = { Text(newLabel) }, onClick = { open = false; onNew() })
            }
        }
    }
}

@Composable
private fun TwoOrThreeFieldDialog(
    title: String,
    labels: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
    footnote: String? = null,
) {
    val values = remember { labels.map { mutableStateOf("") } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { i, l ->
                    OutlinedTextField(values[i].value, { values[i].value = it }, label = { Text(l) }, singleLine = true)
                }
                footnote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(enabled = values[0].value.isNotBlank(), onClick = { onSave(values.map { it.value }) }) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------------------
// Rating, would buy, notes
// ---------------------------------------------------------------------------
@Composable
private fun VerdictSection(vm: NewTastingViewModel) {
    Text("Your verdict", style = MaterialTheme.typography.titleLarge)
    Row {
        (1..5).forEach { star ->
            IconButton(onClick = { vm.rating = if (vm.rating == star) null else star }) {
                Icon(
                    if ((vm.rating ?: 0) >= star) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "$star stars",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("I'd buy a bottle", Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Switch(checked = vm.wouldBuy, onCheckedChange = { vm.wouldBuy = it })
    }
    OutlinedTextField(
        value = vm.notes,
        onValueChange = { vm.notes = it },
        label = { Text("Notes (optional)") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}
