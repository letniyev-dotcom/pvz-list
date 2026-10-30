package ru.pvzlist.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.math.roundToInt

/* ===================== состояние формы отчёта ===================== */

/** Что выбираем в открытом экране выбора: kind = "g" (группа) или "t" (тема) */
class Pick(val kind: String, val mp: String, val tid: String = "")

class RepForm {
    var mp by mutableStateOf("ozon")
    var photoPath by mutableStateOf<String?>(null)
    var preview by mutableStateOf<ImageBitmap?>(null)
    var kv by mutableStateOf<String?>(null)
    var note by mutableStateOf("")

    fun reset(newMp: String, keepNote: Boolean = false) {
        mp = newMp
        photoPath = null
        preview = null
        kv = null
        if (!keepNote) note = ""
    }
}

private val DN = listOf("вс", "пн", "вт", "ср", "чт", "пт", "сб")
private val MN = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря"
)

fun dateCap(): String {
    val d = LocalDate.now()
    return "${DN[d.dayOfWeek.value % 7]} ${d.dayOfMonth} ${MN[d.monthValue - 1]}"
}

fun msgOf(f: RepForm, mp: String, c: MpCfg): String {
    val k = when (f.kv) {
        "yes" -> "КВ есть"
        "no" -> "КВ нет"
        else -> "КВ …"
    }
    val a = Cfg.addr(mp).trim().ifEmpty { "— адрес не задан —" }
    var m = c.fmt.replace("{a}", a).replace("{k}", k)
    if (f.note.isNotBlank()) m += "\n\n" + f.note.trim()
    return m
}

fun ready(mp: String, tid: String, f: RepForm, c: MpCfg): Boolean =
    Cfg.addr(mp).isNotBlank() && destOf(mp, tid).ok &&
        c.need.all { n -> if (n == "kv") f.kv != null else f.photoPath != null }

fun membersTxt(g: TgGroup): String {
    val base = if (g.members > 0) "${g.members} ${pl(g.members, "участник", "участника", "участников")}" else "группа"
    return base + if (g.forum) " · с темами" else ""
}

private fun initials(n: String): String {
    val r = n.filter { it.isLetterOrDigit() || it == ' ' }
        .split(' ').filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().toString() }.uppercase()
    return r.ifEmpty { "#" }
}

/* ===================== общие кусочки ===================== */

class Holder<T> {
    var v: T? = null
}

/** Экран, который выезжает справа (как .ov в html) */
@Composable
fun <T : Any> Slide(value: T?, content: @Composable (T) -> Unit) {
    val h = remember { Holder<T>() }
    if (value != null) h.v = value
    AnimatedVisibility(
        visible = value != null,
        modifier = Modifier.fillMaxSize(),
        enter = slideInHorizontally(tween(300)) { it },
        exit = slideOutHorizontally(tween(300)) { it }
    ) {
        val v = h.v
        if (v != null) content(v)
    }
}

@Composable
fun Hr() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
}

@Composable
fun SepCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(C.Card),
        content = content
    )
}

@Composable
fun RTop(onBack: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(C.Card)
                    .tap(onBack),
                contentAlignment = Alignment.Center
            ) {
                Ico("left", 22.dp, C.Accent)
            }
        }
    }
}

@Composable
fun RHero(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 14.dp)) {
        Txt(title, 26, 32, 600, spacing = -0.3f)
        Spacer(Modifier.height(4.dp))
        Txt(text, 15, 21, 400, C.Mute)
    }
}

@Composable
fun RBar(navInset: Dp, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(C.Card)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = maxOf(28.dp, navInset))
    ) { content() }
}

@Composable
fun RCta(text: String, enabled: Boolean, icon: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (enabled) C.Accent else C.Bg)
            .then(if (enabled) Modifier.tap(onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null) Ico(icon, 22.dp, if (enabled) Color.White else C.Mute)
        Txt(text, 18, 24, 600, if (enabled) Color.White else C.Mute)
    }
}

