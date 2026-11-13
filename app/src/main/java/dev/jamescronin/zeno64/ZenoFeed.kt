package dev.jamescronin.zeno64

import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

/*
 * What Zeno, on the Raspberry Pi, last told the TV. Version 1 of the link: the Pi sends this JSON over adb
 * (see ZenoFeedReceiver). Everything here is plain text. Anything unknown is ignored, limits are enforced
 * again on the TV, and links, markup and invisible characters are stripped, whatever the Pi sends.
 */

data class Feed(
    val sent: OffsetDateTime?,
    val items: List<FeedItem>,   // newest first, at most 8
    val ticker: List<String>,    // at most 6
    val liveCall: LiveCall?,
)

data class FeedItem(val kind: Kind, val title: String, val detail: String, val at: OffsetDateTime?)

enum class Kind(val label: String) {
    BRIEFING("Briefing"), CALL("Call"), MISSION("Mission"), ROUTINE("Routine"),
    EMAIL("Email"), CALENDAR("Calendar"), NOTE("Note"),
}

data class LiveCall(val to: String, val state: CallState, val lines: List<CallLine>)

enum class CallState { RINGING, LIVE, ENDED }

data class CallLine(val fromZeno: Boolean, val text: String)

object FeedParser {
    const val MAX_BYTES = 8 * 1024

    /** The feed, or null if it isn't a valid version 1 feed. */
    fun parse(json: String): Feed? {
        if (json.toByteArray(Charsets.UTF_8).size > MAX_BYTES) return null
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val v = root.opt("v")
        if (v !is Int || v != 1) return null

        val items = objects(root.optJSONArray("items")).mapNotNull(::item).take(8)
        val ticker = strings(root.optJSONArray("ticker")).map { clean(it, 80) }.filter { it.isNotEmpty() }.take(6)
        return Feed(time(root.opt("sent")), items, ticker, (root.opt("live_call") as? JSONObject)?.let(::call))
    }

    private fun item(o: JSONObject): FeedItem? {
        val kind = Kind.entries.firstOrNull { it.name.lowercase() == o.opt("kind") } ?: return null
        val title = clean(o.opt("title") as? String ?: return null, 60).ifEmpty { return null }
        return FeedItem(kind, title, clean(o.opt("detail") as? String ?: "", 200), time(o.opt("at")))
    }

    private fun call(o: JSONObject): LiveCall? {
        val state = when (o.opt("state")) {
            "ringing" -> CallState.RINGING
            "live" -> CallState.LIVE
            "ended" -> CallState.ENDED
            else -> return null
        }
        val lines = objects(o.optJSONArray("lines")).mapNotNull { l ->
            val fromZeno = when (l.opt("who")) {
                "zeno" -> true
                "them" -> false
                else -> return@mapNotNull null
            }
            clean(l.opt("text") as? String ?: "", 200).takeIf { it.isNotEmpty() }?.let { CallLine(fromZeno, it) }
        }.takeLast(6)
        return LiveCall(clean(o.opt("to") as? String ?: "", 40), state, lines)
    }

    private fun objects(a: JSONArray?): List<JSONObject> =
        if (a == null) emptyList() else (0 until a.length()).mapNotNull { a.opt(it) as? JSONObject }

    private fun strings(a: JSONArray?): List<String> =
        if (a == null) emptyList() else (0 until a.length()).mapNotNull { a.opt(it) as? String }

    private fun time(value: Any?): OffsetDateTime? =
        (value as? String)?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }

    private val LINK = Regex("""(?i)\b(?:[a-z][a-z0-9+.-]*://|www\.)\S*""")
    private val TAG = Regex("""</?[A-Za-z][^<>]{0,60}>""")
    private val INVISIBLE = Regex("""[\p{Cc}\p{Cf}]""") // control characters, bidi overrides, zero-width marks
    private val SPACES = Regex("""\s+""")

    /** Plain text only: no links, no tags, no invisible characters, one line, at most [max] characters. */
    fun clean(raw: String, max: Int): String {
        var s = raw.replace(LINK, " ").replace(TAG, " ").replace(INVISIBLE, " ").replace(SPACES, " ").trim()
        if (s.length > max) {
            s = s.take(max - 3).let { if (it.isNotEmpty() && it.last().isHighSurrogate()) it.dropLast(1) else it }
                .trimEnd() + "..."
        }
        return s
    }
}
