package app.whiskyjournal

import android.app.Application
import app.whiskyjournal.data.AuthRepository
import app.whiskyjournal.data.CatalogueRepository
import app.whiskyjournal.data.ProfileRepository
import app.whiskyjournal.data.TastingRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest

class WhiskyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}

/** Simple manual dependency container; swap for Hilt once the app grows. */
class AppContainer {
    val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth) {
            // Magic-link redirect: whiskyjournal://login-callback (also add it in Supabase → Auth → URL configuration)
            scheme = "whiskyjournal"
            host = "login-callback"
        }
        install(Postgrest)
        install(Functions)
    }

    val auth = AuthRepository(supabase)
    val profiles = ProfileRepository(supabase)
    val catalogue = CatalogueRepository(supabase)
    val tastings = TastingRepository(supabase)
}
