package app.whiskyjournal.data

import android.content.Context
import app.whiskyjournal.BuildConfig
import app.whiskyjournal.data.model.BrandContact
import app.whiskyjournal.data.model.Event
import app.whiskyjournal.data.model.FlavourHit
import app.whiskyjournal.data.model.FlavourSuggestion
import app.whiskyjournal.data.model.NewBrandContact
import app.whiskyjournal.data.model.NewEvent
import app.whiskyjournal.data.model.NewTasting
import app.whiskyjournal.data.model.Profile
import app.whiskyjournal.data.model.TastingSummary
import app.whiskyjournal.data.model.UserSettings
import app.whiskyjournal.data.model.WhiskyDetail
import app.whiskyjournal.data.model.WhiskyHit
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

private val json = Json { encodeDefaults = true; explicitNulls = true }

class AuthRepository(private val supabase: SupabaseClient) {

    val sessionStatus get() = supabase.auth.sessionStatus

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    /**
     * Google sign-in via Credential Manager. The hashed nonce goes to Google; the raw nonce goes to
     * Supabase, which checks they match — this stops a replayed ID token being accepted.
     * [activityContext] must be an Activity context so the account picker can be shown.
     */
    suspend fun signInWithGoogle(activityContext: Context) {
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = MessageDigest.getInstance("SHA-256")
            .digest(rawNonce.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(hashedNonce)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activityContext)
            .getCredential(activityContext, request)
            .credential

        require(credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type"
        }
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken

        supabase.auth.signInWith(IDToken) {
            idToken = googleIdToken
            provider = Google
            nonce = rawNonce
        }
    }

    /** Fallback: emails a magic link that opens whiskyjournal://login-callback. */
    suspend fun sendMagicLink(email: String) {
        supabase.auth.signInWith(OTP) { this.email = email.trim() }
    }

    suspend fun signOut() = supabase.auth.signOut()

    /** Calls the delete-account Edge Function; all user rows cascade from auth.users. */
    suspend fun deleteAccount() {
        supabase.functions.invoke("delete-account")
        runCatching { supabase.auth.signOut() }
    }
}

class ProfileRepository(private val supabase: SupabaseClient) {

    private fun uid() = supabase.auth.currentUserOrNull()?.id ?: error("Not signed in")

    suspend fun profile(): Profile? =
        supabase.from("profiles").select { filter { eq("id", uid()) } }.decodeSingleOrNull()

    suspend fun confirmAge() {
        supabase.from("profiles").update(
            buildJsonObject { put("over_18_confirmed_at", Instant.now().toString()) }
        ) { filter { eq("id", uid()) } }
    }

    suspend fun updateDisplayName(name: String) {
        supabase.from("profiles").update(
            buildJsonObject { put("display_name", name.trim()) }
        ) { filter { eq("id", uid()) } }
    }

    suspend fun settings(): UserSettings? =
        supabase.from("user_settings").select { filter { eq("user_id", uid()) } }.decodeSingleOrNull()

    suspend fun updateUnits(units: String) {
        supabase.from("user_settings").update(
            buildJsonObject { put("units", units) }
        ) { filter { eq("user_id", uid()) } }
    }
}

class CatalogueRepository(private val supabase: SupabaseClient) {

    suspend fun search(query: String): List<WhiskyHit> =
        supabase.postgrest.rpc("search_whiskies", buildJsonObject { put("q", query) }).decodeList()

    suspend fun whisky(id: String): WhiskyDetail =
        supabase.from("whiskies")
            .select(Columns.raw("id, name, age_years, abv, verified, distilleries(name, region)")) {
                filter { eq("id", id) }
            }
            .decodeSingle()

    suspend fun flavourSuggestions(whiskyId: String): List<FlavourSuggestion> =
        supabase.postgrest.rpc("flavour_suggestions", buildJsonObject { put("p_whisky_id", whiskyId) })
            .decodeList()

    suspend fun searchFlavours(query: String): List<FlavourHit> =
        supabase.postgrest.rpc("search_flavours", buildJsonObject { put("q", query) }).decodeList()

    /** "None of these" path — creates an unverified whisky and returns its id. */
    suspend fun addUnverifiedWhisky(name: String, distillery: String?, ageYears: Int?, abv: Double?): String =
        supabase.postgrest.rpc("add_unverified_whisky", buildJsonObject {
            put("p_name", name)
            put("p_distillery", distillery)
            put("p_age_years", ageYears)
            put("p_abv", abv)
        }).decodeAs()
}

class TastingRepository(private val supabase: SupabaseClient) {

    suspend fun recent(): List<TastingSummary> =
        supabase.from("tasting_summaries").select {
            order("tasted_at", Order.DESCENDING)
            limit(200)
        }.decodeList()

    suspend fun save(tasting: NewTasting): String =
        supabase.postgrest.rpc("save_tasting", buildJsonObject {
            put("payload", json.encodeToJsonElement(tasting))
        }).decodeAs()

    suspend fun events(): List<Event> =
        supabase.from("events").select { order("created_at", Order.DESCENDING) }.decodeList()

    suspend fun addEvent(name: String, venue: String?): Event =
        supabase.from("events").insert(NewEvent(name.trim(), venue?.trim()?.ifBlank { null })) { select() }
            .decodeSingle()

    suspend fun contacts(): List<BrandContact> =
        supabase.from("brand_contacts").select { order("name", Order.ASCENDING) }.decodeList()

    suspend fun addContact(name: String, brand: String?, role: String?): BrandContact =
        supabase.from("brand_contacts")
            .insert(NewBrandContact(name.trim(), brand?.trim()?.ifBlank { null }, role?.trim()?.ifBlank { null })) {
                select()
            }
            .decodeSingle()
}
