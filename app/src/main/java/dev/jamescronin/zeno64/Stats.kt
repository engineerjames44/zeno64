package dev.jamescronin.zeno64

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

/** One group from assets/debloat-packages.txt and how many of its packages are switched off right now. */
data class BloatGroup(val name: String, val off: Int, val total: Int)

data class TvStats(
    val groups: List<BloatGroup>,
    val installed: Int,
    val disabled: Int,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val diskUsedGb: Double,
    val diskTotalGb: Double,
    val uptimeMin: Long,
    val takeoversMs: List<Int>, // newest last
    val takeoverCount: Int,
)

/** Reads the TV's numbers. Bloat state is checked live, so the page shows what's actually off now. */
fun readStats(context: Context): TvStats {
    val pm = context.packageManager
    val groups = mutableListOf<BloatGroup>()
    var name: String? = null
    var off = 0
    var total = 0
    fun close() { name?.let { groups += BloatGroup(it, off, total) } }
    context.assets.open("debloat-packages.txt").bufferedReader().forEachLine { raw ->
        val line = raw.trim()
        when {
            line.startsWith("## ") -> { close(); name = line.removePrefix("## "); off = 0; total = 0 }
            line.isEmpty() || line.startsWith("#") -> Unit
            else -> {
                total++
                val state = runCatching { pm.getApplicationEnabledSetting(line) }.getOrNull()
                if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
                    state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                ) off++
            }
        }
    }
    close()

    val apps = pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
    val mem = ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
    val disk = StatFs(Environment.getDataDirectory().path)
    val gb = 1024.0 * 1024 * 1024
    return TvStats(
        groups = groups,
        installed = apps.size,
        disabled = apps.count { !it.enabled },
        ramUsedMb = (mem.totalMem - mem.availMem) / (1024 * 1024),
        ramTotalMb = mem.totalMem / (1024 * 1024),
        diskUsedGb = (disk.totalBytes - disk.availableBytes) / gb,
        diskTotalGb = disk.totalBytes / gb,
        uptimeMin = SystemClock.elapsedRealtime() / 60_000,
        takeoversMs = Takeovers.recent(context),
        takeoverCount = Takeovers.count(context),
    )
}

/** Live stats, refreshed every 5 seconds while on screen. */
@Composable
fun rememberStats(): TvStats? {
    val context = LocalContext.current
    var stats by remember { mutableStateOf<TvStats?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            stats = runCatching { withContext(Dispatchers.IO) { readStats(context) } }.getOrNull() ?: stats
            delay(5_000)
        }
    }
    return stats
}

fun statsTicker(s: TvStats?): String = if (s == null) "COUNTING" + SEP else buildString {
    append("${s.groups.sumOf { it.off }} BLOAT APPS SWITCHED OFF")
    append(SEP + "${s.disabled} OF ${s.installed} PACKAGES DISABLED")
    append(SEP + "UP ${s.uptimeMin / 60} H ${s.uptimeMin % 60} MIN")
    if (s.takeoversMs.isNotEmpty()) append(SEP + "HOME TAKEOVER AVERAGE ${s.takeoversMs.average().toInt()} MS")
    append(SEP)
}.uppercase(Locale.US)

