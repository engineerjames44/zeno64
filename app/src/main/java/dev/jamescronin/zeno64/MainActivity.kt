package dev.jamescronin.zeno64

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Zeno 64 is the TV's home app: Home and power-on land here.
 *
 * Another device can open a screen directly with an extra, e.g. Zeno's channel:
 *   am start -n dev.jamescronin.zeno64/.MainActivity --es screen channel
 * ("home", "channel", "stats" or "settings"; anything else is ignored).
 */
class MainActivity : ComponentActivity() {
    private enum class Screen { HOME, CHANNEL, STATS, SETTINGS }

    private var screen by mutableStateOf(Screen.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requested(intent)?.let { screen = it }
        setContent {
            var homePick by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
            var settingsPick by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
            // Back from a sub-screen returns home; Back on home does nothing (there's nowhere behind home).
            BackHandler { screen = Screen.HOME }
            when (screen) {
                Screen.HOME -> HomeScreen(
                    selected = homePick,
                    onSelect = { homePick = it },
                    onOpenZeno = { screen = Screen.CHANNEL },
                    onOpenStats = { screen = Screen.STATS },
                    onOpenSettings = { screen = Screen.SETTINGS },
                )
                Screen.CHANNEL -> ZenoChannel()
                Screen.STATS -> StatsScreen()
                Screen.SETTINGS -> SettingsScreen(settingsPick) { settingsPick = it }
            }
        }
    }

    // Pressing Home while Zeno 64 is already open brings you back to the guide; a "screen" extra picks the screen.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        screen = requested(intent) ?: if (intent.hasCategory(Intent.CATEGORY_HOME)) Screen.HOME else screen
    }

    // Remember whether Zeno 64 is on screen, so after an app update the watcher knows whether to reopen it.
    // An update closes the app (onPause with isFinishing); another app coming to the front pauses it without
    // finishing. Back never finishes it (there's nothing behind home), so finishing means an update or a kill.
    override fun onResume() {
        super.onResume()
        onScreen(this, true)
    }

    override fun onPause() {
        if (!isFinishing) onScreen(this, false)
        super.onPause()
    }

    companion object {
        private fun prefs(context: Context) = context.getSharedPreferences("zeno_ui", Context.MODE_PRIVATE)
        private fun onScreen(context: Context, value: Boolean) = prefs(context).edit().putBoolean("onScreen", value).commit()
        fun wasOnScreen(context: Context) = prefs(context).getBoolean("onScreen", false)
    }

    private fun requested(intent: Intent?): Screen? =
        when (intent?.getStringExtra("screen")) {
            "home" -> Screen.HOME
            "channel" -> Screen.CHANNEL
            "stats" -> Screen.STATS
            "settings" -> Screen.SETTINGS
            else -> null
        }
}
