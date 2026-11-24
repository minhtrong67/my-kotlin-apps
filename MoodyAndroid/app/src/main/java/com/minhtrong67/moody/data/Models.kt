package com.minhtrong67.moody.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime

/** Check-in payload without an id (what gets written to Room / Firestore). */
data class EntryData(
    val date: String,          // yyyy-MM-dd (device local date)
    val time: String,          // ISO-8601 instant, e.g. 2026-10-10T13:00:00Z
    val mood: String,          // great | good | okay | sad | angry
    val score: Int,
    val activities: List<String> = emptyList(),
    val note: String = "",
)

data class Entry(
    val id: String,
    val date: String,
    val time: String,
    val mood: String,
    val score: Int,
    val activities: List<String> = emptyList(),
    val note: String = "",
) {
    /** Parsed once at construction (it is used by every sort / group-by). */
    val instant: Instant = parseInstant(time) ?: Instant.EPOCH
    fun data() = EntryData(date, time, mood, score, activities, note)
}

fun EntryData.toEntry(id: String) = Entry(id, date, time, mood, score, activities, note)

fun parseInstant(value: String): Instant? =
    runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()

@Serializable
data class MoodOverride(val emoji: String? = null, val label: String? = null)

@Serializable
data class ActivityOverride(val emoji: String? = null, val label: String? = null)

@Serializable
data class CustomActivity(
    val id: String,
    val emoji: String,
    val label: String,
    val custom: Boolean = true,
)

/** Same shape as the web app's `users/{uid}/meta/activities` document. */
@Serializable
data class ActivitiesState(
    val items: List<CustomActivity> = emptyList(),
    val order: List<String> = emptyList(),
    val overrides: Map<String, ActivityOverride> = emptyMap(),
    val hidden: List<String> = emptyList(),
)

/** Fully resolved (localized + overridden) mood level used by the UI. */
data class MoodDef(
    val key: String,
    val emoji: String,
    val label: String,
    val score: Int,
    val color: Color,
)

data class ActivityDef(
    val id: String,
    val emoji: String,
    val label: String,
    val custom: Boolean,
)

data class AppUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
)
