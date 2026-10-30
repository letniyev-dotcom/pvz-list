package ru.pvzlist.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/* ---------- типы отчётов: 1:1 с TYPES из html ---------- */

/** need: photo / shot / kv; fmt: шаблон сообщения, {a} — адрес, {k} — КВ */
class MpCfg(val need: List<String>, val hint: String, val fmt: String)

class RType(
    val id: String,
    val icon: String,
    val title: String,
    val all: Boolean,
    val kw: List<String>,
    val mps: Map<String, MpCfg>
)

val MPN = mapOf("ozon" to "Ozon", "wb" to "WB")
val MPF = mapOf("ozon" to "Ozon", "wb" to "Wildberries")

val TYPES: Map<String, RType> = linkedMapOf(
    "accept" to RType(
        "accept", "truck", "Приёмка", true, listOf("прием"),
        linkedMapOf(
            "ozon" to MpCfg(
                listOf("photo"),
                "приёмка → перевозки, затем сделайте фото",
                "Завершение перевозок\n{a}"
            ),
            "wb" to MpCfg(
                emptyList(),
                "отправим адрес и «ВБ принят»",
                "{a}\nВБ принят"
            )
        )
    ),
    "red" to RType(
        "red", "undo", "Красные возвраты", false, listOf("красн", "возврат"),
        linkedMapOf(
            "ozon" to MpCfg(
                listOf("photo", "kv"),
                "отправка → выберите контейнеры и сделайте фото",
                "Красные возвраты\n{a}\n{k}"
            ),
            "wb" to MpCfg(
                listOf("shot", "kv"),
                "мой ПВЗ → экран «Вернуть», сделайте скриншот",
                "{a}\n{k}"
            )
        )
    ),
    "box" to RType(
        "box", "box", "Закрытие коробки", false, listOf("короб"),
        linkedMapOf(
            "wb" to MpCfg(
                listOf("photo"),
                "возврат → коробки, сфотографируйте закрытую коробку",
                "{a}\nкоробка закрыта"
            )
        )
    ),
    "brk" to RType(
        "brk", "clock", "Перерыв", false, listOf("перерыв"),
        linkedMapOf(
            "ozon" to MpCfg(
                listOf("photo"),
                "распечатайте шаблон, повесьте на входе и сделайте фото",
                "Перерыв\n{a}"
            )
        )
    )
)

/** карточки под «Приёмкой» */
val OTHERS = listOf("red", "box", "brk")

/** какие отчёты уходят в каждый маркетплейс */
val MPTYPES = mapOf(
    "ozon" to listOf("accept", "red", "brk"),
    "wb" to listOf("accept", "red", "box")
)

fun mpsOf(t: RType): List<String> = t.mps.keys.toList()

/* ---------- куда отправлять: группа + тема под каждый отчёт ---------- */

class MpDest {
    /** id группы в Telegram, 0 — не выбрана */
    var g by mutableStateOf(0L)
    /** id отчёта -> id темы */
    val topics = mutableStateMapOf<String, Long>()
}

object Cfg {
    private var sp: SharedPreferences? = null
    val ozon = MpDest()
    val wb = MpDest()
    var addrOzon by mutableStateOf("")
    var addrWb by mutableStateOf("")

    fun dest(mp: String): MpDest = if (mp == "ozon") ozon else wb
    fun addr(mp: String): String = if (mp == "ozon") addrOzon else addrWb

    fun setAddr(mp: String, v: String) {
        if (mp == "ozon") addrOzon = v else addrWb = v
        save()
    }

    fun init(ctx: Context) {
        if (sp != null) return
        val p = ctx.getSharedPreferences("pvz", Context.MODE_PRIVATE)
        sp = p
        try {
            val j = JSONObject(p.getString("cfg", "{}") ?: "{}")
            read(j.optJSONObject("ozon"), ozon)
            read(j.optJSONObject("wb"), wb)
            addrOzon = j.optString("a_ozon", "")
            addrWb = j.optString("a_wb", "")
        } catch (e: Exception) {
        }
        Sent.load(p)
    }

