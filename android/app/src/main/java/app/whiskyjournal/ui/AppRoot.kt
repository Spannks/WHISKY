package app.whiskyjournal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.whiskyjournal.AppContainer
import app.whiskyjournal.data.model.Profile
import app.whiskyjournal.ui.auth.AgeGateScreen
import app.whiskyjournal.ui.auth.SignInScreen
import app.whiskyjournal.ui.search.WhiskySearchScreen
import app.whiskyjournal.ui.settings.SettingsScreen
import app.whiskyjournal.ui.tasting.NewTastingScreen
import app.whiskyjournal.ui.tastings.TastingsScreen
import io.github.jan.supabase.auth.status.SessionStatus

/** Top-level gate: signed out → sign in; signed in without age confirmation → age gate; otherwise the app. */
@Composable
fun AppRoot(container: AppContainer) {
    val status by container.auth.sessionStatus.collectAsStateWithLifecycle()
    when (status) {
        is SessionStatus.Initializing -> Loading()
        is SessionStatus.Authenticated -> SignedIn(container)
        else -> SignInScreen(container.auth)
    }
}

@Composable
private fun SignedIn(container: AppContainer) {
    var reloadKey by remember { mutableIntStateOf(0) }
    val profile by produceState<Result<Profile?>?>(initialValue = null, reloadKey) {
        value = runCatching { container.profiles.profile() }
    }
    val p = profile
    when {
        p == null -> Loading()
        p.getOrNull()?.over18ConfirmedAt == null ->
            AgeGateScreen(container, onConfirmed = { reloadKey++ }, loadError = p.exceptionOrNull())
        else -> MainNav(container)
    }
}

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val TASTING = "tasting/{whiskyId}"
    fun tasting(whiskyId: String) = "tasting/$whiskyId"
}

@Composable
private fun MainNav(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            TastingsScreen(
                container = container,
                onAddTasting = { nav.navigate(Routes.SEARCH) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SEARCH) {
            WhiskySearchScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onWhiskyChosen = { id -> nav.navigate(Routes.tasting(id)) },
            )
        }
        composable(
            Routes.TASTING,
            arguments = listOf(navArgument("whiskyId") { type = NavType.StringType }),
        ) { entry ->
            NewTastingScreen(
                container = container,
                whiskyId = entry.arguments?.getString("whiskyId").orEmpty(),
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(container = container, onBack = { nav.popBackStack() })
        }
    }
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}
