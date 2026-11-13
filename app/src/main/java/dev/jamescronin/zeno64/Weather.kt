package dev.jamescronin.zeno64

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** City-level location, on purpose: precise enough for weather, vague enough to be public. */
private const val PLACE = "Boston"
private const val LAT = 42.36
private const val LON = -71.06

/** One forecast day; [name] is "Today", "Tomorrow" or a weekday like "Sat". */
data class Day(val name: String, val code: Int, val highF: Int, val lowF: Int)

data class Weather(
    val place: String,
    val tempF: Int,
    val feelsF: Int,
    val humidity: Int,
    val windMph: Int,
    val windFrom: String,
    val code: Int,
    val days: List<Day>, // today first, then the next two
)

/** Current weather from Open-Meteo (free, no key). Refreshes every 15 minutes; null until the first answer. */
@Composable
fun rememberWeather(): State<Weather?> = produceState<Weather?>(null) {
    while (true) {
        val fresh = runCatching { withContext(Dispatchers.IO) { fetchWeather() } }.getOrNull()
        if (fresh != null) value = fresh
        delay(if (fresh != null) 15 * 60_000L else 60_000L)
    }
}

private fun fetchWeather(): Weather {
    val url = URL(
        "https://api.open-meteo.com/v1/forecast?latitude=$LAT&longitude=$LON" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=3" +
            "&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=America%2FNew_York",
    )
    val conn = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
    val json = conn.inputStream.bufferedReader().use { JSONObject(it.readText()) }
    val now = json.getJSONObject("current")
    val daily = json.getJSONObject("daily")
    val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.US)
    val isoDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    fun day(i: Int) = Day(
        when (i) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> dayFormat.format(isoDate.parse(daily.getJSONArray("time").getString(i))!!)
        },
        daily.getJSONArray("weather_code").getInt(i),
        Math.round(daily.getJSONArray("temperature_2m_max").getDouble(i)).toInt(),
        Math.round(daily.getJSONArray("temperature_2m_min").getDouble(i)).toInt(),
    )
    return Weather(
        place = PLACE,
        tempF = Math.round(now.getDouble("temperature_2m")).toInt(),
        feelsF = Math.round(now.getDouble("apparent_temperature")).toInt(),
        humidity = now.getInt("relative_humidity_2m"),
        windMph = Math.round(now.getDouble("wind_speed_10m")).toInt(),
        windFrom = compass(now.getDouble("wind_direction_10m")),
        code = now.getInt("weather_code"),
        days = List(daily.getJSONArray("time").length()) { day(it) },
    )
}

private fun compass(deg: Double) =
    listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[(((deg % 360) + 22.5) / 45).toInt() % 8]

/** WMO weather codes in plain words. */
fun describe(code: Int) = when (code) {
    0 -> "Clear"
    1 -> "Mostly Clear"
    2 -> "Partly Cloudy"
    3 -> "Cloudy"
    45, 48 -> "Fog"
    in 51..57 -> "Drizzle"
    in 61..67 -> "Rain"
    in 71..77 -> "Snow"
    in 80..82 -> "Showers"
    85, 86 -> "Snow Showers"
    in 95..99 -> "Thunderstorms"
    else -> "Unknown"
}

// ---- Pixel weather icons, drawn on a 12 x 12 grid like Zeno ----

private val SUN = listOf(
    ".....Y......",
    ".Y...Y...Y..",
    "..Y.....Y...",
    "....OOO.....",
    "...OYYYO....",
    "YY.OYYYO.YY.",
    "...OYYYO....",
    "....OOO.....",
    "..Y.....Y...",
    ".Y...Y...Y..",
    ".....Y......",
    "............",
)
private val CLOUD = listOf(
    "............",
    "............",
    "....WWW.....",
    "...WWWWW.WW.",
    "..WWWWWWWWWW",
    ".WWWWWWWWWWW",
    "WWWWWWWWWWWW",
    "WWWWWWWWWWWW",
    ".GGGGGGGGGG.",
    "............",
    "............",
    "............",
)
private val RAIN = listOf("............", "..B...B...B.", ".B...B...B..")
private val SNOW = listOf("............", ".S...S...S..", "...S...S...S")
private val BOLT = listOf("....LL......", "...LL.......", "....L.......")
private val FOG = listOf(
    "............", "............", "............",
    ".GGGGGGGGGG.", "............", "WWWWWWWWWWWW",
    "............", ".GGGGGGGGGG.", "............",
    "WWWWWWWWWWWW", "............", "............",
)

private val INK = mapOf(
    'Y' to Color(0xFFFFD23F), 'O' to Color(0xFFF08A24), 'W' to Paper, 'G' to Color(0xFFB9C2EE),
    'B' to Color(0xFF79C2FF), 'S' to Paper, 'L' to Yellow,
)

/** Lay [top] over [base], shifted by dx, dy. */
private fun over(base: List<String>, top: List<String>, dx: Int = 0, dy: Int = 0): List<String> {
    val g = base.map { it.toCharArray() }
    top.forEachIndexed { y, row ->
        row.forEachIndexed { x, c ->
            val gx = x + dx
            val gy = y + dy
            if (c != '.' && gy in g.indices && gx in g[gy].indices) g[gy][gx] = c
        }
    }
    return g.map { String(it) }
}

private fun iconFor(code: Int): List<String> = when (code) {
    0, 1 -> SUN
    2 -> over(SUN, CLOUD.drop(2).map { it.take(10) }, dx = 2, dy = 4)
    3 -> CLOUD
    45, 48 -> FOG
    in 51..67, in 80..82 -> over(CLOUD, RAIN, dy = 9)
    in 71..77, 85, 86 -> over(CLOUD, SNOW, dy = 9)
    in 95..99 -> over(over(CLOUD, RAIN, dy = 9), BOLT, dy = 8)
    else -> CLOUD
}

@Composable
fun WeatherIcon(code: Int, pixel: Dp, modifier: Modifier = Modifier) {
    val grid = iconFor(code)
    Canvas(modifier.size(pixel * 12)) {
        val p = pixel.toPx()
        grid.forEachIndexed { y, row ->
            row.forEachIndexed { x, c -> INK[c]?.let { drawRect(it, Offset(x * p, y * p), Size(p, p)) } }
        }
    }
}
