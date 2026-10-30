package ru.pvzlist.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser
import java.time.LocalTime

/** Цвета — 1:1 с CSS-переменными из HTML */
object C {
    val Bg = Color(0xFFF4F4F8)
    val Card = Color(0xFFFFFFFF)
    val Line = Color(0xFFECECF2)
    val Ink = Color(0xFF1B1B20)
    val Mute = Color(0xFF6E6F7E)
    val Dim = Color(0xFFC4C5D2)
    val Accent = Color(0xFFA238FF)
    val AccentSoft = Color(0xFFF2E6FF)
    val Ok = Color(0xFF1A8F4D)
    val OkSoft = Color(0xFFE2F2E9)
    val Warn = Color(0xFFB45F06)
    val WarnSoft = Color(0xFFFAEBD9)
    val NavIdle = Color(0xFFA3A1B8)
    val Ozon = Color(0xFF1F6BFF)
    val Wb = Color(0xFFC42DB4)
    /** цвета аватарок групп, как COLORS в html */
    val Gav = listOf(Color(0xFFA238FF), Color(0xFF1F6BFF), Color(0xFFC42DB4), Color(0xFF1A8F4D), Color(0xFFB45F06))
}

const val RATE = 2800
const val WORKED = 14
const val TODAY_LEFT = 1
const val NOV = 15
const val RISK = 1
const val DEAD = 10 * 3600
const val END = 21 * 3600

fun nowS(): Int = LocalTime.now().toSecondOfDay()
fun p2(n: Int): String = n.toString().padStart(2, '0')
fun hm(s: Int): String = p2(s / 3600) + ":" + p2(s % 3600 / 60)
fun cdFmt(sec: Int): String {
    val s = maxOf(0, sec)
    return "${s / 3600}:${p2(s % 3600 / 60)}:${p2(s % 60)}"
}

private val thousands = Regex("\\B(?=(\\d{3})+(?!\\d))")
fun fmt(n: Int): String = n.toString().replace(thousands, "\u00A0") + "\u00A0₽"

fun pl(n: Int, a: String, b: String, c: String): String {
    val m = n % 10
    val h = n % 100
    return if (m == 1 && h != 11) a else if (m in 2..4 && (h < 10 || h >= 20)) b else c
}

class Month(val name: String, val days: Int, val off: Int, val start: Int)

val MONTHS = listOf(
    Month("Октябрь 2026", 31, 3, 0),
    Month("Ноябрь 2026", 30, 6, 31)
)

enum class Tab { List, Shifts, Profile }
enum class Scr { Today, Shifts, Balance, Profile }

class AppState {
    var tab by mutableStateOf(Tab.List)
    var balance by mutableStateOf(false)
    var month by mutableStateOf(0)
    var night by mutableStateOf(true)
    var open by mutableStateOf(false)
    var at by mutableStateOf("")

    /** открытый отчёт (id типа) */
    var repId by mutableStateOf<String?>(null)
    val form = RepForm()
    var sending by mutableStateOf(false)

    /** настройка групп и тем */
    var setupOpen by mutableStateOf(false)
    var setupFirst by mutableStateOf(false)
    var pick by mutableStateOf<Pick?>(null)
    var pickBusy by mutableStateOf(false)

    var toastText by mutableStateOf("")
    var toastBad by mutableStateOf(false)
    var toastKey by mutableStateOf(0)

    fun toast(t: String, bad: Boolean = false) {
        toastText = t
        toastBad = bad
        toastKey++
    }
}

/* ---------- SVG-иконки из HTML (viewBox 24x24, stroke) ---------- */

sealed class Sh {
    class P(val d: String) : Sh()
    class Ci(val cx: Float, val cy: Float, val r: Float) : Sh()
    class Re(val x: Float, val y: Float, val w: Float, val h: Float, val rx: Float) : Sh()
}

