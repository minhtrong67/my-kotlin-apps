package com.expenser.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.expenser.app.R
import com.expenser.app.util.ImageUploadException
import com.expenser.app.util.ImageUploader
import kotlinx.coroutines.launch

/**
 * Photo field: pick an image from the device, upload it to imgbb and report the resulting URL through [onChange].
 * [onBusyChange] lets the parent form block "Save" while an upload is running.
 */
@Composable
fun ImageUploadField(
    value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: String = stringResource(R.string.image_optional), onBusyChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val errNoKey = stringResource(R.string.image_err_nokey)
    val errFailed = stringResource(R.string.image_err_failed)
    val errLarge = stringResource(R.string.image_err_large)
    val errInvalid = stringResource(R.string.image_err_invalid)
    LaunchedEffect(uploading) { onBusyChange(uploading) }
    DisposableEffect(Unit) { onDispose { onBusyChange(false) } }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        picked = uri; error = null; uploading = true
        scope.launch {
            try {
                onChange(ImageUploader.upload(context, uri))
            } catch (e: ImageUploadException) {
                picked = null
                error = when (e.code) { "noKey" -> errNoKey; "tooLarge" -> errLarge; "notImage" -> errInvalid; else -> errFailed }
            } catch (e: Exception) { picked = null; error = errFailed }
            uploading = false
        }
    }
    fun pick() { if (!uploading) launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val has = value.isNotBlank() || picked != null
        if (!has) {
            OutlinedCard(
                onClick = ::pick, shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().height(120.dp),
            ) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.image_pick), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (picked != null && (uploading || value.isBlank())) {
                    AsyncImage(picked, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    RemoteImage(value, Modifier.fillMaxSize())
                }
                if (uploading) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.image_uploading), color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallRoundButton(Icons.Rounded.Edit, stringResource(R.string.image_change)) { pick() }
                        SmallRoundButton(Icons.Rounded.Close, stringResource(R.string.image_remove)) { picked = null; onChange("") }
                    }
                }
            }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun SmallRoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.Black.copy(alpha = 0.55f), contentColor = Color.White, modifier = Modifier.size(34.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, description, Modifier.size(18.dp)) }
    }
}

class PickItem(val id: String, val title: String, val subtitle: String? = null, val imageUrl: String = "")

/** Bottom sheet for picking a shop / drink: photos, search, and a list <-> grid toggle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaChoiceSheet(
    title: String, items: List<PickItem>, selectedId: String?, fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onSelect: (String) -> Unit, onDismiss: () -> Unit,
) {
    var grid by rememberBoolPref("pick_sheet_grid", false)
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(items, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) items else items.filter { it.title.lowercase().contains(q) || (it.subtitle ?: "").lowercase().contains(q) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        Row(Modifier.padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(query, { query = it }, stringResource(R.string.pick_search), Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            ViewToggleButton(
                if (grid) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                stringResource(if (grid) R.string.view_list else R.string.view_grid), { grid = !grid },
            )
        }
        Crossfade(grid, animationSpec = tween(220), label = "pickView") { g ->
            if (g) {
                LazyVerticalGrid(
                    GridCells.Fixed(2), Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    gridItems(shown, key = { it.id }) { it2 ->
                        val sel = it2.id == selectedId
                        Column(
                            Modifier.clip(RoundedCornerShape(18.dp)).background(if (sel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable { onSelect(it2.id); onDismiss() },
                        ) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                                RemoteImage(it2.imageUrl, Modifier.fillMaxSize(), fallbackIcon)
                                if (sel) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(26.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Text(it2.title, Modifier.padding(10.dp), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shown, key = { it.id }) { it2 ->
                        val sel = it2.id == selectedId
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (sel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable { onSelect(it2.id); onDismiss() }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RemoteImage(it2.imageUrl, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)), fallbackIcon)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(it2.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                if (!it2.subtitle.isNullOrBlank()) Text(it2.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                            if (sel) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
