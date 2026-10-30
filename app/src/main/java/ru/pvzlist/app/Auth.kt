package ru.pvzlist.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Корень: до входа — экран входа, после — само приложение */
@Composable
fun Root() {
    when (Tg.stage) {
        Stage.Ready -> App()
        Stage.Starting -> Box(Modifier.fillMaxSize().background(C.Bg))
        else -> AuthScreen()
    }
}

private val PhoneFormat = object : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val d = text.text
        val sb = StringBuilder()
        d.forEachIndexed { i, ch ->
            if (i == 3) sb.append(' ')
            if (i == 6 || i == 8) sb.append('-')
            sb.append(ch)
        }
        val map = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 6 -> offset + 1
                offset <= 8 -> offset + 2
                else -> offset + 3
            }

            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 7 -> offset - 1
                offset <= 10 -> offset - 2
                else -> offset - 3
            }.coerceIn(0, d.length)
        }
        return TransformedText(AnnotatedString(sb.toString()), map)
    }
}

@Composable
fun AuthScreen() {
    val step = when {
        Tg.stage == Stage.Code && !Tg.editingPhone -> 1
        Tg.stage == Stage.Password && !Tg.editingPhone -> 2
        Tg.stage == Stage.Unsupported -> 3
        else -> 0
    }
    BackHandler(enabled = step == 1 || step == 2) { Tg.editPhone() }
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))
            .padding(horizontal = 24.dp)
    ) {
        when (step) {
            0 -> PhoneStep()
            1 -> CodeStep()
            2 -> PasswordStep()
            else -> UnsupportedStep()
        }
    }
}

@Composable
private fun Header(title: String, text: String, top: Int = 56) {
    Spacer(Modifier.height(top.dp))
    Box(
        Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(C.AccentSoft),
        contentAlignment = Alignment.Center
    ) { Ico("send", 40.dp, C.Accent, 1.6f) }
    Spacer(Modifier.height(24.dp))
    Txt(title, 28, 34, 600, C.Ink, Modifier.fillMaxWidth(), align = TextAlign.Center, spacing = -0.4f)
    Spacer(Modifier.height(8.dp))
    Txt(text, 15, 21, 400, C.Mute, Modifier.fillMaxWidth(), align = TextAlign.Center)
}

@Composable
private fun BackBtn() {
    Box(
        Modifier
            .padding(top = 4.dp)
            .size(44.dp)
            .clip(RoundedCornerShape(18.dp))
            .tap { Tg.editPhone() },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(18.dp)).background(C.Card),
            contentAlignment = Alignment.Center
        ) { Ico("left", 22.dp, C.Accent) }
    }
}

@Composable
private fun Msg() {
    val text = when {
        Tg.error.isNotEmpty() -> Tg.error
        !Tg.online -> "Нет сети — подключитесь к интернету"
        else -> ""
    }
    if (text.isNotEmpty()) {
        Txt(
            text, 13, 18, 400,
            if (Tg.error.isNotEmpty()) C.Warn else C.Mute,
            Modifier.fillMaxWidth().padding(top = 18.dp),
            align = TextAlign.Center
        )
    }
}

@Composable
private fun Cta(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(top = 12.dp, bottom = 20.dp)
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (enabled) C.Accent else C.Card)
            .then(if (enabled) Modifier.tap(onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Txt(text, 18, 22, 600, if (enabled) Color.White else C.Mute)
    }
}

private val FieldStyle
    @Composable get() = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = C.Ink,
        fontFamily = LocalOnest.current
    )

/* ---------- 1. номер ---------- */

@Composable
private fun ColumnScope.PhoneStep() {
    var digits by remember { mutableStateOf(Tg.phone) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { fr.requestFocus() }
    val ok = digits.length == 10 && !Tg.busy
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header("Вход через Telegram", "Отчёты будут уходить в вашу группу\nот имени вашего аккаунта")
        Spacer(Modifier.height(32.dp))
        Row(
            Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(26.dp)).background(C.Card),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Txt("+7", 20, 26, 600, modifier = Modifier.padding(start = 20.dp, end = 16.dp))
            BasicTextField(
                value = digits,
                onValueChange = { v ->
                    var d = v.filter { it.isDigit() }
                    if (d.length > 10 && (d[0] == '7' || d[0] == '8')) d = d.drop(1)
                    digits = d.take(10)
                },
                modifier = Modifier.weight(1f).padding(end = 16.dp).focusRequester(fr),
                singleLine = true,
                textStyle = FieldStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (ok) Tg.submitPhone(digits) }),
                visualTransformation = PhoneFormat,
                cursorBrush = SolidColor(C.Accent),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (digits.isEmpty()) Txt("900 000-00-00", 20, 26, 600, C.Dim)
                        inner()
                    }
                }
            )
        }
        Msg()
    }
    Cta(if (Tg.busy) "Отправляем…" else "Получить код", ok) { Tg.submitPhone(digits) }
}