    private fun read(j: JSONObject?, d: MpDest) {
        if (j == null) return
        d.g = j.optLong("g", 0L)
        d.topics.clear()
        val t = j.optJSONObject("t")
        if (t != null) {
            val keys = t.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                d.topics[k] = t.optLong(k)
            }
        }
    }

    private fun write(d: MpDest): JSONObject {
        val o = JSONObject()
        o.put("g", d.g)
        val t = JSONObject()
        for ((k, v) in d.topics) t.put(k, v)
        o.put("t", t)
        return o
    }

    fun save() {
        val p = sp ?: return
        val j = JSONObject()
        j.put("ozon", write(ozon))
        j.put("wb", write(wb))
        j.put("a_ozon", addrOzon)
        j.put("a_wb", addrWb)
        p.edit().putString("cfg", j.toString()).apply()
    }

    /** при выходе из аккаунта группы сбрасываются (адреса остаются) */
    fun clearDest() {
        ozon.g = 0L; ozon.topics.clear()
        wb.g = 0L; wb.topics.clear()
        save()
    }
}

/* ---------- что уже отправлено за смену ---------- */

class SentItem(val mp: String, val time: String)

object Sent {
    val map = mutableStateMapOf<String, List<SentItem>>()
    private var sp: SharedPreferences? = null

    fun of(id: String): List<SentItem> = map[id] ?: emptyList()

    fun add(id: String, mp: String, time: String) {
        map[id] = of(id) + SentItem(mp, time)
        save()
    }

    fun clear() {
        map.clear()
        save()
    }

    fun load(p: SharedPreferences) {
        sp = p
        try {
            val j = JSONObject(p.getString("sent", "{}") ?: "{}")
            if (j.optString("day") != LocalDate.now().toString()) return
            val items = j.optJSONObject("items") ?: return
            val keys = items.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val a = items.optJSONArray(k) ?: continue
                val l = ArrayList<SentItem>()
                for (i in 0 until a.length()) {
                    val e = a.optJSONArray(i) ?: continue
                    l.add(SentItem(e.optString(0), e.optString(1)))
                }
                map[k] = l
            }
        } catch (e: Exception) {
        }
    }

    private fun save() {
        val p = sp ?: return
        val items = JSONObject()
        for ((k, v) in map) {
            val a = JSONArray()
            v.forEach { s -> a.put(JSONArray().put(s.mp).put(s.time)) }
            items.put(k, a)
        }
        val j = JSONObject()
        j.put("day", LocalDate.now().toString())
        j.put("items", items)
        p.edit().putString("sent", j.toString()).apply()
    }
}

fun isDone(t: RType): Boolean {
    val a = Sent.of(t.id)
    return if (t.all) mpsOf(t).all { m -> a.any { it.mp == m } } else a.isNotEmpty()
}

fun subOf(t: RType): String {
    val a = Sent.of(t.id)
    if (a.isEmpty()) return mpsOf(t).joinToString(" · ") { MPN.getValue(it) }
    if (isDone(t)) return "отправлено в ${a.last().time}" + (if (a.size > 1) " · ×${a.size}" else "")
    return "ещё " + mpsOf(t).filter { m -> a.none { it.mp == m } }.joinToString(", ") { MPN.getValue(it) }
}

/* ---------- куда уйдёт отчёт ---------- */

class DestInfo(val ok: Boolean, val g: TgGroup?, val topic: TgTopic?, val loading: Boolean = false)

fun destOf(mp: String, tid: String): DestInfo {
    val d = Cfg.dest(mp)
    val g = Tg.groupById(d.g)
    if (g == null) {
        return DestInfo(false, null, null, loading = d.g != 0L && Tg.gstate == "load")
    }
    if (!g.forum) return DestInfo(true, g, null)
    val want = d.topics[tid]
    val tp = g.topics?.firstOrNull { it.id == want }
    return DestInfo(tp != null, g, tp)
}

fun mpDone(mp: String): Boolean = MPTYPES.getValue(mp).all { destOf(mp, it).ok }

fun norm(s: String): String = s.lowercase().replace('ё', 'е')

/** подбираем темы по названию: «Приёмка» -> отчёт про приёмку и т.д. */
fun autoMap(g: TgGroup): Map<String, Long> {
    val out = HashMap<String, Long>()
    if (!g.forum) return out
    TYPES.values.forEach { t ->
        val tp = g.topics?.firstOrNull { x -> t.kw.any { k -> norm(x.name).contains(k) } }
        if (tp != null) out[t.id] = tp.id
    }
    return out
}
