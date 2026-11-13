package dev.jamescronin.zeno64

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The TV's settings, reached without Amazon's home screen. Amazon's settings app has a separate
 * screen for each section, so each channel here opens one of those directly; Back returns to Zeno 64.
 */
private const val SETTINGS = "com.amazon.tv.settings.v2"

/** Name, what's inside, and how to open it. Amazon locks Display & Sounds and Preferences to its own apps;
 *  picture and sound stay on the remote's settings button, which doesn't need Amazon's home screen. */
private val sections = listOf(
    Triple("Network", "Wi-Fi and internet", ".tv.network.NetworkActivity"),
    Triple("Applications", "Installed apps, storage", ".tv.applications.ApplicationsActivity"),
    Triple("Remotes & Bluetooth", "Pair and manage", ".tv.controllers_bluetooth_devices.ControllersAndBluetoothActivity"),
    Triple("Device & Software", "About, updates, restart", ".tv.device.DeviceActivity"),
    Triple("Accessibility", "Captions, text, audio description", Settings.ACTION_ACCESSIBILITY_SETTINGS),
)

@Composable
fun SettingsScreen(selected: Int, onSelect: (Int) -> Unit) {
    val channels = sections.mapIndexed { i, (name, about, target) ->
        Channel(i + 1, name, about, about) { openSettings(it, target) }
    }
    val pick = selected.coerceIn(channels.indices)
    val ticker = "PICK A SECTION" + SEP + "BACK RETURNS TO ZENO 64" + SEP + "PICTURE AND SOUND ARE ON THE REMOTE'S SETTINGS BUTTON" + SEP
    BroadcastPage("TV" to "Settings", ticker, glanceKey = selected) {
        Row(Modifier.fillMaxHeight()) {
            ChannelList(channels, pick, onSelect, Modifier.width(500.dp), showAbout = false)
            Box(Modifier.fillMaxHeight().width(3.dp).background(BevelDark))
            // What the picked section holds, next to Zeno, so the eye doesn't cross the panel.
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ZenoSprite(pixel = 4.dp, glanceKey = selected)
                Spacer(Modifier.height(16.dp))
                BasicText(channels[pick].name, style = tv(26.sp, Yellow))
                Spacer(Modifier.height(6.dp))
                BasicText(channels[pick].about, style = tv(20.sp, Paper))
            }
        }
    }
}

private fun openSettings(context: Context, target: String) {
    val intent = if (target.startsWith(".")) {
        Intent().setComponent(ComponentName(SETTINGS, SETTINGS + target))
    } else {
        Intent(target) // a system settings action
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "That settings screen didn't open", Toast.LENGTH_SHORT).show() }
}
