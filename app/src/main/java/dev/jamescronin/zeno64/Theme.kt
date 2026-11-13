package dev.jamescronin.zeno64

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Zeno 64's look: an early-90s cable weather channel at dusk. A sunset sky, flat blue panels lit
 * from the top-left, yellow headings, white type with a hard character-generator drop shadow,
 * faint CRT scanlines over everything, and Zeno as the station's mascot.
 *
 * Sizes are dp on a 960 x 540 dp screen (1080p at 2x). Keep ~40 dp from the edges: TVs crop.
 */

// Sky
val SkyTop = Color(0xFF1F1D6E)
val SkyMid = Color(0xFF5A3E8A)
val SkyLow = Color(0xFFE0893B)

// Panels
val Panel = Color(0xFF2E3DA6)
val PanelLit = Color(0xFF3D4FC4)      // the row the remote is on
val BevelLight = Color(0xFF8593EE)
val BevelDark = Color(0xFF12175A)
val TickerInk = Color(0xFF15185A)

// Type
val Yellow = Color(0xFFFFE14D)
val Paper = Color(0xFFF4F1E6)         // warm white, like phosphor rather than #FFF
val Dim = Color(0xFFB4BDF6)           // secondary text on the blue panels (tinted, not grey)
val ShadowInk = Color(0xFF0A0C30)

/** Jersey 15 (SIL OFL, licenses/Jersey15-OFL.txt): a chunky pixel sans close to old TV character generators. One weight. */
val Broadcast = FontFamily(Font(R.font.jersey15))

/** Jersey 15 sets small for its point size; sizes in the code are written as if it didn't. */
private const val FACE_SCALE = 1.18f

/** Broadcast type: Jersey 15 with the hard drop shadow old character generators put on everything. */
fun tv(size: TextUnit, color: Color = Paper, weight: FontWeight = FontWeight.Medium, shadow: Boolean = true) = TextStyle(
    fontFamily = Broadcast,
    fontSize = (size.value * FACE_SCALE).sp,
    fontWeight = FontWeight.Normal, // single-weight face: never fake bold
    color = color,
    letterSpacing = 0.04.em(size),
    shadow = if (shadow) Shadow(ShadowInk, Offset(4f, 4f), 0f) else null,
)

private fun Double.em(size: TextUnit): TextUnit = (size.value * this).sp

/** The dusk sky behind every screen. */
// The orange sits low but above the ticker, so the dusk is visible between the panel and the ticker.
val SkyBrush = Brush.verticalGradient(0f to SkyTop, 0.45f to SkyMid, 0.86f to SkyLow, 1f to SkyLow)

/** Faint horizontal scanlines, drawn over a whole screen. */
fun Modifier.scanlines() = drawWithContent {
    drawContent()
    val step = 3.dp.toPx()
    val line = 1.dp.toPx()
    var y = 0f
    while (y < size.height) {
        drawRect(Color.Black.copy(alpha = 0.08f), Offset(0f, y), Size(size.width, line))
        y += step
    }
}

/** A flat panel lit from the top-left: light top and left edges, dark bottom and right. */
fun Modifier.bevel(fill: Color = Panel, edge: Dp = 4.dp) = drawBehind {
    val e = edge.toPx()
    drawRect(fill)
    drawRect(BevelLight, size = Size(size.width, e))
    drawRect(BevelLight, size = Size(e, size.height))
    drawRect(BevelDark, Offset(0f, size.height - e), Size(size.width, e))
    drawRect(BevelDark, Offset(size.width - e, 0f), Size(e, size.height))
}

/** A small pixel-art arrow pointing right, drawn on a 4 x 7 grid. */
@Composable
fun PixelArrow(pixel: Dp, color: Color = Yellow) {
    val rows = listOf("X...", "XX..", "XXX.", "XXXX", "XXX.", "XX..", "X...")
    Canvas(Modifier.size(pixel * 4, pixel * 7)) {
        val p = pixel.toPx()
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, c -> if (c == 'X') drawRect(color, Offset(x * p, y * p), Size(p, p)) }
        }
    }
}

/** A box inside a panel: same bevel, slightly darker fill, for grouping (forecast days, stats). */
val Inset = Color(0xFF26338F)

@Composable
fun Centered(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) =
    Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center, content = content)