@Composable
fun Gav(g: TgGroup, size: Dp) {
    Box(
        Modifier.size(size).background(C.Gav[g.colorIdx], CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Txt(initials(g.name), (size.value * 0.32f).roundToInt(), 16, 600, Color.White, maxLines = 1)
    }
}

@Composable
fun Tile(icon: String, size: Dp = 42.dp, radius: Dp = 16.dp, iconSize: Dp = 20.dp, bg: Color = C.AccentSoft, fg: Color = C.Accent) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius)).background(bg),
        contentAlignment = Alignment.Center
    ) { Ico(icon, iconSize, fg) }
}

@Composable
fun SegBar(items: List<Pair<String, String>>, selected: String?, onPick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(C.Card)
            .padding(4.dp)
    ) {
        items.forEach { (key, label) ->
            val on = key == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) C.Ink else Color.Transparent)
                    .tap { onPick(key) },
                contentAlignment = Alignment.Center
            ) {
                Txt(label, 16, 20, 600, if (on) Color.White else C.Mute)
            }
        }
    }
}

/** Поле ввода в строке профиля: текст справа */
@Composable
fun AddrRow(label: String, value: String, onChange: (String) -> Unit) {
    RowItem {
        Txt(label, 15, 22, 500, modifier = Modifier.weight(1f))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = LocalOnest.current,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = C.Ink,
                textAlign = TextAlign.End
            ),
            cursorBrush = SolidColor(C.Accent),
            modifier = Modifier.width(190.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) Txt("адрес пвз", 15, 22, 600, C.Dim, align = TextAlign.End)
                    inner()
                }
            }
        )
    }
}

/* ===================== Сегодня: карточки отчётов ===================== */

fun openRep(st: AppState, id: String) {
    val t = TYPES.getValue(id)
    val a = Sent.of(id)
    val first = mpsOf(t).firstOrNull { m -> a.none { it.mp == m } } ?: mpsOf(t)[0]
    st.form.reset(first)
    st.repId = id
}

@Composable
fun TodayReports(st: AppState, now: Int) {
    val a = TYPES.getValue("accept")
    val done = isDone(a)
    val timed = st.night && !done
    val left = DEAD - now
    val late = left <= 1800

    Txt(
        buildAnnotatedString {
            withStyle(SpanStyle(color = C.Ink, fontWeight = FontWeight.SemiBold)) { append(dateCap()) }
            append(" · смена открыта в ${st.at}")
        },
        13, 18, 500, C.Mute,
        Modifier.padding(start = 32.dp, end = 32.dp, top = 6.dp, bottom = 10.dp)
    )

    val sub = if (done || Sent.of("accept").isNotEmpty()) subOf(a)
    else if (st.night) "ночная · до 10:00" else subOf(a)
    val bg = if (done) C.OkSoft else if (timed && late) C.Warn else C.Accent
    val fg = if (done) C.Ink else Color.White
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(bg)
            .tap { openRep(st, "accept") }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(if (done) C.Card else Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) { Ico(a.icon, 26.dp, if (done) C.Ok else Color.White) }
        Column(Modifier.weight(1f)) {
            Txt(a.title, 20, 26, 600, fg, maxLines = 1, ellipsis = true)
            Txt(
                sub, 13, 18, 400,
                if (done) C.Ok else Color.White.copy(alpha = 0.85f),
                maxLines = 1, ellipsis = true
            )
        }
        if (done) {
            Box(
                Modifier.size(28.dp).background(C.Ok, CircleShape),
                contentAlignment = Alignment.Center
            ) { Ico("check", 16.dp, Color.White, 3f) }
        } else if (timed) {
            Box(
                Modifier
                    .background(Color.White, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Txt(
                    if (left > 0) cdFmt(left) else "время вышло", 20, 24, 600,
                    if (late) C.Warn else C.Accent, tnum = true, maxLines = 1
                )
            }
        } else {
            Box(
                Modifier.size(44.dp).background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) { Ico("right", 20.dp, C.Accent, 2.2f) }
        }
    }

    Lbl("Отчёты в группу")
    Column(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OTHERS.chunked(2).forEach { row ->
            if (row.size == 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { id -> ReportCard(st, id, false, Modifier.weight(1f)) }
                }
            } else {
                ReportCard(st, row[0], true, Modifier.fillMaxWidth())
            }
        }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
fun ReportCard(st: AppState, id: String, wide: Boolean, modifier: Modifier) {
    val t = TYPES.getValue(id)
    val d = isDone(t)
    val shape = RoundedCornerShape(28.dp)
    val base = modifier
        .clip(shape)
        .background(if (d) C.OkSoft else C.Card)
        .tap { openRep(st, id) }
    val tile: @Composable () -> Unit = {
        Tile(
            if (d) "check" else t.icon, 42.dp, 16.dp, 22.dp,
            if (d) C.Card else C.AccentSoft, if (d) C.Ok else C.Accent
        )
    }
    val title: @Composable () -> Unit = {
        Txt(t.title, 16, 20, 600, maxLines = 1, ellipsis = true)
        Txt(subOf(t), 12, 16, 400, if (d) C.Ok else C.Mute, Modifier.padding(top = 2.dp), maxLines = 1, ellipsis = true)
    }
    if (wide) {
        Row(
            base.height(76.dp).padding(start = 14.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            tile()
            Column(Modifier.weight(1f)) { title() }
        }
    } else {
        Box(base.height(124.dp)) {
            Column(
                Modifier.fillMaxSize().padding(start = 14.dp, top = 14.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                tile()
                Column { title() }
            }
            if (d) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 14.dp, end = 14.dp)
                        .size(24.dp)
                        .background(C.Ok, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Ico("check", 14.dp, Color.White, 3f) }
            }
        }
    }
}

/* ===================== фото ===================== */

private fun copyToCache(ctx: Context, uri: Uri): File? {
    return try {
        val dir = File(ctx.cacheDir, "cam")
        dir.mkdirs()
        val f = File(dir, "pick_${System.currentTimeMillis()}.jpg")
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(f).use { out -> input.copyTo(out) }
        } ?: return null
        f
    } catch (e: Throwable) {
        null
    }
}

