package com.gnort67.calendarpro.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.gnort67.calendarpro.core.Holiday
import org.json.JSONArray
import org.json.JSONObject

class HolidayTexts(
    val name: String,
    val desc: String,
    val meaning: List<String>,
    val customs: List<String>,
    val fact: String?
)

class ThemeEmoji(val e: String, val top: Float?, val bottom: Float?, val left: Float?, val right: Float?, val size: Float, val delay: Float)

/** Chủ đề màu riêng của một số ngày lễ (Giáng sinh, Tết...). Màu cố định, không đổi theo palette. */
class HolidayTheme(
    val angle: Int,
    val stops: List<Pair<Float, Color>>,
    val glow: Color,
    val strong: Color,
    val accent: Color,
    val pattern: Color,
    val emojis: List<ThemeEmoji>
)

class HolidayCatalog(
    val holidays: List<Holiday>,
    private val vi: Map<String, HolidayTexts>,
    private val en: Map<String, HolidayTexts>,
    private val themes: Map<String, HolidayTheme>,
    private val themeOf: Map<String, String>
) {
    private val byId = holidays.associateBy { it.id }

    fun get(id: String): Holiday? = byId[id]

    fun theme(h: Holiday): HolidayTheme? = themeOf[h.id]?.let { themes[it] }

    fun texts(h: Holiday, lang: String): HolidayTexts {
        if (h.personal) return HolidayTexts(h.customName.orEmpty(), "", emptyList(), emptyList(), null)
        val base = vi[h.id] ?: HolidayTexts(h.id, "", emptyList(), emptyList(), null)
        if (lang != "en") return base
        val e = en[h.id] ?: return base
        return HolidayTexts(
            e.name.ifBlank { base.name }, e.desc.ifBlank { base.desc },
            e.meaning.ifEmpty { base.meaning }, e.customs.ifEmpty { base.customs }, e.fact ?: base.fact
        )
    }

    fun name(h: Holiday, lang: String) = texts(h, lang).name

    fun rule(h: Holiday, lang: String): String? = if (lang == "en") h.ruleEn ?: h.ruleVi else h.ruleVi

    companion object {
        private fun read(ctx: Context, file: String): String =
            ctx.assets.open("data/$file").bufferedReader(Charsets.UTF_8).use { it.readText() }

        private fun strList(a: JSONArray?): List<String> =
            if (a == null) emptyList() else List(a.length()) { a.getString(it) }

        private fun texts(o: JSONObject): HolidayTexts = HolidayTexts(
            o.optString("name"), o.optString("desc"),
            strList(o.optJSONArray("meaning")), strList(o.optJSONArray("customs")),
            if (o.isNull("fact")) null else o.optString("fact")
        )

        private fun textMap(json: String): Map<String, HolidayTexts> {
            val root = JSONObject(json)
            val out = HashMap<String, HolidayTexts>()
            for (k in root.keys()) out[k] = texts(root.getJSONObject(k))
            return out
        }

        private fun hex(s: String) = Color(android.graphics.Color.parseColor(s))
        private fun rgba(o: JSONObject) =
            Color(o.getInt("r") / 255f, o.getInt("g") / 255f, o.getInt("b") / 255f, o.getDouble("a").toFloat())
        private fun pct(o: JSONObject, k: String): Float? =
            if (o.isNull(k)) null else o.getString(k).removeSuffix("%").toFloat() / 100f

        fun load(ctx: Context): HolidayCatalog {
            val arr = JSONArray(read(ctx, "holidays.json"))
            val list = List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val rule = o.optJSONObject("rule")
                Holiday(
                    id = o.getString("id"), type = o.getString("type"), day = o.getInt("day"), month = o.getInt("month"),
                    weekday = o.getInt("weekday"), nth = o.getInt("nth"), isEve = o.getBoolean("isEve"),
                    category = o.getString("category"), icon = o.getString("icon"),
                    ruleVi = rule?.optString("vi"), ruleEn = rule?.optString("en")
                )
            }
            val tRoot = JSONObject(read(ctx, "holiday_themes.json"))
            val themes = HashMap<String, HolidayTheme>()
            val tj = tRoot.getJSONObject("themes")
            for (k in tj.keys()) {
                val t = tj.getJSONObject(k)
                val stops = t.getJSONArray("stops")
                val em = t.getJSONArray("emojis")
                themes[k] = HolidayTheme(
                    angle = t.getInt("angle"),
                    stops = List(stops.length()) { i -> stops.getJSONObject(i).let { it.getInt("p") / 100f to hex(it.getString("c")) } },
                    glow = rgba(t.getJSONObject("glow")), strong = hex(t.getString("strong")),
                    accent = hex(t.getString("accent")), pattern = rgba(t.getJSONObject("pattern")),
                    emojis = List(em.length()) { i ->
                        val e = em.getJSONObject(i)
                        ThemeEmoji(e.getString("e"), pct(e, "top"), pct(e, "bottom"), pct(e, "left"), pct(e, "right"),
                            e.getDouble("size").toFloat(), e.getDouble("delay").toFloat())
                    }
                )
            }
            val by = tRoot.getJSONObject("byHoliday")
            val themeOf = HashMap<String, String>()
            for (k in by.keys()) themeOf[k] = by.getString(k)
            return HolidayCatalog(list, textMap(read(ctx, "holiday_text_vi.json")), textMap(read(ctx, "holiday_text_en.json")), themes, themeOf)
        }
    }
}
