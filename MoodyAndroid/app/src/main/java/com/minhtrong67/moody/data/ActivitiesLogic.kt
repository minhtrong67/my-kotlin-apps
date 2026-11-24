package com.minhtrong67.moody.data

import java.util.UUID

/** Pure state transitions for the activities document (ported 1:1 from the web ActivitiesContext). */
object ActivitiesLogic {
    private fun isDefault(id: String) = id in MoodConfig.DEFAULT_ACTIVITY_IDS

    fun add(state: ActivitiesState, emoji: String, label: String, id: String = UUID.randomUUID().toString()): ActivitiesState =
        state.copy(items = state.items + CustomActivity(id, emoji, label))

    fun update(state: ActivitiesState, id: String, emoji: String, label: String): ActivitiesState =
        if (isDefault(id)) {
            state.copy(overrides = state.overrides + (id to ActivityOverride(emoji, label)))
        } else {
            state.copy(items = state.items.map { if (it.id == id) it.copy(emoji = emoji, label = label) else it })
        }

    fun remove(state: ActivitiesState, id: String): ActivitiesState {
        val order = state.order.filter { it != id }
        return if (isDefault(id)) state.copy(hidden = state.hidden + id, order = order)
        else state.copy(items = state.items.filter { it.id != id }, order = order)
    }

    fun reorder(state: ActivitiesState, idsInOrder: List<String>): ActivitiesState = state.copy(order = idsInOrder)

    class Imported(val id: String?, val isDefault: Boolean, val emoji: String, val label: String)

    /** Merge-only import. Returns the new state plus (added, updated). */
    fun import(state: ActivitiesState, items: List<Imported>): Triple<ActivitiesState, Int, Int> {
        val overrides = state.overrides.toMutableMap()
        val custom = state.items.toMutableList()
        var hidden = state.hidden
        val order = mutableListOf<String>()
        var added = 0
        var updated = 0
        for (it in items) {
            if (it.isDefault && it.id != null) {
                overrides[it.id] = ActivityOverride(it.emoji, it.label)
                hidden = hidden.filter { h -> h != it.id }
                order += it.id
                updated++
                continue
            }
            val idx = if (it.id != null) custom.indexOfFirst { c -> c.id == it.id } else -1
            if (idx >= 0) {
                custom[idx] = custom[idx].copy(emoji = it.emoji, label = it.label)
                order += it.id!!
                updated++
            } else {
                val id = it.id ?: UUID.randomUUID().toString()
                custom += CustomActivity(id, it.emoji, it.label)
                order += id
                added++
            }
        }
        val rest = state.order.filter { it !in order }
        return Triple(state.copy(overrides = overrides, items = custom, hidden = hidden, order = order + rest), added, updated)
    }
}
