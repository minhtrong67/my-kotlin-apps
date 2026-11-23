package com.minhtrong67.moody.data.remote

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.minhtrong67.moody.data.ActivitiesState
import com.minhtrong67.moody.data.ActivityOverride
import com.minhtrong67.moody.data.CustomActivity
import com.minhtrong67.moody.data.Entry
import com.minhtrong67.moody.data.EntryData
import com.minhtrong67.moody.data.MoodOverride
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firestore access. The document layout is identical to the web app, so the same Google account
 * sees the same data on both:
 *   users/{uid}/entries/{id}          -> date, time, mood, score, activities[], note
 *   users/{uid}/meta/moods            -> { overrides: { <moodKey>: { emoji, label } } }
 *   users/{uid}/meta/activities       -> { items[], order[], overrides{}, hidden[] }
 *
 * Writes are deliberately NOT awaited: Firestore applies them to its local cache immediately and the
 * snapshot listeners fire, so the UI stays responsive offline (an awaited write would hang until the
 * server acknowledges it).
 */
class FirestoreStore {
    private val db get() = FirebaseFirestore.getInstance()

    private fun entries(uid: String) = db.collection("users").document(uid).collection("entries")
    private fun meta(uid: String, name: String): DocumentReference =
        db.collection("users").document(uid).collection("meta").document(name)

    private fun logFail(what: String): (Exception) -> Unit = { Log.w("Moody", "Firestore $what failed", it) }

    // ---------------- Entries ----------------

    fun observeEntries(uid: String): Flow<List<Entry>> = callbackFlow {
        val reg = entries(uid).orderBy("date", Query.Direction.DESCENDING).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            if (snap != null) trySend(snap.documents.mapNotNull { it.toEntry() })
        }
        awaitClose { reg.remove() }
    }

    fun addEntry(uid: String, data: EntryData) {
        entries(uid).add(data.toMap()).addOnFailureListener(logFail("add entry"))
    }

    fun updateEntry(uid: String, id: String, data: EntryData) {
        entries(uid).document(id).set(data.toMap(), SetOptions.merge()).addOnFailureListener(logFail("update entry"))
    }

    fun deleteEntry(uid: String, id: String) {
        entries(uid).document(id).delete().addOnFailureListener(logFail("delete entry"))
    }

    /** Batch upsert (id -> data), 400 per batch (Firestore limit is 500). */
    fun importEntries(uid: String, items: List<Pair<String, EntryData>>) {
        items.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (id, data) -> batch.set(entries(uid).document(id), data.toMap(), SetOptions.merge()) }
            batch.commit().addOnFailureListener(logFail("import entries"))
        }
    }

    // ---------------- Mood overrides ----------------

    fun observeMoodOverrides(uid: String): Flow<Map<String, MoodOverride>> = callbackFlow {
        val reg = meta(uid, "moods").addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val raw = snap?.get("overrides") as? Map<*, *>
            val out = raw?.entries?.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val m = v as? Map<*, *> ?: return@mapNotNull null
                key to MoodOverride(m["emoji"] as? String, m["label"] as? String)
            }?.toMap() ?: emptyMap()
            trySend(out)
        }
        awaitClose { reg.remove() }
    }

    fun setMoodOverrides(uid: String, overrides: Map<String, MoodOverride>) {
        val map = overrides.mapValues { (_, o) ->
            buildMap<String, Any> {
                o.emoji?.let { put("emoji", it) }
                o.label?.let { put("label", it) }
            }
        }
        meta(uid, "moods").set(mapOf("overrides" to map)).addOnFailureListener(logFail("set moods"))
    }

    // ---------------- Activities ----------------

    fun observeActivities(uid: String): Flow<ActivitiesState> = callbackFlow {
        val reg = meta(uid, "activities").addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            trySend(if (snap != null && snap.exists()) snap.toActivitiesState() else ActivitiesState())
        }
        awaitClose { reg.remove() }
    }

    fun setActivities(uid: String, state: ActivitiesState) {
        val data = mapOf(
            "items" to state.items.map { mapOf("id" to it.id, "custom" to true, "emoji" to it.emoji, "label" to it.label) },
            "order" to state.order,
            "overrides" to state.overrides.mapValues { (_, o) ->
                buildMap<String, Any> {
                    o.emoji?.let { put("emoji", it) }
                    o.label?.let { put("label", it) }
                }
            },
            "hidden" to state.hidden,
        )
        meta(uid, "activities").set(data).addOnFailureListener(logFail("set activities"))
    }
}

private fun EntryData.toMap(): Map<String, Any> = mapOf(
    "date" to date,
    "time" to time,
    "mood" to mood,
    "score" to score,
    "activities" to activities,
    "note" to note,
)

private fun DocumentSnapshot.toEntry(): Entry? {
    val date = getString("date") ?: return null
    val mood = getString("mood") ?: return null
    val acts = (get("activities") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    return Entry(
        id = id,
        date = date,
        time = getString("time") ?: "${date}T12:00:00Z",
        mood = mood,
        score = (getLong("score") ?: 3L).toInt(),
        activities = acts,
        note = getString("note") ?: "",
    )
}

private fun DocumentSnapshot.toActivitiesState(): ActivitiesState {
    val items = (get("items") as? List<*>)?.mapNotNull { raw ->
        val m = raw as? Map<*, *> ?: return@mapNotNull null
        val id = m["id"] as? String ?: return@mapNotNull null
        CustomActivity(id = id, emoji = m["emoji"] as? String ?: "🙂", label = m["label"] as? String ?: "", custom = true)
    } ?: emptyList()
    val order = (get("order") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    val hidden = (get("hidden") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    val overrides = (get("overrides") as? Map<*, *>)?.entries?.mapNotNull { (k, v) ->
        val key = k as? String ?: return@mapNotNull null
        val m = v as? Map<*, *> ?: return@mapNotNull null
        key to ActivityOverride(m["emoji"] as? String, m["label"] as? String)
    }?.toMap() ?: emptyMap()
    return ActivitiesState(items, order, overrides, hidden)
}
