package com.minhtrong67.moody.transfer

import androidx.compose.ui.graphics.toArgb
import com.minhtrong67.moody.data.ActivitiesLogic
import com.minhtrong67.moody.data.ActivityDef
import com.minhtrong67.moody.data.Entry
import com.minhtrong67.moody.data.EntryData
import com.minhtrong67.moody.data.ImportItem
import com.minhtrong67.moody.data.MoodConfig
import com.minhtrong67.moody.data.MoodDef
import com.minhtrong67.moody.data.MoodOverride
import com.minhtrong67.moody.data.parseInstant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

typealias Row = Map<String, Any?>

/** Same file formats as the web app (CSV / JSON, plus a .zip of CSVs for "everything"), so files are interchangeable. */
object DataTransfer {
    enum class Dataset(val key: String, val headers: List<String>) {
        Entries("entries", listOf("id", "date", "time", "mood", "score", "note", "activities", "activity_labels")),
        Activities("activities", listOf("id", "emoji", "label", "custom")),
        Moods("moods", listOf("key", "emoji", "label", "score", "color"));

        companion object {
            fun of(key: String?) = entries.firstOrNull { it.key == key }
        }
    }

    const val MAX_IMPORT_BYTES = 10 * 1024 * 1024
    private const val LIST_SEP = ";"

    class ImportException(val code: String) : Exception(code) // format | size | invalid | read | mismatch

    // ---------------- app data -> flat rows ----------------

    fun entriesToRows(entries: List<Entry>, activityById: Map<String, ActivityDef>): List<Row> = entries.map { e ->
        linkedMapOf<String, Any?>(
            "id" to e.id,
            "date" to e.date,
            "time" to e.time,
            "mood" to e.mood,
            "score" to e.score,
            "note" to e.note,
            "activities" to e.activities.joinToString(LIST_SEP),
            "activity_labels" to e.activities.joinToString(LIST_SEP) { activityById[it]?.label ?: it },
        )
    }

    fun activitiesToRows(list: List<ActivityDef>): List<Row> = list.map {
        linkedMapOf<String, Any?>("id" to it.id, "emoji" to it.emoji, "label" to it.label, "custom" to it.custom)
    }

    fun moodsToRows(list: List<MoodDef>): List<Row> = list.map {
        linkedMapOf<String, Any?>(
            "key" to it.key, "emoji" to it.emoji, "label" to it.label, "score" to it.score,
            "color" to "#%06X".format(it.color.toArgb() and 0xFFFFFF),
        )
    }

    // ---------------- CSV (injection-safe, Excel-friendly BOM) ----------------

    private val FORMULA_START = Regex("^[=+\\-@\\t\\r]")
    private fun escapeCell(v: String) = if (FORMULA_START.containsMatchIn(v)) "'$v" else v
    private fun unescapeCell(v: String) = if (Regex("^'[=+\\-@\\t\\r]").containsMatchIn(v)) v.substring(1) else v

