package app.whiskyjournal.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("over_18_confirmed_at") val over18ConfirmedAt: String? = null,
    val country: String? = null,
)

@Serializable
data class UserSettings(
    @SerialName("user_id") val userId: String,
    @SerialName("share_location_default") val shareLocationDefault: Boolean = false,
    val units: String = "ml",
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = false,
)

@Serializable
data class WhiskyHit(
    val id: String,
    val name: String,
    val distillery: String? = null,
    val region: String? = null,
    @SerialName("age_years") val ageYears: Int? = null,
    val abv: Double? = null,
    val style: String? = null,
    val verified: Boolean = true,
    val score: Float = 0f,
)

@Serializable
data class WhiskyDetail(
    val id: String,
    val name: String,
    @SerialName("age_years") val ageYears: Int? = null,
    val abv: Double? = null,
    val verified: Boolean = true,
    val distilleries: DistilleryRef? = null,
)

@Serializable
data class DistilleryRef(val name: String, val region: String? = null)

@Serializable
data class FlavourSuggestion(
    @SerialName("term_id") val termId: Int,
    val term: String,
    val category: String,
    val phase: String,
    val confidence: Float,
)

@Serializable
data class FlavourHit(
    @SerialName("term_id") val termId: Int,
    val term: String,
    val category: String,
    val score: Float = 0f,
)

@Serializable
data class Event(
    val id: String,
    val name: String,
    val venue: String? = null,
    @SerialName("start_date") val startDate: String? = null,
)

@Serializable
data class NewEvent(val name: String, val venue: String? = null)

@Serializable
data class BrandContact(
    val id: String,
    val name: String,
    @SerialName("brand_or_company") val brandOrCompany: String? = null,
    val role: String? = null,
    val notes: String? = null,
)

@Serializable
data class NewBrandContact(
    val name: String,
    @SerialName("brand_or_company") val brandOrCompany: String? = null,
    val role: String? = null,
)

@Serializable
data class TastingSummary(
    val id: String,
    @SerialName("tasted_at") val tastedAt: String,
    val rating: Int? = null,
    @SerialName("would_buy") val wouldBuy: Boolean? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("whisky_id") val whiskyId: String,
    @SerialName("whisky_name") val whiskyName: String,
    @SerialName("age_years") val ageYears: Int? = null,
    val distillery: String? = null,
    @SerialName("event_name") val eventName: String? = null,
    @SerialName("brand_contact_name") val brandContactName: String? = null,
    @SerialName("brand_or_company") val brandOrCompany: String? = null,
    @SerialName("confirmed_flavours") val confirmedFlavours: Long = 0,
)

enum class Phase(val key: String, val label: String) {
    NOSE("nose", "Nose"), PALATE("palate", "Palate"), FINISH("finish", "Finish");

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: PALATE
    }
}

enum class FlavourResponse(val key: String?) {
    UNANSWERED(null), CONFIRMED("confirmed"), REJECTED("rejected"), UNSURE("unsure");

    fun next(): FlavourResponse = entries[(ordinal + 1) % entries.size]
}

@Serializable
data class TastingFlavourInput(
    @SerialName("term_id") val termId: Int,
    val phase: String,
    val response: String,
    val intensity: Int? = null,
    @SerialName("was_suggested") val wasSuggested: Boolean = true,
)

@Serializable
data class NewTasting(
    @SerialName("whisky_id") val whiskyId: String,
    @SerialName("tasted_at") val tastedAt: String,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("event_id") val eventId: String? = null,
    @SerialName("brand_contact_id") val brandContactId: String? = null,
    val rating: Int? = null,
    @SerialName("would_buy") val wouldBuy: Boolean? = null,
    @SerialName("personal_notes") val personalNotes: String? = null,
    val flavours: List<TastingFlavourInput> = emptyList(),
)
