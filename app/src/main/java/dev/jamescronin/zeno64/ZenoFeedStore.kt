package dev.jamescronin.zeno64

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The latest feed from Zeno, kept on disk so it survives a restart, and shared live with every screen
 * (the app and the screensaver run in the same process, so a new feed shows up straight away).
 */
object ZenoFeedStore {
    /** [receivedAt] is when the TV got it; [callEndedAt] is when the TV first saw the current call as ended. */
    data class State(val feed: Feed, val receivedAt: Long, val callEndedAt: Long?)

    private val state = MutableStateFlow<State?>(null)
    val latest: StateFlow<State?> = state

    @Volatile private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val p = prefs(context)
            p.getString(KEY_JSON, null)?.let(FeedParser::parse)?.let { feed ->
                state.value = State(feed, p.getLong(KEY_RECEIVED, 0), p.getLong(KEY_ENDED, 0).takeIf { it > 0 })
            }
            loaded = true
        }
    }

    /** Keeps [json] if it's a valid version 1 feed. Returns whether it was. */
    fun accept(context: Context, json: String): Boolean {
        load(context)
        val feed = FeedParser.parse(json) ?: return false
        val now = System.currentTimeMillis()
        val before = state.value
        val endedAt = when {
            feed.liveCall?.state != CallState.ENDED -> null
            before?.feed?.liveCall?.state == CallState.ENDED -> before.callEndedAt ?: now
            else -> now
        }
        prefs(context).edit()
            .putString(KEY_JSON, json)
            .putLong(KEY_RECEIVED, now)
            .putLong(KEY_ENDED, endedAt ?: 0)
            .apply()
        state.value = State(feed, now, endedAt)
        return true
    }

    private fun prefs(context: Context) = context.getSharedPreferences("zeno_feed", Context.MODE_PRIVATE)

    private const val KEY_JSON = "json"
    private const val KEY_RECEIVED = "receivedAt"
    private const val KEY_ENDED = "callEndedAt"
}

/**
 * The call to show, if any. During a real call the Pi resends at least every 2 seconds, so a ringing or live call
 * with no new feed for 20 seconds is a dead call and is hidden. An ended call stays 30 seconds after the feed that
 * ended it, then goes, even if nothing else arrives.
 */
fun ZenoFeedStore.State.visibleCall(now: Long): LiveCall? {
    val call = feed.liveCall ?: return null
    return when (call.state) {
        CallState.ENDED -> call.takeIf { callEndedAt != null && now - callEndedAt < CALL_ENDED_SHOWN_MS }
        else -> call.takeIf { now - receivedAt < CALL_SILENT_MS }
    }
}

const val CALL_SILENT_MS = 20_000L
const val CALL_ENDED_SHOWN_MS = 30_000L

/** How old the news is, when it's more than 2 hours old; null while it's fresh. Uses `sent`, else when it arrived. */
fun ZenoFeedStore.State.ageLabel(now: Long): String? {
    val age = now - (feed.sent?.toInstant()?.toEpochMilli() ?: receivedAt)
    val hours = age / 3_600_000
    return when {
        hours < 2 -> null
        hours < 48 -> "From Zeno $hours h ago"
        else -> "From Zeno ${hours / 24} days ago"
    }
}

/** The latest feed state, updated live. */
@Composable
fun rememberFeed(): ZenoFeedStore.State? {
    val context = LocalContext.current.applicationContext
    remember { ZenoFeedStore.load(context) }
    val latest by ZenoFeedStore.latest.collectAsState()
    return latest
}

/** The current time, ticking every [periodMs], for things that expire (calls, ages). */
@Composable
fun rememberNow(periodMs: Long = 1_000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) {
        while (true) {
            delay(periodMs)
            now = System.currentTimeMillis()
        }
    }
    return now
}
