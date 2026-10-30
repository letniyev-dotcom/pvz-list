package ru.pvzlist.app

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

enum class Stage { Starting, Phone, Code, Password, Unsupported, Ready }

/**
 * Настоящий вход в Telegram через TDLib (на ваших api_id / api_hash).
 * Сессия хранится в приватной папке приложения и переживает перезапуск.
 * Ключи лежат в коде — репозиторий держите приватным.
 */
object Tg {
    private const val API_ID = 39658289
    private const val API_HASH = "44174916ecfe21cdbfdd1d36df98edb5"

    var stage by mutableStateOf(Stage.Starting)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf("")
        private set
    var online by mutableStateOf(true)
        private set
    var codeLen by mutableStateOf(5)
        private set
    var canResend by mutableStateOf(false)
        private set
    var resendAt by mutableStateOf(0L)
        private set
    var passHint by mutableStateOf("")
        private set
    /** Пользователь нажал «назад» на шаге кода/пароля и меняет номер */
    var editingPhone by mutableStateOf(false)
        private set
    /** 10 цифр без +7 */
    var phone by mutableStateOf("")
        private set
    var userName by mutableStateOf("")
        private set
    var userPhone by mutableStateOf("")
        private set
    var leaving by mutableStateOf(false)
        private set

    private val main = Handler(Looper.getMainLooper())
    private var ctx: Context? = null
    private var client: Client? = null

    private fun ui(f: () -> Unit) {
        main.post { f() }
    }

    fun start(context: Context) {
        if (client != null) return
        ctx = context.applicationContext
        create()
    }

    private fun create() {
        client = Client.create(Client.ResultHandler { onUpdate(it) }, null, null)
    }

    private fun send(f: TdApi.Function<*>, cb: ((TdApi.Object) -> Unit)? = null) {
        client?.send(f, Client.ResultHandler { r ->
            if (r is TdApi.Error) ui { busy = false; leaving = false; error = msg(r) } else cb?.invoke(r)
        })
    }

    private fun onUpdate(o: TdApi.Object) {
        when (o) {
            is TdApi.UpdateAuthorizationState -> onAuth(o.authorizationState)
            is TdApi.UpdateConnectionState ->
                ui { online = o.state !is TdApi.ConnectionStateWaitingForNetwork }
            else -> {}
        }
    }

    private fun onAuth(s: TdApi.AuthorizationState) {
        when (s) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val c = ctx ?: return
                val p = TdApi.SetTdlibParameters()
                p.databaseDirectory = File(c.filesDir, "tdlib").absolutePath
                p.filesDirectory = File(c.filesDir, "tdlib-files").absolutePath
                p.databaseEncryptionKey = ByteArray(0)
                p.useFileDatabase = false
                p.useChatInfoDatabase = true
                p.useMessageDatabase = false
                p.useSecretChats = false
                p.apiId = API_ID
                p.apiHash = API_HASH
                p.systemLanguageCode = "ru"
                p.deviceModel = Build.MODEL
                p.systemVersion = Build.VERSION.RELEASE
                p.applicationVersion = "1.0"
                send(p)
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> ui {
                busy = false; editingPhone = false; stage = Stage.Phone
            }
            is TdApi.AuthorizationStateWaitCode -> {
                val info = s.codeInfo
                val len = when (val t = info.type) {
                    is TdApi.AuthenticationCodeTypeTelegramMessage -> t.length
                    is TdApi.AuthenticationCodeTypeSms -> t.length
                    is TdApi.AuthenticationCodeTypeCall -> t.length
                    else -> 5
                }
                val can = info.nextType != null && info.timeout > 0
                val at = System.currentTimeMillis() + info.timeout * 1000L
                ui {
                    busy = false; error = ""; editingPhone = false
                    codeLen = if (len in 4..8) len else 5
                    canResend = can; resendAt = at
                    stage = Stage.Code
                }
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                val hint = s.passwordHint ?: ""
                ui { busy = false; error = ""; editingPhone = false; passHint = hint; stage = Stage.Password }
            }
            is TdApi.AuthorizationStateReady -> {
                ui { busy = false; error = ""; stage = Stage.Ready }
                send(TdApi.GetMe()) { r ->
                    if (r is TdApi.User) {
                        val n = (r.firstName + " " + r.lastName).trim()
                        val ph = r.phoneNumber ?: ""
                        ui { userName = n; userPhone = ph }
                    }
                }
            }
            is TdApi.AuthorizationStateLoggingOut, is TdApi.AuthorizationStateClosing -> {}
            is TdApi.AuthorizationStateClosed -> {
                // после выхода TDLib закрывается — поднимаем новый клиент для нового входа
                client = null
                ui { stage = Stage.Starting; userName = ""; userPhone = ""; leaving = false }
                create()
            }
            else -> ui { busy = false; stage = Stage.Unsupported }
        }
    }

    fun submitPhone(digits: String) {
        if (busy || digits.length != 10) return
        phone = digits; busy = true; error = ""
        val f = TdApi.SetAuthenticationPhoneNumber()
        f.phoneNumber = "+7$digits"
        send(f)
    }

    fun submitCode(code: String) {
        if (busy || code.isEmpty()) return
        busy = true; error = ""
        val f = TdApi.CheckAuthenticationCode()
        f.code = code
        send(f)
    }

    fun submitPassword(pass: String) {
        if (busy || pass.isEmpty()) return
        busy = true; error = ""
        val f = TdApi.CheckAuthenticationPassword()
        f.password = pass
        send(f)
    }

    fun resend() {
        if (busy) return
        error = ""
        send(TdApi.ResendAuthenticationCode())
    }

    fun editPhone() { error = ""; busy = false; editingPhone = true }

    fun logout() {
        if (leaving) return
        error = ""; busy = false; leaving = true
        send(TdApi.LogOut())
    }

    private fun msg(e: TdApi.Error): String {
        val m = e.message ?: ""
        return when {
            "PHONE_NUMBER_INVALID" in m -> "Неверный номер телефона"
            "PHONE_NUMBER_BANNED" in m -> "Этот номер заблокирован в Telegram"
            "PHONE_CODE_INVALID" in m -> "Неверный код"
            "PHONE_CODE_EXPIRED" in m -> "Код устарел — запросите новый"
            "PASSWORD_HASH_INVALID" in m -> "Неверный пароль"
            e.code == 429 || "FLOOD" in m -> {
                val n = Regex("(\\d+)").find(m)?.value
                "Слишком много попыток" + if (n != null) ", подождите $n с" else ""
            }
            else -> "Ошибка: " + m.take(80)
        }
    }
}
