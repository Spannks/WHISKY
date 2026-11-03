package app.whiskyjournal.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.whiskyjournal.AppContainer
import app.whiskyjournal.data.model.WhiskyHit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WhiskySearchViewModel(private val container: AppContainer) : ViewModel() {
    var query by mutableStateOf("")
        private set
    var results by mutableStateOf<List<WhiskyHit>>(emptyList())
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var job: Job? = null

    init { search("") }

    fun onQueryChange(q: String) {
        query = q
        search(q)
    }

    private fun search(q: String) {
        job?.cancel()
        job = viewModelScope.launch {
            delay(250) // debounce typing
            runCatching { container.catalogue.search(q) }
                .onSuccess { results = it; error = null }
                .onFailure { error = it.message }
        }
    }

    suspend fun addUnverified(name: String, distillery: String, age: String, abv: String): String =
        container.catalogue.addUnverifiedWhisky(
            name = name.trim(),
            distillery = distillery.trim().ifBlank { null },
            ageYears = age.trim().toIntOrNull(),
            abv = abv.trim().replace(',', '.').toDoubleOrNull(),
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiskySearchScreen(container: AppContainer, onBack: () -> Unit, onWhiskyChosen: (String) -> Unit) {
    val vm = viewModel { WhiskySearchViewModel(container) }
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Which whisky?") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = vm.query,
                onValueChange = vm::onQueryChange,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Name, distillery or age — e.g. lagavulin 16") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            vm.error?.let {
                Text("Search failed: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            LazyColumn(Modifier.weight(1f)) {
                items(vm.results, key = { it.id }) { hit ->
                    ListItem(
                        headlineContent = { Text(hit.name) },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                    hit.distillery,
                                    hit.region,
                                    hit.abv?.let { "$it%" },
                                    if (!hit.verified) "unverified" else null,
                                ).joinToString(" · ")
                            )
                        },
                        modifier = Modifier.clickable { onWhiskyChosen(hit.id) },
                    )
                    HorizontalDivider()
                }
                item {
                    TextButton(onClick = { showAdd = true }, modifier = Modifier.padding(8.dp)) {
                        Text("Can't find it? Add it")
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddWhiskyDialog(
            initialName = vm.query,
            onDismiss = { showAdd = false },
            onAdd = { name, distillery, age, abv -> vm.addUnverified(name, distillery, age, abv) },
            onAdded = { id -> showAdd = false; onWhiskyChosen(id) },
        )
    }
}

@Composable
private fun AddWhiskyDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onAdd: suspend (String, String, String, String) -> String,
    onAdded: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialName) }
    var distillery by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var abv by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a whisky") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name on the label") }, singleLine = true)
                OutlinedTextField(distillery, { distillery = it }, label = { Text("Distillery") }, singleLine = true)
                OutlinedTextField(
                    age, { age = it.filter(Char::isDigit) }, label = { Text("Age (years, optional)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    abv, { abv = it }, label = { Text("ABV % (optional)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text(
                    "It'll be marked unverified until it's checked against the catalogue.",
                    style = MaterialTheme.typography.bodySmall,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        runCatching { onAdd(name, distillery, age, abv) }
                            .onSuccess(onAdded)
                            .onFailure { error = it.message; busy = false }
                    }
                },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