/** Уменьшаем до 1600 px, поворачиваем по EXIF, сжимаем в JPEG (как shrink() в html) */
private fun preparePhoto(ctx: Context, src: File): Pair<String, ImageBitmap>? {
    return try {
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        BitmapFactory.decodeFile(src.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2400) sample *= 2
        val opts = BitmapFactory.Options()
        opts.inSampleSize = sample
        var bmp = BitmapFactory.decodeFile(src.path, opts) ?: return null
        val rot = when (
            ExifInterface(src.path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rot != 0f) {
            val m = Matrix()
            m.postRotate(rot)
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        val k = 1600f / maxOf(bmp.width, bmp.height)
        if (k < 1f) {
            bmp = Bitmap.createScaledBitmap(
                bmp, (bmp.width * k).roundToInt(), (bmp.height * k).roundToInt(), true
            )
        }
        val out = File(ctx.cacheDir, "out_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        val pk = 700f / maxOf(bmp.width, bmp.height)
        val prev = if (pk < 1f) {
            Bitmap.createScaledBitmap(
                bmp, (bmp.width * pk).roundToInt(), (bmp.height * pk).roundToInt(), true
            )
        } else bmp
        src.delete()
        Pair(out.path, prev.asImageBitmap())
    } catch (e: Throwable) {
        null
    }
}

private suspend fun applyPhoto(ctx: Context, st: AppState, src: File) {
    val r = withContext(Dispatchers.IO) { preparePhoto(ctx, src) }
    if (r == null) {
        st.toast("Не получилось открыть фото", true)
        return
    }
    st.form.photoPath?.let { File(it).delete() }
    st.form.photoPath = r.first
    st.form.preview = r.second
}

@Composable
private fun DropZone(preview: ImageBitmap?, icon: String, text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(196.dp)
            .clip(shape)
            .background(C.Card)
            .drawBehind {
                if (preview == null) {
                    val w = 1.5.dp.toPx()
                    drawRoundRect(
                        color = C.Dim,
                        topLeft = Offset(w / 2f, w / 2f),
                        size = Size(size.width - w, size.height - w),
                        cornerRadius = CornerRadius(28.dp.toPx() - w / 2f),
                        style = Stroke(
                            width = w,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx()))
                        )
                    )
                }
            }
            .tap(onClick),
        contentAlignment = Alignment.Center
    ) {
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .background(Color(0xA60E0F11), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Txt("Заменить", 12, 16, 400, Color.White) }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Ico(icon, 36.dp, C.Accent)
                Txt(text, 16, 20, 600, C.Accent)
            }
        }
    }
}

