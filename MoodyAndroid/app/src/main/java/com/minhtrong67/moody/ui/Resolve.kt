package com.minhtrong67.moody.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import com.minhtrong67.moody.data.ActivitiesState
import com.minhtrong67.moody.data.ActivityDef
import com.minhtrong67.moody.data.MoodConfig
import com.minhtrong67.moody.data.MoodDef
import com.minhtrong67.moody.data.MoodOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

val LocalMoods = staticCompositionLocalOf<List<MoodDef>> { emptyList() }
val LocalActivities = staticCompositionLocalOf<List<ActivityDef>> { emptyList() }
val LocalLocale = compositionLocalOf { Locale.getDefault() }
val LocalToaster = staticCompositionLocalOf<Toaster> { error("Toaster not provided") }

/** Root-scoped snackbar helper (survives the screen that triggered it being closed). */
class Toaster(private val host: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(message: String) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            host.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
}

/** Default label comes from the string resources (current language); user overrides win. */
@Composable
fun resolveMoods(overrides: Map<String, MoodOverride>): List<MoodDef> =
    MoodConfig.MOODS.map { b ->
        val o = overrides[b.key]
        MoodDef(
            key = b.key,
            emoji = o?.emoji?.takeIf { it.isNotBlank() } ?: b.emoji,
            label = o?.label?.takeIf { it.isNotBlank() } ?: stringResource(b.labelRes),
            score = b.score,
            color = Color(b.color),
        )
    }

/** Defaults (minus hidden, with overrides) + custom, sorted by `order` — same as the web ActivitiesContext. */
@Composable
fun resolveActivities(state: ActivitiesState): List<ActivityDef> {
    val defaults = MoodConfig.DEFAULT_ACTIVITIES
        .filter { it.id !in state.hidden }
        .map { d ->
            val o = state.overrides[d.id]
            ActivityDef(
                id = d.id,
                emoji = o?.emoji?.takeIf { it.isNotBlank() } ?: d.emoji,
                label = o?.label?.takeIf { it.isNotBlank() } ?: stringResource(d.labelRes),
                custom = false,
            )
        }
    val merged = defaults + state.items.map { ActivityDef(it.id, it.emoji, it.label, true) }
    if (state.order.isEmpty()) return merged
    val byId = merged.associateBy { it.id }
    val ordered = state.order.mapNotNull { byId[it] }
    return ordered + merged.filter { it.id !in state.order }
}
