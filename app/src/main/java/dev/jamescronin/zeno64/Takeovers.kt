package dev.jamescronin.zeno64

import android.content.Context

/** How long each Home takeover took (Amazon's home appearing to Zeno 64 on screen), kept for the stats page. */
object Takeovers {
    private const val PREFS = "takeovers"
    private const val KEEP = 20

    fun record(context: Context, ms: Long) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = (recent(context) + ms.toInt()).takeLast(KEEP)
        p.edit().putString("ms", list.joinToString(",")).putInt("count", p.getInt("count", 0) + 1).apply()
    }

    fun recent(context: Context): List<Int> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("ms", "")!!
            .split(",").mapNotNull { it.toIntOrNull() }

    fun count(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("count", 0)
}