/* ===================== экран отчёта ===================== */

private fun sendReport(st: AppState, id: String) {
    if (st.sending) return
    val f = st.form
    val t = TYPES.getValue(id)
    val mp = f.mp
    val c = t.mps[mp] ?: return
    if (!ready(mp, id, f, c)) return
    val d = destOf(mp, id)
    val g = d.g ?: return
    st.sending = true
    val text = msgOf(f, mp, c)
    val path = f.photoPath
    val topic = d.topic
    Tg.sendReport(g.id, topic?.id, text, path) { err ->
        st.sending = false
        if (err == null) {
            Sent.add(id, mp, hm(nowS()))
            st.repId = null
            if (path != null) File(path).delete()
            st.toast("Отправлено · " + g.name + (if (topic != null) " › " + topic.name else ""))
        } else {
            st.toast(err, true)
        }
    }
}

@Composable
fun ReportScreen(st: AppState, id: String, base: Dp, navInset: Dp) {
    val t = TYPES.getValue(id)
    val f = st.form
    val mps = mpsOf(t)
    val mp = if (f.mp in t.mps) f.mp else mps[0]
    val c = t.mps.getValue(mp)
    val dst = destOf(mp, id)
    val ok = ready(mp, id, f, c)

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var camFile by remember { mutableStateOf<File?>(null) }
    val camL = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = camFile
        if (saved && file != null) scope.launch { applyPhoto(ctx, st, file) }
    }
    val pickL = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val tmp = withContext(Dispatchers.IO) { copyToCache(ctx, uri) }
                if (tmp != null) applyPhoto(ctx, st, tmp) else st.toast("Не получилось открыть фото", true)
            }
        }
    }

    fun takePhoto() {
        try {
            val dir = File(ctx.cacheDir, "cam")
            dir.mkdirs()
            val file = File(dir, "cam_${System.currentTimeMillis()}.jpg")
            camFile = file
            camL.launch(FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file))
        } catch (e: Exception) {
            st.toast("Не получилось открыть камеру", true)
        }
    }

    Column(Modifier.fillMaxSize().background(C.Bg).tap { }) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = base, bottom = 16.dp)
        ) {
            RTop { st.repId = null }
            RHero(t.title, c.hint)

            if (mps.size > 1) {
                Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    SegBar(mps.map { it to (MPF[it] ?: it) }, mp) { m ->
                        if (m != f.mp) f.reset(m, keepNote = true)
                    }
                }
            } else {
                Box(Modifier.padding(start = 16.dp, bottom = 14.dp)) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(C.Card)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) { Txt(MPF[mps[0]] ?: "", 14, 20, 600) }
                }
            }

            if (c.need.contains("photo") || c.need.contains("shot")) {
                val shot = c.need.contains("shot")
                DropZone(
                    f.preview,
                    if (shot) "shot" else "cam",
                    if (shot) "Загрузить скриншот" else "Сделать фото"
                ) {
                    if (shot) pickL.launch("image/*") else takePhoto()
                }
            }

            if (c.need.contains("kv")) {
                Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                    SegBar(listOf("yes" to "КВ есть", "no" to "КВ нет"), f.kv) { f.kv = it }
                }
            }

            BasicTextField(
                value = f.note,
                onValueChange = { f.note = it },
                textStyle = TextStyle(
                    fontFamily = LocalOnest.current,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = C.Ink
                ),
                cursorBrush = SolidColor(C.Accent),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).fillMaxWidth(),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(C.Card)
                            .padding(horizontal = 18.dp, vertical = 17.dp)
                    ) {
                        if (f.note.isEmpty()) Txt("комментарий (по желанию)", 15, 22, 400, C.Dim)
                        inner()
                    }
                }
            )

            // куда уйдёт отчёт
            val bad = !dst.ok
            Row(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (bad) C.WarnSoft else C.Card)
                    .tap { st.setupFirst = false; st.setupOpen = true }
                    .padding(start = 14.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val g = dst.g
                if (dst.ok && g != null) {
                    Gav(g, 40.dp)
                    Column(Modifier.weight(1f)) {
                        Txt(g.name, 15, 20, 600, maxLines = 1, ellipsis = true)
                        Txt(
                            if (dst.topic != null) "тема: " + dst.topic.name else "общий чат",
                            13, 18, 400, C.Mute, maxLines = 1, ellipsis = true
                        )
                    }
                } else {
                    Tile("warn", 42.dp, 16.dp, 20.dp, C.Card, C.Warn)
                    Column(Modifier.weight(1f)) {
                        Txt(
                            if (dst.loading) "Загружаем группы…" else if (g != null) "Выберите тему" else "Выберите группу",
                            15, 20, 600, C.Warn
                        )
                        Txt("куда отправить этот отчёт", 13, 18, 400, C.Mute)
                    }
                }
                Ico("right", 18.dp, C.Dim)
            }

            // как сообщение будет выглядеть в группе
            Column(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 8.dp))
                    .background(C.AccentSoft)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                val pv = f.preview
                if (pv != null) {
                    Image(
                        bitmap = pv,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Txt(msgOf(f, mp, c), 14, 20, 400)
            }
        }
        RBar(navInset) {
            RCta(if (st.sending) "Отправляем…" else "Отправить в группу", ok && !st.sending, "send") {
                sendReport(st, id)
            }
        }
    }
}

