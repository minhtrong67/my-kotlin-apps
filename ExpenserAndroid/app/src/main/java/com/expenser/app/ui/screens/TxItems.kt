package com.expenser.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expenser.app.R
import com.expenser.app.data.Category
import com.expenser.app.data.Tx
import com.expenser.app.data.TxType
import com.expenser.app.data.Wallet
import com.expenser.app.ui.components.CategoryAvatar
import com.expenser.app.ui.components.IconAvatar
import com.expenser.app.ui.theme.LocalIncomeColors
import com.expenser.app.util.Format
import com.expenser.app.util.toColor

/** Title shown for a transaction: category name (or "Transfer"), plus the note. */
@Composable
fun txTitle(tx: Tx, category: Category?): String {
    val base = if (tx.type == TxType.TRANSFER) stringResource(R.string.tx_transfer) else category?.name ?: stringResource(R.string.common_other)
    return if (tx.note.isNotBlank()) "$base · ${tx.note}" else base
}

@Composable
fun amountColor(type: String): Color = when (type) {
    TxType.INCOME -> LocalIncomeColors.current.income
    TxType.EXPENSE -> com.expenser.app.ui.theme.LocalIncomeColors.current.expense
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

fun signedMoney(tx: Tx): String = when (tx.type) {
    TxType.INCOME -> "+" + Format.money(tx.amount)
    TxType.EXPENSE -> "-" + Format.money(tx.amount)
    else -> Format.money(tx.amount)
}

@Composable
fun TxRow(
    tx: Tx,
    category: Category?,
    wallets: Map<String, Wallet>,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tx.type == TxType.TRANSFER) IconAvatar(Icons.Rounded.SwapHoriz, MaterialTheme.colorScheme.onSurfaceVariant)
        else CategoryAvatar(category?.emoji ?: "🏷️", category?.color?.toColor() ?: Color(0xFF94A3B8))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(txTitle(tx, category), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val walletName = if (tx.type == TxType.TRANSFER) "${wallets[tx.fromWalletId ?: ""]?.name ?: "?"} → ${wallets[tx.toWalletId ?: ""]?.name ?: "?"}" else wallets[tx.walletId ?: ""]?.name
            val sub = buildString {
                if (showDate) append(Format.dateShort(tx.date)).append(" · ")
                append(Format.time(tx.date))
                if (walletName != null) append(" · ").append(walletName)
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(signedMoney(tx), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = amountColor(tx.type))
    }
}
