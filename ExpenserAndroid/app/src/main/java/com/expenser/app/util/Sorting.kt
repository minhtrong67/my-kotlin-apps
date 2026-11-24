package com.expenser.app.util

import com.expenser.app.data.CafeDrink
import com.expenser.app.data.CafeShop
import com.expenser.app.data.Category
import java.text.Collator

/** Manual order first; never-ordered categories go after them, alphabetically (same as the web app). */
fun sortCategories(list: List<Category>): List<Category> {
    val collator = Collator.getInstance(Format.locale)
    return list.sortedWith { a, b ->
        val ao = a.order; val bo = b.order
        when {
            ao != null && bo != null && ao != bo -> ao.compareTo(bo)
            (ao != null) != (bo != null) -> if (ao != null) -1 else 1
            else -> collator.compare(a.name, b.name)
        }
    }
}

private fun <T> sortOrdered(list: List<T>, order: (T) -> Int?, created: (T) -> Long, name: (T) -> String): List<T> =
    list.sortedWith(compareBy<T>({ order(it) ?: Int.MAX_VALUE }, { created(it) }, { name(it) }))

fun sortShops(list: List<CafeShop>) = sortOrdered(list, { it.order }, { it.createdAt }, { it.name })
fun sortDrinks(list: List<CafeDrink>) = sortOrdered(list, { it.order }, { it.createdAt }, { it.name })
