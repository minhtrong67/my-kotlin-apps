package com.expenser.app.util

import androidx.compose.ui.graphics.Color

data class DefaultCategory(val vi: String, val en: String, val emoji: String, val color: String)
data class DefaultWallet(val vi: String, val en: String, val icon: String, val color: String)

val DEFAULT_EXPENSE_CATEGORIES = listOf(
    DefaultCategory("Ăn uống", "Food & drink", "🍔", "#F97316"),
    DefaultCategory("Di chuyển", "Transport", "🚗", "#3B82F6"),
    DefaultCategory("Mua sắm", "Shopping", "🛍️", "#EC4899"),
    DefaultCategory("Giải trí", "Entertainment", "🎮", "#8B5CF6"),
    DefaultCategory("Học tập", "Education", "📚", "#06B6D4"),
    DefaultCategory("Hóa đơn", "Bills", "🧾", "#EF4444"),
    DefaultCategory("Sức khỏe", "Health", "❤️‍🩹", "#10B981"),
    DefaultCategory("Nhà ở", "Housing", "🏠", "#F59E0B"),
    DefaultCategory("Khác", "Other", "📦", "#64748B"),
)

val DEFAULT_INCOME_CATEGORIES = listOf(
    DefaultCategory("Lương", "Salary", "💰", "#10B981"),
    DefaultCategory("Thưởng", "Bonus", "🎁", "#F59E0B"),
    DefaultCategory("Làm thêm", "Freelance", "💻", "#3B82F6"),
    DefaultCategory("Đầu tư", "Investment", "📈", "#8B5CF6"),
    DefaultCategory("Quà tặng", "Gifts", "🎀", "#EC4899"),
    DefaultCategory("Khác", "Other", "📦", "#64748B"),
)

val DEFAULT_WALLETS = listOf(
    DefaultWallet("Tiền mặt", "Cash", "payments", "#10B981"),
    DefaultWallet("Ngân hàng", "Bank", "account_balance", "#3B82F6"),
    DefaultWallet("Ví điện tử", "E-wallet", "qr_code_2", "#8B5CF6"),
)

val CATEGORY_COLORS = listOf(
    "#EF4444", "#F97316", "#F59E0B", "#EAB308", "#84CC16", "#10B981",
    "#06B6D4", "#3B82F6", "#6366F1", "#8B5CF6", "#D946EF", "#EC4899", "#64748B",
)

val WALLET_ICON_KEYS = listOf(
    "payments", "account_balance", "qr_code_2", "credit_card", "savings",
    "account_balance_wallet", "currency_exchange", "diamond", "smartphone", "attach_money",
    "account_box", "real_estate_agent", "trending_up", "card_giftcard", "business_center",
)

/** Emoji groups for the category picker: (string-resource name suffix, emojis). */
val EMOJI_GROUPS: List<Pair<String, List<String>>> = listOf(
    "food" to listOf("🍔", "🍕", "🍜", "🍣", "🍱", "🍰", "☕", "🧋", "🍺", "🍎", "🥗", "🍳"),
    "transport" to listOf("🚗", "🚕", "🚌", "🚆", "✈️", "🛵", "🚲", "⛽", "🅿️", "🚢", "🚀", "🚦"),
    "shopping" to listOf("🛍️", "🛒", "👗", "👟", "💄", "💍", "🎁", "🏬", "👜", "⌚", "🕶️", "🧴"),
    "entertainment" to listOf("🎮", "🎬", "🎵", "⚽", "🏀", "🎉", "🎲", "📸", "🎨", "🎳", "🎤", "🧩"),
    "work" to listOf("📚", "✏️", "💻", "🎓", "🔬", "🧮", "🖨️", "📝", "💼", "📈", "🧑‍💻", "📎"),
    "utilities" to listOf("🧾", "💡", "💧", "📶", "📱", "📺", "🔌", "🧺", "🛢️", "📡", "🔋", "🧯"),
    "health" to listOf("❤️‍🩹", "🏥", "💪", "🧘", "💊", "🧠", "🩺", "💉", "👓", "🦷", "🧴", "🏃"),
    "home" to listOf("🏠", "🏢", "🧒", "🐶", "🪑", "🌳", "🧹", "🔧", "👶", "👨‍👩‍👧", "🛏️", "🐱"),
    "finance" to listOf("💰", "🏦", "📈", "💵", "🎁", "💱", "💳", "🎀", "🏡", "🤝", "📊", "🪙"),
    "other" to listOf("🏷️", "📦", "🚩", "⚓", "🌐", "❓", "⭐", "🔥", "✨", "🧊", "🎯", "🗂️"),
)

fun String.toColor(fallback: Color = Color(0xFF64748B)): Color =
    try { Color(android.graphics.Color.parseColor(this)) } catch (_: Exception) { fallback }
