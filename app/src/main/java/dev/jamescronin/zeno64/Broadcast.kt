package dev.jamescronin.zeno64

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Ticker separator. Jersey 15 has the bullet, so it stays in the same face. */
const val SEP = "   •   "

/**
 * Every Zeno 64 screen is one broadcast page: a header (Zeno's badge, a two-line title, the clock),
 * one big bevelled panel, and a ticker along the bottom, all sitting in the dusk sky.
 */
@Composable
fun BroadcastPage(
    title: Pair<String, String>,
    ticker: String,
    glanceKey: Any? = null,
    panel: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(SkyBrush).scanlines()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 24.dp).height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Zeno's badge: the station logo box
            Centered(Modifier.size(width = 76.dp, height = 64.dp).bevel(edge = 3.dp)) {
                ZenoSprite(pixel = 1.5.dp, glanceKey = glanceKey, glanceLeft = false)
            }
            Spacer(Modifier.width(18.dp))
            // Two-line page title, the way the old channel stacked "Current / Conditions"
            Column {
                BasicText(title.first, style = tv(26.sp, Yellow))
                BasicText(title.second, style = tv(26.sp, Yellow))
            }
            Spacer(Modifier.weight(1f))
            Clock()
        }
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.padding(horizontal = 40.dp).fillMaxWidth().weight(1f).bevel(),
            content = panel,
        )
        Spacer(Modifier.height(24.dp)) // a band of dusk between panel and ticker
        Ticker(ticker)
        Spacer(Modifier.height(14.dp)) // keep the ticker clear of the screen edge
    }
}

/** Time and date, top right. Ticks once a second. */
@Composable
fun Clock() {
    val time = remember { SimpleDateFormat("h:mm:ss a", Locale.US) }
    val date = remember { SimpleDateFormat("EEE MMM d", Locale.US) }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000 - System.currentTimeMillis() % 1000)
        }
    }
    Column(horizontalAlignment = Alignment.End) {
        BasicText(time.format(now), style = tv(24.sp, Paper).copy(textAlign = TextAlign.End))
        BasicText(date.format(now).uppercase(Locale.US), style = tv(24.sp, Paper).copy(textAlign = TextAlign.End))
    }
}

@Composable
private fun Ticker(text: String) {
    Row(Modifier.fillMaxWidth().height(44.dp).background(TickerInk), verticalAlignment = Alignment.CenterVertically) {
        Centered(Modifier.padding(start = 40.dp).fillMaxHeight().background(Yellow).padding(horizontal = 14.dp)) {
            BasicText("ZENO", style = tv(20.sp, TickerInk, shadow = false))
        }
        Box(Modifier.weight(1f).padding(start = 18.dp, end = 40.dp)) {
            // One continuous crawl: no gap or pause between loops (the text ends with its own separator).
            BasicText(
                text,
                style = tv(20.sp, Paper),
                modifier = Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    repeatDelayMillis = 0,
                    initialDelayMillis = 0,
                    spacing = MarqueeSpacing(0.dp),
                    velocity = 60.dp,
                ),
            )
        }
    }
}

fun greeting(): Pair<String, String> = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning," to "James"
    in 12..16 -> "Good afternoon," to "James"
    else -> "Good evening," to "James"
}

/** What the ticker says: greeting, weather when we have it, and that Zeno is around. */
fun tickerLine(weather: Weather?): String = buildString {
    append("${greeting().first} ${greeting().second}".uppercase(Locale.US))
    if (weather != null) {
        append(SEP + "${weather.place} ${weather.tempF}° ${describe(weather.code)}".uppercase(Locale.US))
        append(SEP + "TOMORROW ${describe(weather.days[1].code)}, HIGH ${weather.days[1].highF}°".uppercase(Locale.US))
    }
    append(SEP + "NOTHING NEW FROM ZENO YET" + SEP)
}
