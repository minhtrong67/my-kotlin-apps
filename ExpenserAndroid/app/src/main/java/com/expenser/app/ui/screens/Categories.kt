package com.expenser.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenser.app.R
import com.expenser.app.data.Category
import com.expenser.app.data.TxType
import com.expenser.app.ui.MainViewModel
import com.expenser.app.ui.components.*
import com.expenser.app.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(vm: MainViewModel) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    var search by rememberSaveable { mutableStateOf("") }
    var editor by remember { mutableStateOf<CategoryEditorState?>(null) }
    var deleteTarget by remember { mutableStateOf<Category?>(null) }

    val usage = remember(transactions) { transactions.groupingBy { it.categoryId }.eachCount() }
    val all = categories.filter { it.type == type }
    val list = remember(all, search) { val q = normalizeText(search); if (q.isEmpty()) all else all.filter { normalizeText(it.name).contains(q) } }

    val listState = rememberLazyListState()
    val reorder = rememberReorder(list.map { it.id }, listState) { ids -> vm.reorderCategories(ids) }
    val byId = list.associateBy { it.id }
    val shown = reorder.display(list.map { it.id }).mapNotNull { byId[it] }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 104.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                PillSegmented(
                    listOf(
                        Seg(TxType.EXPENSE, stringResource(R.string.cat_expense_tab), container = MaterialTheme.colorScheme.errorContainer, content = MaterialTheme.colorScheme.onErrorContainer),
                        Seg(TxType.INCOME, stringResource(R.string.cat_income_tab), container = com.expenser.app.ui.theme.LocalIncomeColors.current.container, content = com.expenser.app.ui.theme.LocalIncomeColors.current.onContainer),
                    ), type, { type = it },
                )
            }
            item { SearchField(search, { search = it }, stringResource(R.string.cat_search)) }
            if (list.isNotEmpty()) item { SwipeHint() }
            if (list.isEmpty() && loading) item { ListSkeleton(rows = 6) }
            else if (list.isEmpty()) item { EmptyState(Icons.Rounded.Category, if (search.isBlank()) stringResource(R.string.cat_empty) else stringResource(R.string.cat_no_match, search)) }
            items(shown, key = { it.id }) { c ->
                SwipeRow(onEdit = { editor = CategoryEditorState(c, c.type) }, onDelete = { deleteTarget = c }, modifier = reorderable(reorder, c.id)) {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            CategoryAvatar(c.emoji, c.color.toColor())
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.cat_usage, usage[c.id] ?: 0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (search.isBlank()) DragHandle(reorder, c.id)
                        }
                    }
                }
            }
        }
        ScrollToTop(listState)
        ScreenFab({ editor = CategoryEditorState(null, type) }, Modifier.align(Alignment.BottomEnd))
    }

    editor?.let { st ->
        CategoryEditor(st, categories, onDismiss = { editor = null }, onSave = { name, emoji, color ->
            if (st.editing == null) vm.addCategory(name, st.type, emoji, color) { editor = null }
            else vm.updateCategory(st.editing, name, emoji, color) { editor = null }
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

    FormScreen(
        stringResource(if (editing == null) R.string.cat_add_title else R.string.cat_edit_title), stringResource(R.string.common_save),
        onClose = onDismiss, onConfirm = { submitted = true; if (err == null) onSave(trimmed, emoji, color) },
    ) {
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.cat_name)) }, singleLine = true, isError = submitted && err != null,
            supportingText = { Text(if (submitted && err != null) err else stringResource(R.string.cat_name_hint)) }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.cat_emoji), style = MaterialTheme.typography.titleSmall)
        EmojiPicker(emoji) { emoji = it }
        Text(stringResource(R.string.common_color), style = MaterialTheme.typography.titleSmall)
        ColorPicker(color, { color = it }, CATEGORY_COLORS)
    }
}
