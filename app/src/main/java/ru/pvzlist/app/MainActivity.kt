package ru.pvzlist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        setContent {
            val ctx = LocalContext.current
            val density = LocalDensity.current
            val onest = remember { loadOnest(ctx) }
            // Размеры шрифтов как в макете (px = dp), без влияния системного масштаба
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 1f),
                LocalOnest provides onest
            ) {
                App()
            }
        }
    }
}

@Composable
fun App() {
    val st = remember { AppState() }
    var now by remember { mutableStateOf(nowS()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = nowS()
            delay(1000)
        }
    }
    val ended = now >= END
    LaunchedEffect(ended) {
        if (ended) {
            st.open = false
            st.done.clear()
        }
    }

    val base = maxOf(WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), 50.dp)
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val screen = if (st.balance) Scr.Balance else when (st.tab) {
        Tab.List -> Scr.Today
        Tab.Shifts -> Scr.Shifts
        Tab.Profile -> Scr.Profile
    }

    Column(Modifier.fillMaxSize().background(C.Bg)) {
        when (screen) {
            Scr.Today -> Head(
                base, "Сегодня",
                if (st.open) "смена открыта в ${st.at}" else "смена 09:00–21:00"
            )
            Scr.Shifts -> Head(base, "Смены") {
                BalancePill(base) { st.tab = Tab.Shifts; st.balance = true }
            }
            Scr.Balance -> Head(base, "Баланс") {
                BackButton(base) { st.balance = false }
            }
            Scr.Profile -> Head(base, "Профиль")
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val viewH = maxHeight
            val scroll = remember(screen, st.open, st.month) { ScrollState(0) }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(top = 12.dp)
            ) {
                when (screen) {
                    Scr.Today -> if (!st.open) StartContent(st, ended, viewH) else TodayList(st, now)
                    Scr.Shifts -> ShiftsContent(st)
                    Scr.Balance -> BalanceContent()
                    Scr.Profile -> ProfileContent()
                }
            }
        }

        BottomBar(st, ended, navInset) {
            st.open = true
            st.at = hm(nowS())
        }
    }
}
