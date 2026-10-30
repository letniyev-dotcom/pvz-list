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

class TgTopic(val id: Long, val name: String)

/** Группа из аккаунта Telegram. Для групп с темами темы подгружаются отдельно */
class TgGroup(val id: Long, val name: String, val members: Int, val forum: Boolean) {
    var topics by mutableStateOf<List<TgTopic>?>(null)
    val colorIdx: Int get() = (id.hashCode() and 0x7fffffff) % 5
}

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
    /** Группы из аккаунта (только группы, не каналы) */
    var groups by mutableStateOf<List<TgGroup>>(emptyList())
        private set
    /** idle | load | ok | err */
    var gstate by mutableStateOf("idle")
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
            is TdApi.UpdateMessageSendSucceeded -> {
                val old = o.oldMessageId
                ui { finish(old, null) }
            }
            is TdApi.UpdateMessageSendFailed -> {
                val old = o.oldMessageId
                val text = failText(o)
                ui { finish(old, text) }
            }
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
                ui {
                    stage = Stage.Starting; userName = ""; userPhone = ""; leaving = false
                    groups = emptyList(); gstate = "idle"
                    Cfg.clearDest()
                }
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

    /* ---------- группы и темы ---------- */

    fun groupById(id: Long): TgGroup? = if (id == 0L) null else groups.firstOrNull { it.id == id }

    /** Грузим список чатов (основной и архив) и оставляем только группы — с темами и без */
    fun loadGroups() {
        if (gstate == "load") return
        val cl = client ?: return
        gstate = "load"
        loadList(cl, { TdApi.ChatListMain() }, 0) { mainIds ->
            if (mainIds == null) {
                ui { gstate = "err" }
            } else {
                loadList(cl, { TdApi.ChatListArchive() }, 0) { arch ->
                    val all = (mainIds.toList() + (arch?.toList() ?: emptyList())).distinct().take(600)
                    resolveAll(cl, all.toLongArray())
                }
            }
        }
    }

    /** Подгружаем чаты списка до конца (404 — всё загружено) и забираем их id; null — ошибка */
    private fun loadList(cl: Client, mk: () -> TdApi.ChatList, n: Int, done: (LongArray?) -> Unit) {
        val f = TdApi.LoadChats()
        f.chatList = mk()
        f.limit = 100
        cl.send(f, Client.ResultHandler { r ->
            if (r is TdApi.Ok && n < 9) {
                loadList(cl, mk, n + 1, done)
            } else {
                val g = TdApi.GetChats()
                g.chatList = mk()
                g.limit = 500
                cl.send(g, Client.ResultHandler { c ->
                    done(if (c is TdApi.Chats) c.chatIds else null)
                })
            }
        })
    }

    private fun resolveAll(cl: Client, ids: LongArray) {
        if (ids.isEmpty()) {
            ui { groups = emptyList(); gstate = "ok" }
            return
        }
        val out = arrayOfNulls<TgGroup>(ids.size)
        val left = java.util.concurrent.atomic.AtomicInteger(ids.size)
        for (i in ids.indices) {
            resolveGroup(cl, ids[i]) { g ->
                out[i] = g
                if (left.decrementAndGet() == 0) onGroups(out.filterNotNull())
            }
        }
    }

    private fun onGroups(list: List<TgGroup>) {
        ui {
            groups = list
            // для уже выбранных групп с темами сразу подгружаем темы
            val need = listOf(Cfg.ozon.g, Cfg.wb.g).distinct()
                .mapNotNull { id -> list.firstOrNull { it.id == id && it.forum } }
            if (need.isEmpty()) {
                gstate = "ok"
            } else {
                var left = need.size
                need.forEach { g ->
                    loadTopics(g) {
                        left--
                        if (left == 0) gstate = "ok"
                    }
                }
            }
        }
    }

    private fun resolveGroup(cl: Client, chatId: Long, done: (TgGroup?) -> Unit) {
        cl.send(TdApi.GetChat(chatId), Client.ResultHandler { c ->
            if (c !is TdApi.Chat) {
                done(null)
            } else {
                val t = c.type
                val title = c.title ?: ""
                if (t is TdApi.ChatTypeSupergroup && !t.isChannel) {
                    cl.send(TdApi.GetSupergroup(t.supergroupId), Client.ResultHandler { s ->
                        if (s is TdApi.Supergroup) done(TgGroup(chatId, title, s.memberCount, s.isForum)) else done(null)
                    })
                } else if (t is TdApi.ChatTypeBasicGroup) {
                    cl.send(TdApi.GetBasicGroup(t.basicGroupId), Client.ResultHandler { b ->
                        if (b is TdApi.BasicGroup && b.isActive) done(TgGroup(chatId, title, b.memberCount, false)) else done(null)
                    })
                } else {
                    done(null)
                }
            }
        })
    }

    /** Темы группы-форума. done вызывается в главном потоке */
    fun loadTopics(g: TgGroup, done: (Boolean) -> Unit) {
        val cl = client
        if (cl == null) {
            ui { done(false) }
            return
        }
        val f = TdApi.GetForumTopics()
        f.chatId = g.id
        f.query = ""
        f.limit = 100
        cl.send(f, Client.ResultHandler { r ->
            if (r is TdApi.ForumTopics) {
                val list = r.topics.map { TgTopic(topicIdOf(it.info), it.info.name ?: "") }
                ui { g.topics = list; done(true) }
            } else {
                ui { done(false) }
            }
        })
    }

    /** В разных версиях TDLib поле называется по-разному — читаем то, что есть */
    private fun topicIdOf(info: Any): Long {
        for (n in arrayOf("forumTopicId", "messageThreadId")) {
            try {
                val v = info.javaClass.getField(n).get(info)
                if (v is Number) return v.toLong()
            } catch (e: Exception) {
            }
        }
        return 0L
    }

    /* ---------- отправка отчёта ---------- */

    private val pending = HashMap<Long, (String?) -> Unit>()
    private val early = HashMap<Long, String?>()

    /**
     * Отправляем текст или фото с подписью в группу (и в тему, если она есть).
     * done(null) — доставлено, done("текст") — ошибка. Вызывается в главном потоке.
     */
    fun sendReport(chatId: Long, topicId: Long?, text: String, photoPath: String?, done: (String?) -> Unit) {
        if (photoPath == null) {
            sendOne(chatId, topicId, textContent(text), done)
            return
        }
        // длинную подпись Telegram не примет — шлём отдельным сообщением
        val long = text.length > 1000
        sendOne(chatId, topicId, photoContent(photoPath, if (long) "" else text)) { err ->
            if (err != null || !long) done(err) else sendOne(chatId, topicId, textContent(text), done)
        }
    }

    private fun fmtText(t: String): TdApi.FormattedText = TdApi.FormattedText(t, arrayOf<TdApi.TextEntity>())

    private fun textContent(t: String): TdApi.InputMessageContent {
        val c = TdApi.InputMessageText()
        c.text = fmtText(t)
        return c
    }

    private fun photoContent(path: String, caption: String): TdApi.InputMessageContent {
        val c = TdApi.InputMessagePhoto()
        c.photo = TdApi.InputFileLocal(path)
        c.caption = fmtText(caption)
        return c
    }

    private fun sendOne(chatId: Long, topicId: Long?, content: TdApi.InputMessageContent, done: (String?) -> Unit) {
        val cl = client
        if (cl == null) {
            ui { done("Нет связи с Telegram") }
            return
        }
        val f = TdApi.SendMessage()
        f.chatId = chatId
        f.inputMessageContent = content
        if (topicId != null && topicId > 1L) setTopic(f, topicId)
        cl.send(f, Client.ResultHandler { r ->
            if (r is TdApi.Message) {
                val id = r.id
                ui { watch(id, done) }
            } else if (r is TdApi.Error) {
                val text = msg(r)
                ui { done(text) }
            }
        })
    }

    private fun setNum(o: Any, name: String, v: Long) {
        val fld = o.javaClass.getField(name)
        if (fld.type == java.lang.Integer.TYPE) fld.setInt(o, v.toInt()) else fld.setLong(o, v)
    }

    /** Тема: в новых версиях topicId (MessageTopicForum), в старых messageThreadId */
    private fun setTopic(f: TdApi.SendMessage, id: Long) {
        try {
            val fld = f.javaClass.getField("topicId")
            val cls = Class.forName("org.drinkless.tdlib.TdApi\$MessageTopicForum")
            val o = cls.getConstructor().newInstance()
            setNum(o, "forumTopicId", id)
            fld.set(f, o)
            return
        } catch (e: Exception) {
        }
        try {
            setNum(f, "messageThreadId", id)
        } catch (e: Exception) {
        }
    }

    private fun watch(id: Long, cb: (String?) -> Unit) {
        if (early.containsKey(id)) {
            cb(early.remove(id))
            return
        }
        pending[id] = cb
        main.postDelayed({
            val c = pending.remove(id)
            if (c != null) c("Нет связи с Telegram")
        }, 60000L)
    }

    private fun finish(id: Long, err: String?) {
        val cb = pending.remove(id)
        if (cb != null) cb(err) else early[id] = err
    }

    private fun failText(o: TdApi.UpdateMessageSendFailed): String {
        try {
            val e = o.javaClass.getField("error").get(o)
            if (e is TdApi.Error) return msg(e)
        } catch (ex: Exception) {
        }
        try {
            val m = o.javaClass.getField("errorMessage").get(o)
            if (m is String) return friendly(m, 0)
        } catch (ex: Exception) {
        }
        return "Не получилось отправить"
    }

    private fun msg(e: TdApi.Error): String = friendly(e.message ?: "", e.code)

    private fun friendly(m: String, code: Int): String {
        return when {
            "PHONE_NUMBER_INVALID" in m -> "Неверный номер телефона"
            "PHONE_NUMBER_BANNED" in m -> "Этот номер заблокирован в Telegram"
            "PHONE_CODE_INVALID" in m -> "Неверный код"
            "PHONE_CODE_EXPIRED" in m -> "Код устарел — запросите новый"
            "PASSWORD_HASH_INVALID" in m -> "Неверный пароль"
            "WRITE_FORBIDDEN" in m || "USER_BANNED_IN_CHANNEL" in m ||
                "rights to send" in m || "write access" in m -> "В эту группу нельзя писать"
            "TOPIC_CLOSED" in m || "opic closed" in m -> "Тема закрыта"
            code == 429 || "FLOOD" in m -> {
                val n = Regex("(\\d+)").find(m)?.value
                "Слишком много попыток" + if (n != null) ", подождите $n с" else ""
            }
            else -> "Ошибка: " + m.take(80)
        }
    }
}
