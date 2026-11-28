@file:OptIn(ExperimentalMaterial3Api::class)

package com.minhtrong67.moody.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.AppUser
import com.minhtrong67.moody.data.Entry
import com.minhtrong67.moody.ui.LocalActivities
import com.minhtrong67.moody.ui.LocalLocale
import com.minhtrong67.moody.ui.LocalMoods
import com.minhtrong67.moody.ui.components.ActionButton
import com.minhtrong67.moody.ui.components.MoodBubble
import com.minhtrong67.moody.ui.components.MoodyCard
import com.minhtrong67.moody.ui.components.SectionTitle
import com.minhtrong67.moody.ui.components.StaggerIn
import com.minhtrong67.moody.ui.components.UserAvatar
import com.minhtrong67.moody.util.calcStreak
import com.minhtrong67.moody.util.daysOfWeek
import com.minhtrong67.moody.util.latestByDate
import com.minhtrong67.moody.util.relativeDateLabel
import com.minhtrong67.moody.util.today
import java.time.LocalTime
import java.util.Locale

@Composable
fun TodayScreen(
    entries: List<Entry>?,
    user: AppUser?,
    onCheckIn: (id: String?) -> Unit,
    onViewAll: () -> Unit,
    onAvatar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val moods = LocalMoods.current
    val activities = LocalActivities.current
    val locale = LocalLocale.current
    val ctx = LocalContext.current
    val moodByKey = remember(moods) { moods.associateBy { it.key } }
    val activityById = remember(activities) { activities.associateBy { it.id } }
    val cs = MaterialTheme.colorScheme

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        val hour = remember { LocalTime.now().hour }
        val greeting = stringResource(if (hour < 11) R.string.dash_morning else if (hour < 18) R.string.dash_afternoon else R.string.dash_evening)
        val firstName = user?.displayName?.trim()?.split(" ")?.lastOrNull()?.let { ", $it" } ?: ""

        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(greeting + firstName, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.dash_question1) + "\n" + stringResource(R.string.dash_question2), style = MaterialTheme.typography.headlineMedium)
            }
            if (user != null) UserAvatar(user, 40.dp, Modifier.clickable(onClick = onAvatar))
        }
        Spacer(Modifier.height(20.dp))

        if (entries == null) {
            com.minhtrong67.moody.ui.components.TodaySkeleton()
            return@Column
        }

        val byDate = remember(entries) { latestByDate(entries) }
        val todayIso = today().toString()
        val todayEntry = remember(entries) { entries.filter { it.date == todayIso }.maxByOrNull { it.instant } }

        // Today card
        StaggerIn(0) {
            MoodyCard(Modifier.fillMaxWidth(), containerColor = cs.primaryContainer, contentPadding = 20.dp) {
                if (todayEntry != null) {
                    val m = moodByKey[todayEntry.mood]
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(cs.surface.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                            Text(m?.emoji ?: "", fontSize = 30.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.dash_checkedIn, m?.label ?: ""), style = MaterialTheme.typography.titleMedium, color = cs.onPrimaryContainer)
                            if (todayEntry.note.isNotBlank()) {
                                Text(todayEntry.note, style = MaterialTheme.typography.bodyMedium, color = cs.onPrimaryContainer.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IconButton(onClick = { onCheckIn(todayEntry.id) }) {
                            Icon(Icons.Rounded.Edit, stringResource(R.string.dash_editCheckin), tint = cs.onPrimaryContainer)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(cs.surface.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.EditNote, null, tint = cs.onPrimaryContainer)
                        }
                        Column {
                            Text(stringResource(R.string.dash_notYet), style = MaterialTheme.typography.titleMedium, color = cs.onPrimaryContainer)
                            Text(stringResource(R.string.dash_fewSeconds), style = MaterialTheme.typography.bodyMedium, color = cs.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // tonal-on-container button, like the web "Check in now →"
                    androidx.compose.material3.Button(
                        onClick = { onCheckIn(null) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                    ) {
                        Text(stringResource(R.string.dash_checkinNow))
                        Spacer(Modifier.size(8.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Stat tiles
        val week = remember { daysOfWeek() }
        val weekAvg = remember(byDate) {
            val scores = week.mapNotNull { byDate[it.toString()]?.score }
            if (scores.isEmpty()) "—" else "%.1f/5".format(Locale.US, scores.average())
        }
        val streak = remember(byDate) { calcStreak(byDate) }
        StaggerIn(1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(Icons.Rounded.LocalFireDepartment, stringResource(R.string.dash_streak), streak.toString(), Color(0xFFF0A34E), Modifier.weight(1f))
                StatTile(Icons.Rounded.Insights, stringResource(R.string.dash_weekAvg), weekAvg, Color(0xFF7D8CB0), Modifier.weight(1f))
                StatTile(Icons.Rounded.EventAvailable, stringResource(R.string.dash_total), byDate.size.toString(), Color(0xFF8FBF7F), Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(28.dp))

        // Week strip
        StaggerIn(2) {
            Column {
                SectionTitle(stringResource(R.string.dash_thisWeek))
                val labels = stringArrayResource(R.array.weekdays_short)
                MoodyCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        week.forEachIndexed { i, d ->
                            val m = byDate[d.toString()]?.let { moodByKey[it.mood] }
                            val isToday = d == today()
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape)
                                        .background(m?.color?.copy(alpha = 0.25f) ?: cs.surfaceContainer)
                                        .then(if (isToday) Modifier.border(2.dp, cs.primary, CircleShape) else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) { Text(m?.emoji ?: "", fontSize = 18.sp) }
                                Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelMedium, color = if (isToday) cs.primary else cs.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))

        // Recent
        val recent = remember(byDate) { byDate.values.sortedByDescending { it.date }.take(3) }
        if (recent.isNotEmpty()) {
            StaggerIn(3) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.dash_recent), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = onViewAll) { Text(stringResource(R.string.dash_viewAll)) }
                    }
                    Spacer(Modifier.height(4.dp))
                    recent.forEach { e ->
                        val m = moodByKey[e.mood]
                        MoodyCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), onClick = { onCheckIn(e.id) }, contentPadding = 12.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                MoodBubble(m, 40.dp, 20)
                                Column(Modifier.weight(1f)) {
                                    Text(m?.label ?: "", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val sub = relativeDateLabel(ctx, e.date, locale) + when {
                                        e.note.isNotBlank() -> " · " + e.note
                                        e.activities.isNotEmpty() -> " · " + e.activities.mapNotNull { activityById[it]?.emoji }.joinToString(" ")
                                        else -> ""
                                    }
                                    Text(sub, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = cs.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(96.dp)) // room for the FAB
    }
}

@Composable
private fun StatTile(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    MoodyCard(modifier, contentPadding = 12.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
