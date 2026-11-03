package app.whiskyjournal.ui.tastings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.whiskyjournal.AppContainer
import app.whiskyjournal.data.model.TastingSummary
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TastingsViewModel(private val container: AppContainer) : ViewModel() {
    var tastings by mutableStateOf<List<TastingSummary>?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun load() = viewModelScope.launch {
        runCatching { container.tastings.recent() }
            .onSuccess { tastings = it; error = null }
            .onFailure { error = it.message }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TastingsScreen(container: AppContainer, onAddTasting: () -> Unit, onOpenSettings: () -> Unit) {
    val vm = viewModel { TastingsViewModel(container) }
    LaunchedEffect(Unit) { vm.load() } // refresh each time we return here

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your tastings") },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTasting,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New tasting") },
            )
        },
    ) { padding ->
        val list = vm.tastings
        when {
            list == null && vm.error == null ->
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            vm.error != null && list == null ->
                Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), Alignment.Center) {
                    Text("Couldn't load tastings: ${vm.error}")
                }
            list.isNullOrEmpty() ->
                Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), Alignment.Center) {
                    Text(
                        "No tastings yet. Tap \"New tasting\" to record your first dram.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list.orEmpty(), key = { it.id }) { TastingCard(it) }
            }
        }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm")

@Composable
private fun TastingCard(t: TastingSummary) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.whiskyName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (t.wouldBuy == true) Icon(Icons.Default.ShoppingCart, "Would buy", tint = MaterialTheme.colorScheme.primary)
            }
            listOfNotNull(t.distillery, t.ageYears?.let { "$it yo" }).takeIf { it.isNotEmpty() }?.let {
                Text(it.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            val whenText = runCatching {
                dateFormat.format(Instant.parse(t.tastedAt).atZone(ZoneId.systemDefault()))
            }.getOrDefault(t.tastedAt)
            Text(
                listOfNotNull(whenText, t.locationName).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
            t.rating?.let { Text("★".repeat(it) + "☆".repeat(5 - it), color = MaterialTheme.colorScheme.primary) }
            if (t.eventName != null || t.brandContactName != null || t.confirmedFlavours > 0) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    t.eventName?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                    t.brandContactName?.let { name ->
                        AssistChip(onClick = {}, label = {
                            Text(listOfNotNull(name, t.brandOrCompany).joinToString(", "))
                        })
                    }
                    if (t.confirmedFlavours > 0) {
                        AssistChip(onClick = {}, label = { Text("${t.confirmedFlavours} flavours") })
                    }
                }
            }
        }
    }
}
