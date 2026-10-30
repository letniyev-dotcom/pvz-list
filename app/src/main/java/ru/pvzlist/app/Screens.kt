package ru.pvzlist.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ===================== Шапка ===================== */

/** Высота шапки = отступ статус-бара + 56dp на ВСЕХ экранах */
val HEAD_H = 56.dp

@Composable
fun Head(
    base: Dp,
    title: String,
    sub: String? = null,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(base + HEAD_H)
            .background(C.Card)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 56.dp, top = base + 14.dp)
        ) {
            Txt(title, 17, 20, 600, C.Ink, Modifier.fillMaxWidth(), align = TextAlign.Center, maxLines = 1)
            if (sub != null) {
                Txt(sub, 13, 16, 400, C.Mute, Modifier.fillMaxWidth(), align = TextAlign.Center, maxLines = 1)
            }
        }
        overlay()
    }
}

@Composable
fun BoxScope.BalancePill(base: Dp, onClick: () -> Unit) {
    Row(
        Modifier
            .align(Alignment.TopEnd)
            .padding(end = 16.dp, top = base + 7.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(C.Bg)
            .tap(onClick)
            .padding(start = 10.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Ico("wallet", 17.dp, C.Accent)
        Txt(fmt(WORKED * RATE), 14, 18, 600)
    }
}

@Composable
fun BoxScope.BackButton(base: Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .align(Alignment.TopStart)
            .padding(start = 12.dp, top = base + 4.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(C.Bg)
            .tap(onClick),
        contentAlignment = Alignment.Center
    ) {
        Ico("left", 22.dp, C.Accent)
    }
}

/* ===================== Общие блоки ===================== */

@Composable
fun Lbl(text: String, startPad: Dp = 32.dp, right: String? = null, chip: String = "") {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = startPad, end = 32.dp, top = 20.dp, bottom = 6.dp)
            .height(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(text, 12, 16, 600, C.Mute)
            if (chip.isNotEmpty()) {
                Box(
                    Modifier
                        .padding(start = 6.dp)
                        .wrapContentHeight(Alignment.CenterVertically, unbounded = true)
                        .background(C.AccentSoft, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Txt(chip, 12, 15, 600, C.Accent)
                }
            }
        }
        if (right != null) Txt(right, 12, 16, 600, C.Mute)
    }
}

@Composable
fun CardBox(content: @Composable () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(C.Card)
    ) { content() }
}