/* ===================== настройка групп и тем ===================== */

@Composable
fun SetupScreen(st: AppState, base: Dp, navInset: Dp) {
    val done = mpDone("ozon") && mpDone("wb")
    val close = { st.setupOpen = false }
    Column(Modifier.fillMaxSize().background(C.Bg).tap { }) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = base, bottom = 16.dp)
        ) {
            RTop(if (st.setupFirst) null else close)
            RHero("Куда отправлять", "группа для каждого маркетплейса и тема под каждый вид отчёта")
            listOf("ozon", "wb").forEach { mp ->
                val d = Cfg.dest(mp)
                val g = Tg.groupById(d.g)
                Row(
                    Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 20.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.size(8.dp).background(if (mp == "ozon") C.Ozon else C.Wb, CircleShape))
                    Txt(MPF.getValue(mp), 13, 16, 600, C.Mute)
                }
                SepCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp)
                            .tap { st.pick = Pick("g", mp) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (g != null) Gav(g, 44.dp) else Tile("send")
                        Column(Modifier.weight(1f)) {
                            Txt(
                                if (g != null) g.name
                                else if (d.g != 0L && Tg.gstate == "load") "Загружаем группы…"
                                else "Выбрать группу",
                                15, 22, 500, if (g != null) C.Ink else C.Accent,
                                maxLines = 1, ellipsis = true
                            )
                            Txt(
                                if (g != null) membersTxt(g) else "куда отправлять отчёты ${MPF.getValue(mp)}",
                                13, 18, 400, C.Mute, maxLines = 1, ellipsis = true
                            )
                        }
                        Ico("right", 18.dp, C.Dim)
                    }
                    if (g != null && g.forum) {
                        MPTYPES.getValue(mp).forEach { tid ->
                            val tp = g.topics?.firstOrNull { it.id == d.topics[tid] }
                            Hr()
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .tap { st.pick = Pick("t", mp, tid) }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Tile(TYPES.getValue(tid).icon, 36.dp, 13.dp, 18.dp)
                                Txt(TYPES.getValue(tid).title, 15, 22, 500, modifier = Modifier.weight(1f))
                                Txt(
                                    tp?.name ?: "Выбрать тему", 14, 20,
                                    if (tp != null) 400 else 600,
                                    if (tp != null) C.Mute else C.Warn,
                                    Modifier.widthIn(max = 150.dp),
                                    maxLines = 1, ellipsis = true
                                )
                                Ico("right", 18.dp, C.Dim)
                            }
                        }
                    } else if (g != null) {
                        Hr()
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Txt("в группе нет тем, отчёты уйдут в общий чат", 13, 18, 400, C.Mute)
                        }
                    }
                }
            }
        }
        RBar(navInset) {
            RCta(if (done) "Готово" else "Выберите группы и темы", done) { st.setupOpen = false }
        }
    }
}

/* ===================== выбор группы / темы ===================== */

