package com.minhtrong67.moody.data

import com.minhtrong67.moody.data.auth.AuthManager
import com.minhtrong67.moody.data.local.EntryDao
import com.minhtrong67.moody.data.local.SettingsStore
import com.minhtrong67.moody.data.local.toEntity
import com.minhtrong67.moody.data.remote.FirestoreStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** One imported check-in: `id` may be null (then matched by date+time or created). */
class ImportItem(val id: String?, val data: EntryData)

/**
 * Single source of truth. Guest mode -> Room + DataStore on the device.
 * Signed in -> Firestore (same layout as the web app, with offline cache).
 * Every flow emits `null` first while (re)loading, like the web `loading` flags.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataRepository(
    private val dao: EntryDao,
    private val settings: SettingsStore,
    private val remote: FirestoreStore?,
    private val auth: AuthManager,
) {
    private val uidFlow: Flow<String?> = auth.user.map { it?.uid }.distinctUntilChanged()
    private fun currentUid(): String? = if (remote == null) null else auth.user.value?.uid

    private fun localEntries(): Flow<List<Entry>> = dao.observeAll().map { list -> list.map { it.toEntry() } }

    fun entries(): Flow<List<Entry>?> = uidFlow.flatMapLatest { uid ->
        flow<List<Entry>?> {
            emit(null)
            if (uid == null || remote == null) emitAll(localEntries())
            else emitAll(remote.observeEntries(uid).catch { emitAll(localEntries()) })
        }
    }

    fun activitiesState(): Flow<ActivitiesState?> = uidFlow.flatMapLatest { uid ->
        flow<ActivitiesState?> {
            emit(null)
            if (uid == null || remote == null) emitAll(settings.localActivities)
            else emitAll(remote.observeActivities(uid).catch { emitAll(settings.localActivities) })
        }
    }

    fun moodOverrides(): Flow<Map<String, MoodOverride>?> = uidFlow.flatMapLatest { uid ->
        flow<Map<String, MoodOverride>?> {
            emit(null)
            if (uid == null || remote == null) emitAll(settings.localMoods)
            else emitAll(remote.observeMoodOverrides(uid).catch { emitAll(settings.localMoods) })
        }
    }

    // ---------------- Entries ----------------

    suspend fun addEntry(data: EntryData) {
        val uid = currentUid()
        if (uid == null) dao.upsert(data.toEntity(UUID.randomUUID().toString()))
        else remote!!.addEntry(uid, data)
    }

    suspend fun updateEntry(id: String, data: EntryData) {
        val uid = currentUid()
        if (uid == null) dao.upsert(data.toEntity(id)) else remote!!.updateEntry(uid, id, data)
    }

    suspend fun deleteEntry(id: String) {
        val uid = currentUid()
        if (uid == null) dao.delete(id) else remote!!.deleteEntry(uid, id)
    }

    /** Same merge rules as the web app: match by id, else by date+time, else create. Returns (added, updated). */
    suspend fun importEntries(items: List<ImportItem>): Pair<Int, Int> {
        val uid = currentUid()
        val current: List<Entry> =
            if (uid == null) dao.getAll().map { it.toEntry() } else entries().filterNotNull().first()
        val byId = current.associateBy { it.id }
        val byTime = current.associateBy { "${it.date}|${it.time}" }
        var added = 0
        var updated = 0
        val writes = LinkedHashMap<String, EntryData>()
        for (item in items) {
            val match = (item.id?.let { byId[it] }) ?: byTime["${item.data.date}|${item.data.time}"]
            val finalId = match?.id ?: item.id ?: UUID.randomUUID().toString()
            if (!writes.containsKey(finalId)) { if (match != null) updated++ else added++ }
            writes[finalId] = item.data
        }
        if (uid == null) dao.upsertAll(writes.map { (id, d) -> d.toEntity(id) })
        else remote!!.importEntries(uid, writes.toList())
        return added to updated
    }

    // ---------------- Meta ----------------

    suspend fun saveActivities(state: ActivitiesState) {
        val uid = currentUid()
        if (uid == null) settings.setLocalActivities(state) else remote!!.setActivities(uid, state)
    }

    suspend fun saveMoodOverrides(map: Map<String, MoodOverride>) {
        val uid = currentUid()
        if (uid == null) settings.setLocalMoods(map) else remote!!.setMoodOverrides(uid, map)
    }
}
