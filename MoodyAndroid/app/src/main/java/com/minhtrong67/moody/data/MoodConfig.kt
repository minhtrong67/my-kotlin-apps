package com.minhtrong67.moody.data

import androidx.annotation.StringRes
import com.minhtrong67.moody.R

/** The 5 fixed mood levels (same keys / scores / colors as the web app). */
object MoodConfig {
    class Base(val key: String, val emoji: String, val score: Int, val color: Long, @StringRes val labelRes: Int)

    val MOODS = listOf(
        Base("great", "😄", 5, 0xFFF0A34E, R.string.mood_great),
        Base("good", "🙂", 4, 0xFF8FBF7F, R.string.mood_good),
        Base("okay", "😐", 3, 0xFF9FB8C8, R.string.mood_okay),
        Base("sad", "😔", 2, 0xFF7D8CB0, R.string.mood_sad),
        Base("angry", "😠", 1, 0xFFC4676B, R.string.mood_angry),
    )

    class DefaultActivity(val id: String, val emoji: String, @StringRes val labelRes: Int)

    val DEFAULT_ACTIVITIES = listOf(
        DefaultActivity("sleep", "😴", R.string.activity_sleep),
        DefaultActivity("work", "💼", R.string.activity_work),
        DefaultActivity("study", "📚", R.string.activity_study),
        DefaultActivity("health", "🩺", R.string.activity_health),
        DefaultActivity("social", "👥", R.string.activity_social),
        DefaultActivity("family", "👨‍👩‍👧", R.string.activity_family),
        DefaultActivity("weather", "🌤️", R.string.activity_weather),
        DefaultActivity("money", "💰", R.string.activity_money),
        DefaultActivity("exercise", "🏃", R.string.activity_exercise),
        DefaultActivity("food", "🍜", R.string.activity_food),
    )

    val DEFAULT_ACTIVITY_IDS: List<String> = DEFAULT_ACTIVITIES.map { it.id }
    val MOOD_KEYS: List<String> = MOODS.map { it.key }

    /** Chart palette for the activity pie (same as web `CATEGORY_COLORS`). */
    val CATEGORY_LIGHT = longArrayOf(0xFF2A78D6, 0xFFEB6834, 0xFF1BAF7A, 0xFFEDA100, 0xFFE87BA4, 0xFF008300, 0xFF6250D6, 0xFFE34948)
    val CATEGORY_DARK = longArrayOf(0xFF3987E5, 0xFFD95926, 0xFF199E70, 0xFFC98500, 0xFFD55181, 0xFF008300, 0xFF9085E9, 0xFFE66767)
}