private val ICONS: Map<String, List<Sh>> = mapOf(
    "box" to listOf(
        Sh.P("M12 3l8 4.5v9L12 21l-8-4.5v-9L12 3z"),
        Sh.P("M4 7.5l8 4.5 8-4.5M12 12v9")
    ),
    "swap" to listOf(Sh.P("M4 8h14l-3.5-3.5M20 16H6l3.5 3.5")),
    "send" to listOf(
        Sh.P("M21 3L10.5 13.5"),
        Sh.P("M21 3l-6.5 18-4-7.5L3 9.5z")
    ),
    "check" to listOf(Sh.P("M5 12.5l4.5 4.5L19 7.5")),
    "wallet" to listOf(
        Sh.P("M17 8V6.5A1.5 1.5 0 0 0 15.5 5H6a2 2 0 0 0-2 2"),
        Sh.P("M4 7v10.5A1.5 1.5 0 0 0 5.5 19h12A1.5 1.5 0 0 0 19 17.5v-8A1.5 1.5 0 0 0 17.5 8H5a1 1 0 0 1-1-1z"),
        Sh.Ci(15f, 13.5f, 1f)
    ),
    "left" to listOf(Sh.P("M15 5l-7 7 7 7")),
    "right" to listOf(Sh.P("M9 5l7 7-7 7")),
    "list" to listOf(
        Sh.P("M10 6.5h10M10 12h10M10 17.5h10"),
        Sh.P("M3.8 6.4l1.3 1.3 2.3-2.6"),
        Sh.P("M3.8 11.9l1.3 1.3 2.3-2.6"),
        Sh.P("M3.8 17.4l1.3 1.3 2.3-2.6")
    ),
    "cal" to listOf(
        Sh.Re(3.5f, 5f, 17f, 15.5f, 3f),
        Sh.P("M3.5 10h17M8 3v4M16 3v4")
    ),
    "bulb" to listOf(
        Sh.P("M9 18h6M10 21h4"),
        Sh.P("M12 3a6 6 0 0 0-3.5 10.9c0.6 0.5 1 1.2 1 2.1h5c0 -0.9 0.4 -1.6 1 -2.1A6 6 0 0 0 12 3z")
    ),
    "eye" to listOf(
        Sh.P("M2 12s3.5-6.5 10-6.5S22 12 22 12s-3.5 6.5-10 6.5S2 12 2 12z"),
        Sh.Ci(12f, 12f, 2.8f)
    ),
    "grid" to listOf(
        Sh.Re(4f, 4f, 7f, 7f, 1.8f),
        Sh.Re(13f, 4f, 7f, 7f, 1.8f),
        Sh.Re(4f, 13f, 7f, 7f, 1.8f),
        Sh.Re(13f, 13f, 7f, 7f, 1.8f)
    ),
    "hour" to listOf(Sh.P("M7 3h10M7 21h10M8 3v3l4 4.5L16 6V3M8 21v-3l4-4.5 4 4.5v3")),
    "undo" to listOf(
        Sh.P("M9 7L4.5 11.5 9 16"),
        Sh.P("M4.5 11.5H15a4.5 4.5 0 0 1 0 9h-3")
    ),
    "clip" to listOf(
        Sh.Re(5f, 4.5f, 14f, 16.5f, 2.5f),
        Sh.P("M9 4.5V3.5h6v1M9 13l2.2 2.2L15.5 11")
    ),
    "power" to listOf(
        Sh.P("M12 3v8"),
        Sh.P("M6.8 6.8a8 8 0 1 0 10.4 0")
    ),
    "moon" to listOf(Sh.P("M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z")),
    "truck" to listOf(
        Sh.P("M3 6.5h11V16H3z"),
        Sh.P("M14 9.5h4l3 3.2V16h-7"),
        Sh.Ci(7f, 17.5f, 1.8f),
        Sh.Ci(17f, 17.5f, 1.8f)
    ),
    "clock" to listOf(
        Sh.Ci(12f, 12f, 9f),
        Sh.P("M12 7v5l3 2")
    ),
    "cam" to listOf(
        Sh.P("M4 8.5h3L8.5 6h7L17 8.5h3V19H4z"),
        Sh.Ci(12f, 13.5f, 3.5f)
    ),
    "shot" to listOf(
        Sh.Re(7f, 3f, 10f, 18f, 2.5f),
        Sh.P("M10.5 18h3")
    ),
    "warn" to listOf(
        Sh.P("M12 4l9 16H3z"),
        Sh.P("M12 10v4.5M12 17.6v.1")
    ),
    "door" to listOf(
        Sh.P("M6 21V4.5A1.5 1.5 0 0 1 7.5 3h9A1.5 1.5 0 0 1 18 4.5V21M4 21h16"),
        Sh.Ci(14.5f, 12.5f, 0.9f)
    )
)

object IconPaths {
    private val cache = HashMap<String, List<Path>>()

    fun get(name: String): List<Path> = cache.getOrPut(name) {
        ICONS.getValue(name).map { toPath(it) }
    }

    private fun toPath(s: Sh): Path = when (s) {
        is Sh.P -> PathParser().parsePathString(s.d).toPath()
        is Sh.Ci -> Path().apply {
            addOval(Rect(s.cx - s.r, s.cy - s.r, s.cx + s.r, s.cy + s.r))
        }
        is Sh.Re -> Path().apply {
            addRoundRect(RoundRect(s.x, s.y, s.x + s.w, s.y + s.h, s.rx, s.rx))
        }
    }
}
