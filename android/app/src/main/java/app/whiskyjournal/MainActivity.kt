package app.whiskyjournal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.whiskyjournal.ui.AppRoot
import app.whiskyjournal.ui.theme.WhiskyTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    private val container get() = (application as WhiskyApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container.supabase.handleDeeplinks(intent)
        enableEdgeToEdge()
        setContent {
            WhiskyTheme { AppRoot(container) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        container.supabase.handleDeeplinks(intent)
    }
}