/* ---------- 2. код ---------- */

@Composable
private fun ColumnScope.CodeStep() {
    var code by remember { mutableStateOf("") }
    val n = Tg.codeLen
    val fr = remember { FocusRequester() }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { fr.requestFocus() }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    LaunchedEffect(Tg.error) { if (Tg.error.isNotEmpty()) code = "" }
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth()) { BackBtn() }
        Header(
            "Введите код",
            "Мы отправили код в приложение Telegram\nна +7 ${fmtPhoneDigits(Tg.phone)}",
            top = 20
        )
        Spacer(Modifier.height(32.dp))
        BasicTextField(
            value = code,
            onValueChange = { v ->
                val d = v.filter { it.isDigit() }.take(n)
                code = d
                if (d.length == n) Tg.submitCode(d)
            },
            modifier = Modifier.fillMaxWidth().focusRequester(fr),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = {
                Row(Modifier.fillMaxWidth().tap { fr.requestFocus() }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(n) { i ->
                        val cur = i == code.length && !Tg.busy
                        Box(
                            Modifier
                                .weight(1f)
                                .height(68.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(C.Card)
                                .then(if (cur) Modifier.border(2.dp, C.Accent, RoundedCornerShape(22.dp)) else Modifier),
                            contentAlignment = Alignment.Center
                        ) { Txt(code.getOrNull(i)?.toString() ?: "", 30, 36, 600) }
                    }
                }
            }
        )
        Msg()
        val left = ((Tg.resendAt - now + 999) / 1000).toInt()
        when {
            Tg.busy -> Txt("Проверяем код…", 13, 18, 400, C.Mute, Modifier.padding(top = 18.dp))
            !Tg.canResend -> {}
            left > 0 -> Txt(
                "Отправить код ещё раз через ${left / 60}:${p2(left % 60)}",
                13, 18, 400, C.Mute, Modifier.padding(top = 18.dp)
            )
            else -> Txt(
                "Отправить ещё раз", 13, 18, 600, C.Accent,
                Modifier.padding(top = 18.dp).tap { Tg.resend() }
            )
        }
    }
    Spacer(Modifier.height(20.dp))
}

/* ---------- 3. облачный пароль ---------- */

@Composable
private fun ColumnScope.PasswordStep() {
    var pass by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { fr.requestFocus() }
    val ok = pass.isNotEmpty() && !Tg.busy
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth()) { BackBtn() }
        Header(
            "Пароль Telegram",
            "У аккаунта включена двухэтапная проверка.\nВведите облачный пароль" +
                if (Tg.passHint.isNotBlank()) "\nПодсказка: ${Tg.passHint}" else "",
            top = 20
        )
        Spacer(Modifier.height(32.dp))
        Row(
            Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(26.dp)).background(C.Card),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = pass,
                onValueChange = { pass = it },
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp).focusRequester(fr),
                singleLine = true,
                textStyle = FieldStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { if (ok) Tg.submitPassword(pass) }),
                visualTransformation = PasswordVisualTransformation(),
                cursorBrush = SolidColor(C.Accent),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (pass.isEmpty()) Txt("Пароль", 20, 26, 600, C.Dim)
                        inner()
                    }
                }
            )
        }
        Msg()
    }
    Cta(if (Tg.busy) "Проверяем…" else "Войти", ok) { Tg.submitPassword(pass) }
}

/* ---------- нет аккаунта ---------- */

@Composable
private fun ColumnScope.UnsupportedStep() {
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(
            "Нужен аккаунт Telegram",
            "Для этого номера аккаунта нет или вход требует\nдействий, которых в приложении пока нет.\nВойдите в Telegram на телефоне и повторите"
        )
        Msg()
    }
    Cta("Назад", !Tg.leaving) { Tg.logout() }
}

/** 9038837741 -> 903 883-77-41 */
fun fmtPhoneDigits(d: String): String {
    val sb = StringBuilder()
    d.forEachIndexed { i, ch ->
        if (i == 3) sb.append(' ')
        if (i == 6 || i == 8) sb.append('-')
        sb.append(ch)
    }
    return sb.toString()
}
