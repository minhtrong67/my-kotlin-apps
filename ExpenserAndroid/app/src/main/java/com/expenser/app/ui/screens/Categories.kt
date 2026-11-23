package com.expenser.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenser.app.R
import com.expenser.app.data.Category
import com.expenser.app.data.TxType
import com.expenser.app.ui.MainViewModel
import com.expenser.app.ui.components.*
import com.expenser.app.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var editor by remember { mutableStateOf<CategoryEditorState?>(null) }
    var deleteTarget by remember { mutableStateOf<Category?>(null) }

    val type = if (tab == 0) TxType.EXPENSE else TxType.INCOME
    val usage = remember(transactions) { transactions.groupingBy { it.categoryId }.eachCount() }
    val all = categories.filter { it.type == type }
    val list = remember(all, search) { val q = normalizeText(search); if (q.isEmpty()) all else all.filter { normalizeText(it.name).contains(q) } }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.nav_categories), onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { editor = CategoryEditorState(null, type) }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(stringResource(R.string.cat_add)) })
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text(stringResource(R.string.cat_expense_tab)) })
                Tab(tab == 1, { tab = 1 }, text = { Text(stringResource(R.string.cat_income_tab)) })
            }
            SearchField(search, { search = it }, stringResource(R.string.cat_search), Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
            if (list.isEmpty()) {
                EmptyState(Icons.Rounded.Category, if (search.isBlank()) stringResource(R.string.cat_empty) else stringResource(R.string.cat_no_match, search))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    itemsIndexed(list, key = { _, c -> c.id }) { index, c ->
                        ListItem(
                            leadingContent = { CategoryAvatar(c.emoji, c.color.toColor()) },
                            headlineContent = { Text(c.name) },
                            supportingContent = { Text(stringResource(R.string.cat_usage, usage[c.id] ?: 0)) },
                            trailingContent = {
                                Row {
                                    if (search.isBlank()) {
                                        IconButton(enabled = index > 0, onClick = { vm.reorderCategories(all.map { it.id }.toMutableList().apply { add(index - 1, removeAt(index)) }) }) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.move_up)) }
                                        IconButton(enabled = index < all.lastIndex, onClick = { vm.reorderCategories(all.map { it.id }.toMutableList().apply { add(index + 1, removeAt(index)) }) }) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.move_down)) }
                                    }
                                    IconButton(onClick = { editor = CategoryEditorState(c, c.type) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.common_edit)) }
                                    IconButton(onClick = { deleteTarget = c }) { Icon(Icons.Rounded.Delete, stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.error) }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    editor?.let { st ->
        CategoryEditor(st, categories, onDismiss = { editor = null }, onSave = { name, emoji, color ->
            if (st.editing == null) vm.addCategory(name, st.type, emoji, color) { editor = null }
            else vm.updateCategory(st.editing.copy(name = name, emoji = emoji, color = color)) { editor = null }
        })
    }
    deleteTarget?.let { c ->
        ConfirmDialog(stringResource(R.string.cat_confirm_delete, c.name), onConfirm = { vm.deleteCategory(c) }, onDismiss = { deleteTarget = null })
    }
}

data class CategoryEditorState(val editing: Category?, val type: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditor(state: CategoryEditorState, all: List<Category>, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    val editing = state.editing
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var emoji by remember { mutableStateOf(editing?.emoji ?: "🏷️") }
    var color by remember { mutableStateOf(editing?.color ?: CATEGORY_COLORS.last()) }
    var submitted by remember { mutableStateOf(false) }

    val trimmed = name.trim()
    val err = when {
        trimmed.isEmpty() -> stringResource(R.string.validation_category_name_required)
        trimmed.length > 40 -> stringResource(R.string.validation_category_name_max)
        all.any { it.type == state.type && it.id != editing?.id && it.name.equals(trimmed, ignoreCase = true) } -> stringResource(R.string.validation_category_exists)
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (editing == null) R.string.cat_add_title else R.string.cat_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.cat_name)) }, singleLine = true, isError = submitted && err != null,
                    supportingText = { Text(if (submitted && err != null) err else stringResource(R.string.cat_name_hint)) }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.cat_emoji), style = MaterialTheme.typography.titleSmall)
                EmojiPicker(emoji) { emoji = it }
                Text(stringResource(R.string.common_color), style = MaterialTheme.typography.titleSmall)
                ColorPicker(color, { color = it }, CATEGORY_COLORS)
            }
        },
        confirmButton = { TextButton(onClick = { submitted = true; if (err == null) onSave(trimmed, emoji, color) }) { Text(stringResource(R.string.common_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
