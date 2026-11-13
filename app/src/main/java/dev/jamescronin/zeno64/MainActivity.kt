package dev.jamescronin.zeno64

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Zeno 64 is the TV's home app: Home and power-on land here. */
class MainActivity : ComponentActivity() {
    private enum class Screen { HOME, CHANNEL, STATS, SETTINGS }

    private var screen by mutableStateOf(Screen.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    // Pressing Home while Zeno 64 is already open brings you back to the guide.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) screen = Screen.HOME
    }
}
