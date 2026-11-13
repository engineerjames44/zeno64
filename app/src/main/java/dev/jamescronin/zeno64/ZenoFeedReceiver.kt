package dev.jamescronin.zeno64

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log

/**
 * Where Zeno's news arrives. The Pi sends it over adb:
 *
 *   am broadcast -a dev.jamescronin.zeno64.ZENO_FEED -n dev.jamescronin.zeno64/.ZenoFeedReceiver --es feed <base64>
 *
 * The manifest protects this receiver with android.permission.DUMP, which only adb shell and the system hold,
 * so no app on the TV can send Zeno 64 a fake feed. Nothing from the feed is logged.
 */
class ZenoFeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val encoded = intent.getStringExtra("feed")
        if (encoded == null || encoded.length > MAX_BASE64) {
            Log.w(TAG, "ignored a feed: missing or too long")
            return
        }
        val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
        if (bytes == null || bytes.size > FeedParser.MAX_BYTES) {
            Log.w(TAG, "ignored a feed: not base64, or over ${FeedParser.MAX_BYTES} bytes")
            return
        }
        val ok = ZenoFeedStore.accept(context.applicationContext, String(bytes, Charsets.UTF_8))
        Log.i(TAG, if (ok) "feed received (${bytes.size} bytes)" else "ignored a feed: not valid version 1 JSON")
        resultCode = if (ok) 1 else 0
    }

    companion object {
        const val ACTION = "dev.jamescronin.zeno64.ZENO_FEED"
        private const val TAG = "Zeno64Feed"
        private const val MAX_BASE64 = (FeedParser.MAX_BYTES / 3 + 1) * 4 + 16
    }
}