    fun toCsv(headers: List<String>, rows: List<Row>): String {
        fun q(v: Any?): String {
            val s = if (v == null) "" else escapeCell(v.toString())
            return if (s.any { it == '"' || it == ',' || it == '\r' || it == '\n' || it == ';' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
        }
        val lines = listOf(headers.joinToString(",")) + rows.map { r -> headers.joinToString(",") { q(r[it]) } }
        return "\uFEFF" + lines.joinToString("\r\n")
    }

    fun parseCsv(text: String): List<List<String>> {
        val src = text.removePrefix("\uFEFF")
        val firstLine = src.lineSequence().firstOrNull() ?: ""
        val delim = if (!firstLine.contains(',') && firstLine.contains(';')) ';' else ','
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < src.length) {
            val c = src[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < src.length && src[i + 1] == '"') { cell.append('"'); i++ } else inQuotes = false
                } else cell.append(c)
            } else when {
                c == '"' -> inQuotes = true
                c == delim -> { row.add(cell.toString()); cell.clear() }
                c == '\n' || c == '\r' -> {
                    if (c == '\r' && i + 1 < src.length && src[i + 1] == '\n') i++
                    row.add(cell.toString()); cell.clear()
                    rows.add(row); row = ArrayList()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        return rows
    }

    private fun tableToObjects(table: List<List<String>>): List<Row> {
        val nonEmpty = table.filter { r -> r.any { it.isNotBlank() } }
        if (nonEmpty.size < 2) return emptyList()
        val headers = nonEmpty[0].map { it.trim().lowercase() }
        return nonEmpty.drop(1).map { r ->
            val o = LinkedHashMap<String, Any?>()
            headers.forEachIndexed { i, h -> if (h.isNotEmpty()) o[h] = unescapeCell(r.getOrElse(i) { "" }) }
            o
        }
    }

    // ---------------- export ----------------

    private val prettyJson = Json { prettyPrint = true }

    private fun rowToJson(headers: List<String>, r: Row): JsonObject = buildJsonObject {
        for (h in headers) when (val v = r[h]) {
            null -> put(h, "")
            is Number -> put(h, v)
            is Boolean -> put(h, v)
            else -> put(h, v.toString())
        }
    }

    private fun rowsJson(ds: Dataset, rows: List<Row>): JsonArray = buildJsonArray { rows.forEach { add(rowToJson(ds.headers, it)) } }

    fun buildJson(ds: Dataset, rows: List<Row>): ByteArray {
        val payload = buildJsonObject {
            put("app", "moody"); put("dataset", ds.key); put("version", 1)
            put("exportedAt", Instant.now().toString())
            put("data", rowsJson(ds, rows))
        }
        return prettyJson.encodeToString(JsonElement.serializer(), payload).toByteArray()
    }

    fun buildAllJson(all: Map<Dataset, List<Row>>): ByteArray {
        val payload = buildJsonObject {
            put("app", "moody"); put("dataset", "all"); put("version", 1)
            put("exportedAt", Instant.now().toString())
            put("data", buildJsonObject { Dataset.entries.forEach { ds -> put(ds.key, rowsJson(ds, all[ds] ?: emptyList())) } })
        }
        return prettyJson.encodeToString(JsonElement.serializer(), payload).toByteArray()
    }

    fun buildCsv(ds: Dataset, rows: List<Row>): ByteArray = toCsv(ds.headers, rows).toByteArray(Charsets.UTF_8)

    fun buildAllZip(all: Map<Dataset, List<Row>>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            Dataset.entries.forEach { ds ->
                z.putNextEntry(ZipEntry("${ds.key}.csv"))
                z.write(buildCsv(ds, all[ds] ?: emptyList()))
                z.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    // ---------------- import: parse ----------------

    /** A lone CSV doesn't say what it contains: recognise it by its column names. */
    private fun detectDataset(rows: List<Row>): Dataset? {
        val keys = rows.firstOrNull()?.keys ?: return null
        return when {
            "mood" in keys && "date" in keys -> Dataset.Entries
            "key" in keys && ("emoji" in keys || "label" in keys) -> Dataset.Moods
            "label" in keys -> Dataset.Activities
            else -> null
        }
    }

    private fun jsonToRow(o: JsonObject): Row = o.entries.associate { (k, v) ->
        k to when (v) {
            is JsonNull -> ""
            is JsonPrimitive -> v.content
            is JsonArray -> v.map { (it as? JsonPrimitive)?.content ?: "" }
            else -> ""
        }
    }

    private fun cleanList(e: JsonElement?): List<Row>? =
        (e as? JsonArray)?.mapNotNull { (it as? JsonObject)?.let(::jsonToRow) }

    /**
     * Reads a backup (.json / .zip of CSVs / a single .csv) into raw rows per dataset. A lone CSV or a bare JSON array is
     * recognised by its column names, so a file exported by any part of the app (or the web app) can be imported.
     */
    fun parseImport(fileName: String, bytes: ByteArray): Map<Dataset, List<Row>> {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext !in listOf("csv", "json", "zip")) throw ImportException("format")
        if (bytes.size > MAX_IMPORT_BYTES) throw ImportException("size")
        val out = LinkedHashMap<Dataset, List<Row>>()
        try {
            when (ext) {
                "json" -> {
                    val json = Json.parseToJsonElement(bytes.toString(Charsets.UTF_8))
                    if (json is JsonArray) cleanList(json)?.let { out[detectDataset(it) ?: Dataset.Entries] = it }
                    else if (json is JsonObject) {
                        val dsName = (json["dataset"] as? JsonPrimitive)?.content
                        val data = json["data"]
                        if (dsName == "all" && data is JsonObject) {
                            Dataset.entries.forEach { ds -> cleanList(data[ds.key])?.let { out[ds] = it } }
                        } else if (Dataset.of(dsName) != null) {
                            cleanList(data)?.let { out[Dataset.of(dsName)!!] = it }
                        } else cleanList(data)?.let { out[detectDataset(it) ?: Dataset.Entries] = it }
                    }
                }
                "csv" -> {
                    val rows = tableToObjects(parseCsv(bytes.toString(Charsets.UTF_8)))
                    val ds = detectDataset(rows) ?: throw ImportException("invalid")
                    out[ds] = rows
                }
                else -> ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
                    var e = z.nextEntry
                    while (e != null) {
                        val name = e.name.substringAfterLast('/')
                        val base = name.removeSuffix(".csv").removeSuffix(".CSV").lowercase()
                        if (name.endsWith(".csv", ignoreCase = true)) {
                            Dataset.of(base)?.let { ds ->
                                val data = z.readBytes()
                                if (data.size <= MAX_IMPORT_BYTES) out[ds] = tableToObjects(parseCsv(data.toString(Charsets.UTF_8)))
                            }
                        }
                        e = z.nextEntry
                    }
                }
            }
        } catch (e: ImportException) {
            throw e
        } catch (e: Exception) {
            throw ImportException("read")
        }
        if (out.isEmpty()) throw ImportException("invalid")
        return out
    }

    // ---------------- import: validate + normalize ----------------

    private fun str(v: Any?): String = when (v) { null -> ""; else -> v.toString().trim() }

    private fun splitList(v: Any?): List<String> = when (v) {
        is List<*> -> v.map { str(it) }.filter { it.isNotEmpty() }
        else -> str(v).split(';', '|').map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun toIsoDate(v: Any?): String? {
        val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})").find(str(v)) ?: return null
        return runCatching { LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()).toString() }.getOrNull()
    }

