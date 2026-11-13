package dev.jamescronin.zeno64

import android.accessibilityservice.AccessibilityService
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * The HOME takeover. Fire OS won't let an app catch the Home button, so instead this watches for
 * Amazon's home screen to appear and does two things straight away:
 *  1. draws a Zeno 64 cover over the whole screen (an overlay shows in a frame or two), then
 *  2. opens Zeno 64 (via an alarm, see redirect()), and removes the cover once Zeno 64 is on screen.
 *
 * Turned on and off with adb (see the README commands); it does nothing until then.
 */
class HomeWatcher : AccessibilityService() {
    private val main = Handler(Looper.getMainLooper())
    private var cover: View? = null
    private val recentRedirects = ArrayDeque<Long>()
    private var pausedUntil = 0L
    private var lastRedirect = 0L
    private var redirectAt = 0L      // when the current takeover started, for the stats page
    private var weMuted = false
    private var pendingOpen: PendingIntent? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: ""

        when {
            pkg == packageName -> {
                // Zeno 64 is up: note how long it took, then lift the cover
                if (redirectAt > 0) Takeovers.record(this, SystemClock.elapsedRealtime() - redirectAt)
                redirectAt = 0
                pendingOpen = null
                hideCover()
            }
            // Another app came to the front before Zeno 64 opened (say the Pi pressed Home, then opened
            // Netflix): stand down, so Zeno 64 doesn't jump on top of it.
            cover != null && pkg != AMAZON_LAUNCHER && isApp(pkg) -> cancelRedirect()
            pkg == AMAZON_LAUNCHER && cls.startsWith("$AMAZON_LAUNCHER.ui.HomeActivity") -> redirect(timed = true)
        }
    }

    private fun isApp(pkg: String) =
        packageManager.getLeanbackLaunchIntentForPackage(pkg) != null || packageManager.getLaunchIntentForPackage(pkg) != null

    private fun cancelRedirect() {
        pendingOpen?.let { getSystemService(AlarmManager::class.java).cancel(it) }
        pendingOpen = null
        redirectAt = 0
        Log.i(TAG, "another app came to the front first, not opening Zeno 64")
        hideCover()
    }

    private fun redirect(timed: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (now < pausedUntil) return
        // Amazon's home sends a few "opened" events per Home press: only act on the first.
        if (cover != null || now - lastRedirect < 1_500) return
        lastRedirect = now

        // Safety: if we keep bouncing (e.g. Zeno 64 is crashing), back off and let Amazon's home show.
        recentRedirects.addLast(now)
        while (recentRedirects.isNotEmpty() && now - recentRedirects.first() > 15_000) recentRedirects.removeFirst()
        if (recentRedirects.size > 4) {
            Log.w(TAG, "too many redirects, pausing for 60 s")
            pausedUntil = now + 60_000
            recentRedirects.clear()
            hideCover()
            return
        }

        Log.i(TAG, "Amazon home appeared, covering and opening Zeno 64")
        showCover()
        muteAmazon()
        redirectAt = if (timed) now else 0
        // Android holds back normal app launches for ~5 s after Home is pressed ("app switch
        // protection"). Launches fired by an alarm come from the system itself and skip that wait,
        // so we set an alarm for "right now" that opens Zeno 64.
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .putExtra("screen", "home"), // Home always lands on the guide
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        pendingOpen = open
        getSystemService(AlarmManager::class.java)
            .setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), open)
        // Never leave the cover up for long, whatever happens.
        main.postDelayed({ hideCover() }, 8_000)
    }

    private fun showCover() {
        if (cover != null) return
        val wm = getSystemService(WindowManager::class.java)
        val view = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF2B2A8C.toInt(), 0xFFE08A3C.toInt()),
            )
            val sprite = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.zeno_idle))
                .apply { setFilterBitmap(false) } // crisp pixels
            addView(
                ImageView(context).apply { setImageDrawable(sprite); scaleType = ImageView.ScaleType.FIT_CENTER },
                FrameLayout.LayoutParams(384, 384, Gravity.CENTER),
            )
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE,
        )
        wm.addView(view, params)
        cover = view
    }

    private fun hideCover() {
        val view = cover ?: return unmute()
        cover = null
        main.removeCallbacksAndMessages(null)
        // Wait until Zeno 64 has faded in underneath, so there's no black gap between cover and app.
        main.postDelayed({
            getSystemService(WindowManager::class.java).removeView(view)
            unmute()
        }, 450)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "watcher connected")
        // At power-on Amazon's home is already up before we connect, so no "opened" event will come: open
        // Zeno 64 straight away. The watcher also reconnects after an app update, and then only reopen Zeno 64 if
        // it was the app on screen, so an update never pulls anyone out of Netflix. (Not timed: not a Home press.)
        val justBooted = SystemClock.elapsedRealtime() < 5 * 60_000
        if (justBooted || MainActivity.wasOnScreen(this)) redirect(timed = false)
    }

    /**
     * Amazon's home can autoplay trailers with sound. It's only behind the cover for a moment, but mute
     * media audio until Zeno 64 is up anyway, so nothing ever plays out loud. Never touches a mute
     * James set himself.
     */
    private fun muteAmazon() {
        val audio = getSystemService(AudioManager::class.java)
        if (weMuted || audio.isStreamMute(AudioManager.STREAM_MUSIC)) return
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
        weMuted = true
    }

    private fun unmute() {
        if (!weMuted) return
        getSystemService(AudioManager::class.java).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
        weMuted = false
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        cover?.let { getSystemService(WindowManager::class.java).removeView(it) }
        cover = null
        unmute()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "Zeno64Home"
        const val AMAZON_LAUNCHER = "com.amazon.tv.launcher"
    }
}
