package dev.jamescronin.zeno64

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One line in a channel list. */
data class Channel(val number: Int, val name: String, val about: String, val zenoSays: String, val open: (Context) -> Unit)

/**
 * The home screen, laid out like a cable channel guide: the channels on the left,
 * Zeno and the weather on the right. Up/down moves, OK tunes in.
 */
@Composable
fun HomeScreen(selected: Int, onSelect: (Int) -> Unit, onOpenZeno: () -> Unit, onOpenStats: () -> Unit, onOpenSettings: () -> Unit) {
    val weather by rememberWeather()
    val channels = listOf(
        Channel(1, "Netflix", "Shows and films", "Popcorn time?") { openApp(it, "com.netflix.ninja") },
        Channel(2, "YouTube", "Videos", "Just one video. Sure.") { openApp(it, "com.amazon.firetv.youtube") },
        Channel(3, "Zeno", "Briefing and weather", "That's my channel.") { onOpenZeno() },
        Channel(4, "Stats", "Bloat removed, takeovers", "Numbers! My favourite.") { onOpenStats() },
        Channel(5, "Settings", "Wi-Fi, apps, remote", "Tinkering again?") { onOpenSettings() },
    )
    BroadcastPage(greeting(), tickerLine(weather), glanceKey = selected) {
        Row(Modifier.fillMaxHeight()) {
            ChannelList(channels, selected, onSelect, Modifier.width(500.dp), rowHeight = 58)
            Box(Modifier.fillMaxHeight().width(3.dp).background(BevelDark))
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ZenoSprite(pixel = 4.dp, glanceKey = selected)
                Spacer(Modifier.height(14.dp))
                BasicText(channels[selected.coerceIn(channels.indices)].zenoSays, style = tv(22.sp, Paper))
                Spacer(Modifier.height(18.dp))
                WeatherLine(weather)
            }
        }
    }
}

@Composable
fun ChannelList(
    channels: List<Channel>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    rowHeight: Int = 58,
    showAbout: Boolean = true,
) {
    val context = LocalContext.current
    val requesters = remember(channels.size) { List(channels.size) { FocusRequester() } }
    LaunchedEffect(Unit) { requesters[selected.coerceIn(requesters.indices)].requestFocus() }
    Column(modifier.fillMaxHeight().padding(horizontal = 14.dp), verticalArrangement = Arrangement.Center) {
        channels.forEachIndexed { i, ch ->
            ChannelRow(
                ch,
                Modifier
                    .focusRequester(requesters[i])
                    .onFocusChanged { if (it.isFocused) onSelect(i) },
                rowHeight,
                showAbout,
            ) { ch.open(context) }
        }
    }
}

@Composable
private fun ChannelRow(ch: Channel, modifier: Modifier, height: Int, showAbout: Boolean, onPress: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .then(if (focused) Modifier.bevel(PanelLit, 3.dp) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onPress)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(18.dp)) { if (focused) PixelArrow(2.dp) }
        val big = if (height >= 56) 28.sp else 22.sp
        BasicText(ch.number.toString(), style = tv(big, if (focused) Yellow else Dim, FontWeight.Bold), modifier = Modifier.width(36.dp))
        BasicText(ch.name, style = tv(big, if (focused) Yellow else Paper, FontWeight.Bold), modifier = Modifier.weight(1f))
        if (showAbout) BasicText(ch.about, style = tv(18.sp, if (focused) Paper else Dim))
    }
}

/** Boston 48° Cloudy, with the pixel icon. */
@Composable
fun WeatherLine(weather: Weather?) {
    if (weather == null) {
        BasicText("Weather is on its way", style = tv(18.sp, Dim))
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        WeatherIcon(weather.code, pixel = 3.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            BasicText("${weather.place}  ${weather.tempF}°", style = tv(26.sp, Yellow, FontWeight.Bold))
            BasicText(describe(weather.code), style = tv(18.sp, Paper))
        }
    }
}

/** Open another app on the TV, using its TV launcher entry if it has one. */
fun openApp(context: Context, pkg: String) {
    val pm = context.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
    if (intent == null) {
        Toast.makeText(context, "That app isn't on the TV", Toast.LENGTH_SHORT).show()
        return
    }
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