/** The stats page body: bloat by group, home takeover times, and the TV's memory and storage. */
@Composable
fun StatsPanel(s: TvStats?) {
    if (s == null) {
        Centered(Modifier.fillMaxSize()) { BasicText("Counting…", style = tv(26.sp, Dim)) }
        return
    }
    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            InsetBox("Bloat switched off", Modifier.weight(1.25f)) {
                // each bar is its own group, part of a whole: lit = switched off, dark = still on
                s.groups.forEach { g -> BarRow(g.name, g.off, g.total, "${g.off} of ${g.total}") }
            }
            InsetBox("This TV", Modifier.weight(1f)) {
                BarRow("Memory", s.ramUsedMb.toInt(), s.ramTotalMb.toInt(), "%.1f of %.1f GB".format(s.ramUsedMb / 1024.0, s.ramTotalMb / 1024.0))
                BarRow("Storage", (s.diskUsedGb * 10).toInt(), (s.diskTotalGb * 10).toInt(), "%.1f of %.1f GB".format(s.diskUsedGb, s.diskTotalGb))
            }
        }
        InsetBox("Home takeovers", Modifier.width(330.dp).fillMaxHeight()) {
            if (s.takeoversMs.isEmpty()) {
                BasicText("Press Home on the remote and", style = tv(18.sp, Dim))
                BasicText("Zeno will time how fast he takes over.", style = tv(18.sp, Dim))
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    BasicText("${s.takeoversMs.average().toInt()}", style = tv(44.sp, Paper))
                    BasicText(" ms average", style = tv(20.sp, Paper), modifier = Modifier.padding(bottom = 8.dp))
                }
                BasicText("Amazon's home to Zeno 64, last ${s.takeoversMs.size} takeovers (${s.takeoverCount} in all)", style = tv(16.sp, Dim))
                Spacer(Modifier.height(10.dp))
                ColumnChart(s.takeoversMs, Modifier.fillMaxWidth().weight(1f))
                // each column's time, oldest on the left, under its column
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    s.takeoversMs.forEach { v ->
                        BasicText("$v", style = tv(if (s.takeoversMs.size > 8) 12.sp else 15.sp, Paper).copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun InsetBox(title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.bevel(Inset, 3.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(title, style = tv(22.sp, Yellow))
        content()
    }
}

/** A labelled horizontal bar made of pixel blocks: lit blocks are the value, dark blocks the rest. */
@Composable
private fun BarRow(label: String, value: Int, max: Int, readout: String) {
    Row(Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(label, style = tv(18.sp, Paper), modifier = Modifier.width(128.dp))
        Canvas(Modifier.weight(1f).height(14.dp)) { blockBar(value.toFloat() / max.coerceAtLeast(1), 6.dp, 2.dp) }
        BasicText(readout, style = tv(18.sp, Paper), modifier = Modifier.width(118.dp).padding(start = 12.dp))
    }
}

private fun DrawScope.blockBar(fraction: Float, block: Dp, gap: Dp) {
    val b = block.toPx()
    val g = gap.toPx()
    val n = ((size.width + g) / (b + g)).toInt()
    val lit = (fraction.coerceIn(0f, 1f) * n).let { if (fraction > 0f && it < 1f) 1 else it.toInt() }
    for (i in 0 until n) {
        drawRect(if (i < lit) Yellow else BevelDark, Offset(i * (b + g), 0f), Size(b, size.height))
    }
}

/** Takeover times as pixel columns, oldest on the left, with the average as a dashed line. */
@Composable
private fun ColumnChart(values: List<Int>, modifier: Modifier) {
    Canvas(modifier) {
        val top = (values.maxOrNull() ?: 1).coerceAtLeast(1) * 1.15f
        val gap = 2.dp.toPx()
        val w = (size.width - gap * (values.size - 1)) / values.size
        val block = 6.dp.toPx()
        values.forEachIndexed { i, v ->
            // each column is a stack of blocks with a 2 dp gap, like the bars
            val blocks = Math.round(size.height * v / top / (block + gap)).coerceAtLeast(1)
            for (k in 0 until blocks) {
                drawRect(Yellow, Offset(i * (w + gap), size.height - (k + 1) * (block + gap) + gap), Size(w, block))
            }
        }
        val avgY = size.height - size.height * values.average().toFloat() / top
        var x = 0f
        while (x < size.width) {
            drawRect(Paper, Offset(x, avgY - 1.dp.toPx()), Size(6.dp.toPx(), 2.dp.toPx()))
            x += 10.dp.toPx()
        }
    }
}