    private fun toIsoTime(v: Any?, date: String): String {
        val s = str(v)
        if (s.isNotEmpty()) {
            parseInstant(s)?.let { return it.toString() }
            runCatching { return LocalDateTime.parse(s).atZone(ZoneId.systemDefault()).toInstant().toString() }
        }
        return LocalDate.parse(date).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toString()
    }

    class Normalized<T>(val items: List<T>, val skipped: Int)

    fun normalizeEntries(raw: List<Row>, moods: List<MoodDef>, activities: List<ActivityDef>): Normalized<ImportItem> {
        val moodByKey = moods.associateBy { it.key }
        val actById = activities.associateBy { it.id }
        val actByLabel = activities.associateBy { it.label.lowercase() }
        val items = ArrayList<ImportItem>()
        var skipped = 0
        for (r in raw) {
            val mood = str(r["mood"]).lowercase()
            val date = toIsoDate(r["date"]) ?: toIsoDate(r["time"])
            val def = moodByKey[mood]
            if (def == null || date == null) { skipped++; continue }
            val refs = splitList(r["activities"]).ifEmpty { splitList(r["activity_labels"]) }
            val ids = ArrayList<String>()
            for (ref in refs) {
                val a = actById[ref] ?: actByLabel[ref.lowercase()]
                if (a != null && a.id !in ids) ids += a.id
            }
            items += ImportItem(
                id = str(r["id"]).ifEmpty { null },
                data = EntryData(date, toIsoTime(r["time"], date), mood, def.score, ids, str(r["note"]).take(2000)),
            )
        }
        return Normalized(items, skipped)
    }

    fun normalizeActivities(raw: List<Row>): Normalized<ActivitiesLogic.Imported> {
        val items = ArrayList<ActivitiesLogic.Imported>()
        var skipped = 0
        for (r in raw) {
            val label = str(r["label"])
            if (label.isEmpty()) { skipped++; continue }
            val id = str(r["id"])
            items += ActivitiesLogic.Imported(
                id = id.ifEmpty { null },
                isDefault = id in MoodConfig.DEFAULT_ACTIVITY_IDS,
                emoji = str(r["emoji"]).ifEmpty { "🙂" },
                label = label.take(60),
            )
        }
        return Normalized(items, skipped)
    }

    fun normalizeMoods(raw: List<Row>): Normalized<Pair<String, MoodOverride>> {
        val items = ArrayList<Pair<String, MoodOverride>>()
        var skipped = 0
        for (r in raw) {
            val key = str(r["key"]).lowercase()
            val label = str(r["label"])
            val emoji = str(r["emoji"])
            if (key !in MoodConfig.MOOD_KEYS || (label.isEmpty() && emoji.isEmpty())) { skipped++; continue }
            items += key to MoodOverride(emoji.ifEmpty { null }, label.take(60).ifEmpty { null })
        }
        return Normalized(items, skipped)
    }
}
