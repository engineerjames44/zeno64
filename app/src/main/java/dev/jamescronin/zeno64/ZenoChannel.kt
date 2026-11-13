package dev.jamescronin.zeno64

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private sealed interface Page {
    /** One page of Zeno's news: [items] is at most 3 of them; [index] of [count] briefing pages. */
    data class Briefing(val items: List<FeedItem>, val index: Int, val count: Int) : Page
    data object Conditions : Page
    data object Outlook : Page
    data object Stats : Page
}

/**
 * Zeno's channel: the screensaver, and what the Zeno channel shows. Like the old weather channel,
 * it cycles through pages on its own, a hard cut every 10 seconds. While Zeno is on the phone,
 * the call takes over the channel (only the channel: nothing else on the TV is interrupted).
 */
@Composable
fun ZenoChannel() {
    val weather by rememberWeather()
    val stats = rememberStats()
    val feed = rememberFeed()
    val now = rememberNow()
    val items = feed?.feed?.items.orEmpty()
    val briefings = items.chunked(3).ifEmpty { listOf(emptyList()) }
    val pages = buildList<Page> {
        briefings.forEachIndexed { i, chunk -> add(Page.Briefing(chunk, i, briefings.size)) }
        if (weather != null) { add(Page.Conditions); add(Page.Outlook) }
        add(Page.Stats)
    }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            tick++
        }
    }
    val call = feed?.visibleCall(now)
    if (call != null) {
        val title = if (call.to.isEmpty()) "Zeno is" to "on the phone" else "On the phone" to "with ${call.to}"
        BroadcastPage(title, tickerLine(weather, feed, call), glanceKey = call.lines.size) { CallPanel(call, now) }
        return
    }
    val page = pages[tick % pages.size]
    val w = weather
    val title = when (page) {
        is Page.Briefing -> "Zeno's" to "Briefing"
        Page.Conditions -> "Current" to "Conditions"
        Page.Outlook -> (w?.place ?: "Local") to "Outlook"
        Page.Stats -> "TV" to "Stats"
    }
    val ticker = if (page == Page.Stats) statsTicker(stats) else tickerLine(w, feed, null)
    BroadcastPage(title, ticker, glanceKey = page) {
        when {
            page == Page.Stats -> StatsPanel(stats)
            page is Page.Briefing && page.items.isNotEmpty() && feed != null -> BriefingItems(page, feed, now)
            page is Page.Briefing || w == null -> Briefing(heardFromZeno = feed != null)
            page == Page.Conditions -> Conditions(w)
            else -> Outlook(w)
        }
    }
}

/** Nothing to show yet: Zeno, and where his news will appear. */
@Composable
private fun Briefing(heardFromZeno: Boolean) {
    Row(Modifier.fillMaxSize().padding(horizontal = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        ZenoSprite(pixel = 7.dp)
        Spacer(Modifier.width(48.dp))
        Column {
            val (a, b) = greeting()
            BasicText("$a $b.", style = tv(34.sp, Yellow))
            Spacer(Modifier.height(16.dp))
            BasicText(if (heardFromZeno) "Nothing new from Zeno." else "Nothing new from Zeno yet.", style = tv(24.sp, Paper))
            Spacer(Modifier.height(6.dp))
            BasicText("Briefings and mission results land here.", style = tv(20.sp, Dim))
        }
    }
}

/** Zeno's news, up to 3 items a page, newest first, each in its own box. */
@Composable
private fun BriefingItems(page: Page.Briefing, feed: ZenoFeedStore.State, now: Long) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Column(Modifier.weight(1f).clipToBounds(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            page.items.forEach { ItemRow(it, now) }
        }
        val age = feed.ageLabel(now)
        if (page.count > 1 || age != null) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp)) {
                if (page.count > 1) BasicText("Page ${page.index + 1} of ${page.count}", style = tv(16.sp, Dim))
                Spacer(Modifier.weight(1f))
                if (age != null) BasicText(age, style = tv(16.sp, Dim))
            }
        }
    }
}

@Composable
private fun ItemRow(item: FeedItem, now: Long) {
    Row(Modifier.fillMaxWidth().bevel(Inset, 3.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
        BasicText(item.kind.label, style = tv(18.sp, Yellow), modifier = Modifier.width(112.dp).padding(top = 4.dp))
        Column(Modifier.weight(1f)) {
            BasicText(item.title, style = tv(22.sp, Paper), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.detail.isNotEmpty()) {
                BasicText(item.detail, style = tv(17.sp, Dim), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        item.at?.let { BasicText(whenLabel(it, now), style = tv(17.sp, Dim), modifier = Modifier.padding(start = 16.dp, top = 4.dp)) }
    }
}

/** "7:33 PM" today, "Yesterday", a weekday within the week, else "Nov 9". */
private fun whenLabel(at: OffsetDateTime, now: Long): String {
    val zone = ZoneId.systemDefault()
    val local = at.atZoneSameInstant(zone)
    val days = ChronoUnit.DAYS.between(local.toLocalDate(), Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
    return when {
        days <= 0L -> local.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
        days == 1L -> "Yesterday"
        days < 7L -> local.format(DateTimeFormatter.ofPattern("EEE", Locale.US))
        else -> local.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
    }
}

/** Zeno on the phone: the call's state next to him, and the conversation so far, newest at the bottom. */
@Composable
private fun CallPanel(call: LiveCall, now: Long) {
    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier.width(240.dp).fillMaxHeight().bevel(Inset, 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ZenoSprite(pixel = 4.dp, glanceKey = call.lines.size, glanceLeft = false)
            Spacer(Modifier.height(18.dp))
            when (call.state) {
                // a blinking "Ringing", like a phone light
                CallState.RINGING -> BasicText("Ringing", style = tv(24.sp, if ((now / 1_000) % 2 == 0L) Yellow else Dim))
                CallState.LIVE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(Yellow))
                    Spacer(Modifier.width(10.dp))
                    BasicText("Live", style = tv(24.sp, Yellow))
                }
                CallState.ENDED -> BasicText("Call ended", style = tv(24.sp, Dim))
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight().bevel(Inset, 3.dp).padding(horizontal = 20.dp, vertical = 14.dp)) {
            if (call.lines.isEmpty()) {
                BasicText(
                    if (call.state == CallState.RINGING) "Waiting for them to pick up." else "Waiting for the first words.",
                    style = tv(20.sp, Dim),
                )
            } else {
                // newest at the bottom; older lines scroll off the top
                LazyColumn(reverseLayout = true, userScrollEnabled = false, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(call.lines.asReversed()) { line ->
                        Column {
                            BasicText(if (line.fromZeno) "Zeno" else "Them", style = tv(16.sp, if (line.fromZeno) Yellow else Dim))
                            BasicText(line.text, style = tv(22.sp, Paper))
                        }
                    }
                }
            }
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
            w.days.firstOrNull()?.let { Stat("Today", "High ${it.highF}°  Low ${it.lowF}°") }
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