private fun setGroup(st: AppState, mp: String, g: TgGroup) {
    if (st.pickBusy) return
    val d = Cfg.dest(mp)
    val apply = {
        if (d.g != g.id) {
            d.g = g.id
            d.topics.clear()
            d.topics.putAll(autoMap(g))
        }
        Cfg.save()
        st.pick = null
    }
    if (g.forum && g.topics == null) {
        st.pickBusy = true
        Tg.loadTopics(g) { loaded ->
            st.pickBusy = false
            if (loaded) apply() else st.toast("Не получилось загрузить темы", true)
        }
    } else {
        apply()
    }
}

@Composable
fun PickerScreen(st: AppState, p: Pick, base: Dp, navInset: Dp) {
    val d = Cfg.dest(p.mp)
    var q by remember(p) { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(C.Bg).tap { }) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = base, bottom = 16.dp)
        ) {
            RTop { st.pick = null }
            if (p.kind == "g") {
                RHero("Группа для ${MPF.getValue(p.mp)}", "группы из вашего аккаунта Telegram")
                BasicTextField(
                    value = q,
                    onValueChange = { q = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = LocalOnest.current,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = C.Ink
                    ),
                    cursorBrush = SolidColor(C.Accent),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp).fillMaxWidth(),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(C.Card)
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (q.isEmpty()) Txt("поиск по группам", 15, 22, 400, C.Dim)
                            inner()
                        }
                    }
                )
                SepCard {
                    val list = Tg.groups.filter { norm(it.name).contains(norm(q)) }
                    if (Tg.gstate == "load" || Tg.gstate == "idle") {
                        InfoRow("Загружаем группы…", C.Mute)
                    } else if (Tg.gstate == "err") {
                        InfoRow("Не получилось загрузить группы", C.Warn)
                    } else if (list.isEmpty()) {
                        InfoRow("Ничего не найдено", C.Mute)
                    } else {
                        list.forEachIndexed { i, g ->
                            if (i > 0) Hr()
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .tap { setGroup(st, p.mp, g) }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Gav(g, 40.dp)
                                Column(Modifier.weight(1f)) {
                                    Txt(g.name, 15, 22, 500, maxLines = 1, ellipsis = true)
                                    Txt(membersTxt(g), 13, 18, 400, C.Mute, maxLines = 1, ellipsis = true)
                                }
                                if (d.g == g.id) Ico("check", 22.dp, C.Accent, 2.4f)
                            }
                        }
                    }
                }
            } else {
                val title = TYPES.getValue(p.tid).title.replaceFirstChar { it.lowercase() }
                RHero("Тема для отчёта", "$title · ${MPF.getValue(p.mp)}")
                val g = Tg.groupById(d.g)
                SepCard {
                    val topics = g?.topics.orEmpty()
                    if (topics.isEmpty()) InfoRow("В группе пока нет тем", C.Mute)
                    topics.forEachIndexed { i, tp ->
                        if (i > 0) Hr()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .tap {
                                    d.topics[p.tid] = tp.id
                                    Cfg.save()
                                    st.pick = null
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Txt(tp.name, 15, 22, 500, modifier = Modifier.weight(1f), maxLines = 1, ellipsis = true)
                            if (d.topics[p.tid] == tp.id) Ico("check", 22.dp, C.Accent, 2.4f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(text: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { Txt(text, 15, 22, 400, color) }
}

/* ===================== всплывающие экраны и уведомление ===================== */

@Composable
fun Overlays(st: AppState, base: Dp, navInset: Dp) {
    Slide(st.repId) { id -> ReportScreen(st, id, base, navInset) }
    Slide(if (st.setupOpen) true else null) { SetupScreen(st, base, navInset) }
    Slide(st.pick) { p -> PickerScreen(st, p, base, navInset) }
}

@Composable
fun BoxScope.ToastHost(st: AppState) {
    LaunchedEffect(st.toastKey) {
        if (st.toastKey != 0) {
            delay(2400)
            st.toastText = ""
        }
    }
    if (st.toastText.isNotEmpty()) {
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 104.dp, start = 16.dp, end = 16.dp)
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(C.Ink)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Ico(if (st.toastBad) "warn" else "check", 18.dp, Color.White, if (st.toastBad) 2.2f else 2.6f)
            Txt(st.toastText, 14, 20, 600, Color.White, maxLines = 1, ellipsis = true)
        }
    }
}
