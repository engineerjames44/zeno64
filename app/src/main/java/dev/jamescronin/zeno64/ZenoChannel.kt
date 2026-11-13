package dev.jamescronin.zeno64

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class Page { BRIEFING, CONDITIONS, OUTLOOK, STATS }

/**
 * Zeno's channel: the screensaver, and what the Zeno channel shows. Like the old weather channel,
 * it cycles through pages on its own, a hard cut every 10 seconds.
 */
@Composable
fun ZenoChannel() {
    val weather by rememberWeather()
    val stats = rememberStats()
    val pages = buildList {
        add(Page.BRIEFING)
        if (weather != null) { add(Page.CONDITIONS); add(Page.OUTLOOK) }
        add(Page.STATS)
    }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            tick++
        }
    }
    val page = pages[tick % pages.size]
    val w = weather
    val title = when (page) {
        Page.BRIEFING -> "Zeno's" to "Briefing"
        Page.CONDITIONS -> "Current" to "Conditions"
        Page.OUTLOOK -> (w?.place ?: "Local") to "Outlook"
        Page.STATS -> "TV" to "Stats"
    }
    val ticker = if (page == Page.STATS) statsTicker(stats) else tickerLine(w)
    BroadcastPage(title, ticker, glanceKey = page) {
        when {
            page == Page.STATS -> StatsPanel(stats)
            page == Page.BRIEFING || w == null -> Briefing()
            page == Page.CONDITIONS -> Conditions(w)
            else -> Outlook(w)
        }
    }
}

@Composable
private fun Briefing() {
    Row(Modifier.fillMaxSize().padding(horizontal = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        ZenoSprite(pixel = 7.dp)
        Spacer(Modifier.width(48.dp))
        Column {
            val (a, b) = greeting()
            BasicText("$a $b.", style = tv(34.sp, Yellow))
            Spacer(Modifier.height(16.dp))
            BasicText("Nothing new from Zeno yet.", style = tv(24.sp, Paper))
            Spacer(Modifier.height(6.dp))
            BasicText("Briefings and mission results land here.", style = tv(20.sp, Dim))
        }
    }
}

@Composable
private fun Conditions(w: Weather) {
    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier.width(300.dp).fillMaxHeight().bevel(Inset, 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText("${w.tempF}°", style = tv(72.sp, Paper))
            BasicText(describe(w.code), style = tv(26.sp, Paper))
            Spacer(Modifier.height(12.dp))
            WeatherIcon(w.code, pixel = 6.dp)
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().bevel(Inset, 3.dp).padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            BasicText(w.place, style = tv(28.sp, Yellow))
            Stat("Feels like", "${w.feelsF}°")
            Stat("Humidity", "${w.humidity}%")
            Stat("Wind", "${w.windFrom} ${w.windMph} mph")
            Stat("Today", "High ${w.days[0].highF}°  Low ${w.days[0].lowF}°")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        BasicText("$label:", style = tv(26.sp, Paper), modifier = Modifier.weight(1f))
        BasicText(value, style = tv(26.sp, Paper))
    }
}

/** The extended forecast: one inset box per day, like the old three-day page. */
@Composable
private fun Outlook(w: Weather) {
    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        w.days.take(3).forEach { d ->
            Column(
                Modifier.weight(1f).fillMaxHeight().bevel(Inset, 3.dp).padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                BasicText(d.name, style = tv(30.sp, Yellow))
                WeatherIcon(d.code, pixel = 6.dp)
                BasicText(describe(d.code), style = tv(24.sp, Paper))
                Row {
                    BasicText("Hi ", style = tv(22.sp, Dim))
                    BasicText("${d.highF}°", style = tv(28.sp, Paper))
                    Spacer(Modifier.width(20.dp))
                    BasicText("Lo ", style = tv(22.sp, Dim))
                    BasicText("${d.lowF}°", style = tv(28.sp, Paper))
                }
            }
        }
    }
}

/** The stats page on its own, for the Stats channel. */
@Composable
fun StatsScreen() {
    val stats = rememberStats()
    BroadcastPage("TV" to "Stats", statsTicker(stats), glanceKey = stats?.takeoverCount) { StatsPanel(stats) }
}