@Composable
fun RowItem(
    modifier: Modifier = Modifier,
    minH: Dp = 56.dp,
    hPad: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = minH)
            .padding(horizontal = hPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
fun TwoLine(
    title: AnnotatedString,
    sub: String,
    modifier: Modifier = Modifier,
    subColor: Color = C.Mute
) {
    Column(modifier) {
        Txt(title, 15, 22, 500)
        Txt(sub, 13, 18, 400, subColor)
    }
}

/* ===================== Сегодня ===================== */

@Composable
fun StartContent(st: AppState, ended: Boolean, viewH: Dp) {
    val h = maxOf(viewH - 12.dp, 380.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .height(h)
            .padding(start = 36.dp, end = 36.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (ended) {
            Ico("check", 96.dp, C.Accent, 1.4f, Modifier.alpha(0.3f))
            Spacer(Modifier.height(20.dp))
            Txt("Смена закрыта", 24, 30, 600, C.Ink, align = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Txt("Закрылась автоматически в 21:00", 15, 21, 400, C.Mute, align = TextAlign.Center)
        } else {
            Ico("box", 96.dp, C.Accent, 1.4f, Modifier.alpha(0.3f))
            Spacer(Modifier.height(20.dp))
            Txt("Смена не открыта", 24, 30, 600, C.Ink, align = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Txt("Подготовка с 08:40, открытие в 09:00", 15, 21, 400, C.Mute, align = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(C.Card)
                    .tap { st.night = !st.night }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Ico("moon", 22.dp, C.Accent)
                Column(Modifier.weight(1f)) {
                    Txt("Ночная приёмка", 15, 20, 600)
                    Txt("принять до 10:00", 13, 18, 400, C.Mute)
                }
                Box(
                    Modifier
                        .size(width = 48.dp, height = 28.dp)
                        .clip(CircleShape)
                        .background(if (st.night) C.Accent else C.Dim)
                        .padding(2.dp)
                ) {
                    Box(
                        Modifier
                            .offset(x = if (st.night) 20.dp else 0.dp)
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

/* ===================== Смены ===================== */

@Composable
fun MonthBtn(icon: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(RoundedCornerShape(16.dp))
            .then(if (enabled) Modifier.tap(onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Ico(icon, 22.dp, C.Accent)
    }
}

@Composable
fun DayCell(modifier: Modifier, d: Int, m: Int, mo: Month) {
    Column(
        modifier.height(54.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (d != 0) {
            val on = ((mo.start + d - 1) % 4) < 2
            var isNow = false
            var past = false
            var icon: String? = null
            var icolor = C.Accent
            var iw = 2f
            if (m == 0) {
                if (d == 29) {
                    isNow = true; icon = "box"; icolor = C.Accent
                } else if (d == 30) {
                    icon = "swap"; icolor = C.Warn
                } else if (d < 29) {
                    past = true
                    if (on) { icon = "check"; icolor = C.Ok; iw = 2.25f }
                }
            } else if (on) {
                icon = "box"; icolor = C.Accent
            }
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isNow) C.Accent else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Txt(
                    "$d", 15, 20,
                    if (isNow) 600 else 400,
                    if (isNow) Color.White else if (past) C.Mute else C.Ink
                )
            }
            icon?.let { Ico(it, 18.dp, icolor, iw) }
        }
    }
}

@Composable
fun LegendItem(icon: String, color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Ico(icon, 14.dp, color, 2.25f)
        Txt(text, 12, 15, 400, C.Mute)
    }
}

@Composable
fun NearRow(
    icon: String,
    color: Color,
    title: AnnotatedString,
    sub: String,
    tag: String? = null
) {
    RowItem(hPad = 28.dp) {
        Ico(icon, 22.dp, color)
        TwoLine(title, sub, Modifier.weight(1f))
        if (tag != null) Txt(tag, 13, 16, 600, C.Warn)
    }
}

@Composable
fun ShiftsContent(st: AppState) {
    val mo = MONTHS[st.month]
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MonthBtn("left", st.month > 0) { st.month = maxOf(0, st.month - 1) }
        Txt(mo.name, 16, 22, 600)
        MonthBtn("right", st.month < MONTHS.size - 1) {
            st.month = minOf(MONTHS.size - 1, st.month + 1)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
    ) {
        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach {
            Txt(it, 12, 15, 500, C.Mute, Modifier.weight(1f), align = TextAlign.Center)
        }
    }
    val cells = ArrayList<Int>()
    repeat(mo.off) { cells.add(0) }
    for (d in 1..mo.days) cells.add(d)
    while (cells.size % 7 != 0) cells.add(0)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { d -> DayCell(Modifier.weight(1f), d, st.month, mo) }
            }
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 28.dp, top = 10.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem("check", C.Ok, "Отработана")
        LegendItem("box", C.Accent, "Смена")
        LegendItem("swap", C.Warn, "Нужна подмена")
    }
    Lbl("Ближайшие", 28.dp)
    NearRow(
        "box", C.Accent,
        buildAnnotatedString {
            withStyle(SpanStyle(color = C.Accent, fontWeight = FontWeight.SemiBold)) {
                append("Сегодня")
            }
            append(", чт 29 октября")
        },
        "09:00–21:00"
    )
    NearRow("swap", C.Warn, AnnotatedString("Пт, 30 октября"), "09:00–21:00", "Нужна подмена")
    NearRow("box", C.Accent, AnnotatedString("Пн, 2 ноября"), "09:00–21:00")
    Spacer(Modifier.height(12.dp))
}

/* ===================== Баланс ===================== */

@Composable
fun BalanceContent() {
    val earned = WORKED * RATE
    val fc = TODAY_LEFT + NOV
    val fut = fc * RATE
    Column(Modifier.padding(top = 8.dp)) {
        CardBox {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp)) {
                Txt("уже заработано", 13, 16, 400, C.Mute)
                Txt(fmt(earned), 32, 40, 600, C.Ok, spacing = -1f)
                Txt(
                    "$WORKED ${pl(WORKED, "смена", "смены", "смен")} в октябре по ${fmt(RATE)}",
                    13, 16, 400, C.Mute, Modifier.padding(top = 2.dp)
                )
            }
            RowItem(minH = 64.dp) {
                Ico("box", 22.dp, C.Accent)
                Column(Modifier.weight(1f)) {
                    Txt("Сегодня", 15, 22, 600, C.Accent)
                    Txt("смена идёт", 13, 18, 400, C.Mute)
                }
                Txt("+" + fmt(RATE), 16, 20, 600, C.Accent)
            }
        }
        Lbl("Что ещё будет")
        CardBox {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)) {
                Txt(
                    "за $fc ${pl(fc, "запланированную смену", "запланированные смены", "запланированных смен")}",
                    13, 16, 400, C.Mute
                )
                Txt("+" + fmt(fut), 22, 28, 600, C.Accent)
            }
            RowItem(minH = 60.dp) {
                TwoLine(
                    AnnotatedString("До конца октября"),
                    "$TODAY_LEFT ${pl(TODAY_LEFT, "смена", "смены", "смен")}, сегодня",
                    Modifier.weight(1f)
                )
                Txt(fmt(TODAY_LEFT * RATE), 16, 20, 600)
            }
            RowItem(minH = 60.dp) {
                TwoLine(
                    AnnotatedString("Ноябрь"),
                    "$NOV ${pl(NOV, "смена", "смены", "смен")} по графику",
                    Modifier.weight(1f)
                )
                Txt(fmt(NOV * RATE), 16, 20, 600)
            }
            RowItem(minH = 64.dp) {
                Ico("swap", 22.dp, C.Warn)
                TwoLine(
                    AnnotatedString("Пт, 30 октября"),
                    "нужна подмена, не в расчёте",
                    Modifier.weight(1f)
                )
                Txt(fmt(RISK * RATE), 16, 20, 600, C.Warn)
            }
        }
        Lbl("Итого")
        CardBox {
            RowItem(minH = 64.dp) {
                TwoLine(
                    AnnotatedString("К концу ноября"),
                    "${fmt(earned)} + ${fmt(fut)}",
                    Modifier.weight(1f)
                )
                Txt(fmt(earned + fut), 20, 24, 600)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/* ===================== Профиль ===================== */

@Composable
fun ProfileContent(st: AppState) {
    val name = Tg.userName.ifBlank { "Сотрудник ПВЗ" }
    val ini = name.split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "ПВ" }
    var sure by remember { mutableStateOf(false) }
    val gn = { mp: String -> Tg.groupById(Cfg.dest(mp).g)?.name ?: "не выбрана" }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(56.dp).background(C.AccentSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Txt(ini, 18, 22, 600, C.Accent)
        }
        Txt(name, 16, 20, 600, maxLines = 1, ellipsis = true)
    }
    CardBox {
        RowItem {
            Txt("Ставка за смену", 15, 22, 500, modifier = Modifier.weight(1f))
            Txt(fmt(RATE), 16, 20, 600)
        }
        RowItem {
            Txt("График", 15, 22, 500, modifier = Modifier.weight(1f))
            Txt("2/2", 16, 20, 600)
        }
        RowItem {
            Txt("Время смены", 15, 22, 500, modifier = Modifier.weight(1f))
            Txt("09:00–21:00", 16, 20, 600)
        }
    }
    Lbl("Telegram")
    CardBox {
        RowItem {
            Txt("Аккаунт", 15, 22, 500, modifier = Modifier.weight(1f))
            val d = Tg.userPhone.removePrefix("7")
            Txt(
                if (Tg.userPhone.isBlank()) "—" else "+7 " + fmtPhoneDigits(d),
                14, 20, 500, C.Mute
            )
        }
        Hr()
        RowItem(Modifier.tap { st.setupFirst = false; st.setupOpen = true }, minH = 64.dp) {
            Column(Modifier.weight(1f)) {
                Txt("Группы и темы", 15, 22, 500)
                Txt(
                    "Ozon: ${gn("ozon")} · WB: ${gn("wb")}",
                    13, 18, 400, C.Mute, maxLines = 1, ellipsis = true
                )
            }
            Ico("right", 18.dp, C.Dim)
        }
        Hr()
        RowItem(Modifier.tap { if (sure) Tg.logout() else sure = true }) {
            Txt(
                when {
                    Tg.leaving -> "Выходим…"
                    sure -> "Нажмите ещё раз, чтобы выйти"
                    else -> "Выйти из аккаунта"
                },
                15, 22, 500, C.Warn, Modifier.weight(1f)
            )
        }
    }
    if (Tg.error.isNotEmpty()) {
        Txt(Tg.error, 13, 18, 400, C.Warn, Modifier.padding(horizontal = 32.dp, vertical = 10.dp))
    }
    Lbl("Адреса для отчётов")
    CardBox {
        AddrRow("Адрес Ozon", Cfg.addrOzon) { Cfg.setAddr("ozon", it) }
        Hr()
        AddrRow("Адрес WB", Cfg.addrWb) { Cfg.setAddr("wb", it) }
        Hr()
        RowItem {
            Column(Modifier.weight(1f)) {
                Txt("Открытие и закрытие Ozon", 15, 22, 500)
                Txt("через Seckeeper", 13, 18, 400, C.Mute)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

/* ===================== Нижняя панель ===================== */

@Composable
fun RowScope.NavItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit
) {
    val color = if (selected) C.Ink else C.NavIdle
    Column(
        Modifier
            .weight(1f)
            .tap(onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.height(26.dp), contentAlignment = Alignment.Center) { icon(color) }
        Txt(label, 12, 15, if (selected) 600 else 500, color)
    }
}

@Composable
fun BottomBar(
    st: AppState,
    ended: Boolean,
    navInset: Dp,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(C.Card)
    ) {
        if (st.tab == Tab.List && !st.balance && !ended && !st.open) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(C.Accent)
                        .tap(onOpen),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    Ico("door", 26.dp, Color.White, 1.9f)
                    Txt("Открыть смену", 18, 24, 600, Color.White)
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = maxOf(20.dp, navInset))
        ) {
            NavItem(st.tab == Tab.List, "Сегодня", { st.tab = Tab.List; st.balance = false }) {
                Ico("list", 26.dp, it, 1.9f)
            }
            NavItem(st.tab == Tab.Shifts, "Смены", { st.tab = Tab.Shifts; st.balance = false }) {
                Ico("cal", 26.dp, it, 1.9f)
            }
            NavItem(st.tab == Tab.Profile, "Профиль", { st.tab = Tab.Profile; st.balance = false }) {
                val sel = st.tab == Tab.Profile
                Box(
                    Modifier.size(26.dp).background(if (sel) C.Ink else C.Bg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Txt("АК", 10, 12, 600, if (sel) Color.White else C.NavIdle)
                }
            }
        }
    }
}
