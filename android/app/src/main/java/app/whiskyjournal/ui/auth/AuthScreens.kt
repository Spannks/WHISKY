package app.whiskyjournal.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import app.whiskyjournal.AppContainer
import app.whiskyjournal.data.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(auth: AuthRepository) {
    val context = LocalContext.current // Activity context, needed by Credential Manager
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Whisky Journal", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Record what you taste and learn what you like.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))

        Button(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = {
                busy = true; message = null
                scope.launch {
                    try {
                        auth.signInWithGoogle(context)
                    } catch (_: GetCredentialCancellationException) {
                        // user backed out of the picker
                    } catch (_: NoCredentialException) {
                        message = "No Google account on this device. Use the email link instead."
                    } catch (e: Exception) {
                        message = "Sign-in failed: ${e.message}"
                    } finally {
                        busy = false
                    }
                }
            },
        ) { Text("Continue with Google") }

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Text("  or  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            enabled = !busy && email.contains('@'),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                busy = true; message = null
                scope.launch {
                    message = runCatching { auth.sendMagicLink(email) }.fold(
                        onSuccess = { "Check your email for a sign-in link, then open it on this phone." },
                        onFailure = { "Couldn't send the link: ${it.message}" },
                    )
                    busy = false
                }
            },
        ) { Text("Email me a sign-in link") }

        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun AgeGateScreen(container: AppContainer, onConfirmed: () -> Unit, loadError: Throwable?) {
    val scope = rememberCoroutineScope()
    var checked by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(loadError?.let { "Couldn't load your profile: ${it.message}" }) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Before you start", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "Whisky Journal is for adults of legal drinking age. In the UK that's 18 or over. " +
                "Please drink responsibly.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { checked = it })
            Text("I confirm I'm 18 or over")
        }
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = checked && !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                busy = true
                scope.launch {
                    runCatching { container.profiles.confirmAge() }
                        .onSuccess { onConfirmed() }
                        .onFailure { error = "Couldn't save: ${it.message}" }
                    busy = false
                }
            },
        ) { Text("Continue") }
        TextButton(onClick = { scope.launch { runCatching { container.auth.signOut() } } }) {
            Text("Sign out")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
