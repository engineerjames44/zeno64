package dev.jamescronin.zeno64

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The TV trusts nothing in a feed: these pin down what gets shown and what gets dropped. */
class FeedParserTest {
    private val sample = """
        {"v": 1, "sent": "2026-11-12T20:45:00-05:00",
         "items": [
           {"kind": "call", "title": "Booked a table for 2", "detail": "Wednesday at 8pm.", "at": "2026-11-12T19:33:00-05:00"},
           {"kind": "email", "title": "1 email needs you", "detail": "", "at": "2026-11-12T18:20:00-05:00"}
         ],
         "ticker": ["Table for 2 booked", "1 email needs you"],
         "live_call": {"to": "a restaurant", "state": "live",
           "lines": [{"who": "zeno", "text": "Hi, I'm calling for James."}, {"who": "them", "text": "Sure."}]}}
    """.trimIndent()

    @Test fun readsAValidFeed() {
        val feed = FeedParser.parse(sample)!!
        assertEquals(2, feed.items.size)
        assertEquals(Kind.CALL, feed.items[0].kind)
        assertEquals("Booked a table for 2", feed.items[0].title)
        assertNotNull(feed.items[0].at)
        assertEquals(listOf("Table for 2 booked", "1 email needs you"), feed.ticker)
        assertEquals(CallState.LIVE, feed.liveCall!!.state)
        assertTrue(feed.liveCall!!.lines[0].fromZeno)
        assertFalse(feed.liveCall!!.lines[1].fromZeno)
    }

    @Test fun ignoresOtherVersionsAndBrokenJson() {
        assertNull(FeedParser.parse(sample.replace("\"v\": 1", "\"v\": 2")))
        assertNull(FeedParser.parse(sample.replace("\"v\": 1", "\"v\": \"1\"")))
        assertNull(FeedParser.parse("""{"items": []}"""))
        assertNull(FeedParser.parse("not json"))
        assertNull(FeedParser.parse("[1, 2]"))
    }

    @Test fun refusesFeedsOver8KB() {
        val big = """{"v": 1, "items": [], "ticker": ["${"a".repeat(9000)}"]}"""
        assertNull(FeedParser.parse(big))
    }

    @Test fun dropsUnknownKindsAndItemsWithoutATitle() {
        val feed = FeedParser.parse(
            """{"v": 1, "items": [{"kind": "ad", "title": "Buy now"}, {"kind": "CALL", "title": "Shouty"},
               {"kind": "note", "title": ""}, {"kind": "note"}, {"kind": "note", "title": "Kept", "extra": {"x": 1}}]}""",
        )!!
        assertEquals(listOf("Kept"), feed.items.map { it.title })
    }

    @Test fun capsCountsAndLengths() {
        val items = (1..12).joinToString(",") { """{"kind": "note", "title": "${"t".repeat(100)}", "detail": "${"d".repeat(400)}"}""" }
        val ticker = (1..10).joinToString(",") { "\"${"x".repeat(120)}\"" }
        val feed = FeedParser.parse("""{"v": 1, "items": [$items], "ticker": [$ticker]}""")!!
        assertEquals(8, feed.items.size)
        assertEquals(60, feed.items[0].title.length)
        assertEquals(200, feed.items[0].detail.length)
        assertTrue(feed.items[0].title.endsWith("..."))
        assertEquals(6, feed.ticker.size)
        assertTrue(feed.ticker.all { it.length == 80 })
    }

    @Test fun stripsLinksMarkupAndInvisibleCharacters() {
        assertEquals("Read this now", FeedParser.clean("Read https://evil.example/x this www.evil.example now", 80))
        assertEquals("Hello there", FeedParser.clean("<b>Hello</b>\nthere", 80))
        assertEquals("abc def", FeedParser.clean("abc‮​def", 80))
        assertEquals("tab and newline", FeedParser.clean("tab\tand\r\nnewline", 80))
    }

    @Test fun keepsOnlyTheLastSixCallLinesAndValidSpeakers() {
        val lines = (1..9).joinToString(",") { """{"who": "zeno", "text": "line $it"}""" } +
            """, {"who": "someone", "text": "dropped"}, {"who": "them", "text": ""}"""
        val call = FeedParser.parse("""{"v": 1, "live_call": {"to": "a shop", "state": "ringing", "lines": [$lines]}}""")!!.liveCall!!
        assertEquals((4..9).map { "line $it" }, call.lines.map { it.text })
        assertEquals(CallState.RINGING, call.state)
    }

    @Test fun ignoresACallWithAnUnknownStateOrNull() {
        assertNull(FeedParser.parse("""{"v": 1, "live_call": {"to": "x", "state": "on hold", "lines": []}}""")!!.liveCall)
        assertNull(FeedParser.parse("""{"v": 1, "live_call": null}""")!!.liveCall)
    }

    @Test fun badTimesAreDroppedNotFatal() {
        val feed = FeedParser.parse("""{"v": 1, "sent": "yesterday", "items": [{"kind": "note", "title": "T", "at": "soon"}]}""")!!
        assertNull(feed.sent)
        assertNull(feed.items[0].at)
    }
}
