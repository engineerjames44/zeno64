package dev.jamescronin.zeno64

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Zeno's hand-drawn 32x32 frames (made by tools/zeno_sprite.py). */
private enum class Frame { IDLE, BLINK, LOOK_RIGHT, LOOK_LEFT }

/**
 * Zeno, idling. Blinks every few seconds, now and then glances to the side,
 * and bobs one sprite pixel up and down so he looks alive.
 * [pixel] is how big one sprite pixel is on screen: the sprite is drawn at 32 * pixel.
 * Changing [glanceKey] makes him glance towards [glanceLeft]'s side, e.g. at what the remote just picked.
 */
@Composable
fun ZenoSprite(
    pixel: Dp = 8.dp,
    modifier: Modifier = Modifier,
    glanceKey: Any? = null,
    glanceLeft: Boolean = true,
) {
    val frames = mapOf(
        Frame.IDLE to ImageBitmap.imageResource(R.drawable.zeno_idle),
        Frame.BLINK to ImageBitmap.imageResource(R.drawable.zeno_blink),
        Frame.LOOK_RIGHT to ImageBitmap.imageResource(R.drawable.zeno_look),
        Frame.LOOK_LEFT to ImageBitmap.imageResource(R.drawable.zeno_look_left),
    )
    var frame by remember { mutableStateOf(Frame.IDLE) }
    var bob by remember { mutableIntStateOf(0) }

    // Eyes: mostly open, blink every 2.5-5 s, sometimes look around for a moment.
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2500, 5000))
            if (Random.nextInt(4) == 0) {
                frame = if (Random.nextBoolean()) Frame.LOOK_RIGHT else Frame.LOOK_LEFT
                delay(1200)
            }
            frame = Frame.BLINK
            delay(130)
            frame = Frame.IDLE
        }
    }

    // He notices when you move: glance at the new pick, then blink back to idle.
    LaunchedEffect(glanceKey) {
        if (glanceKey == null) return@LaunchedEffect
        frame = if (glanceLeft) Frame.LOOK_LEFT else Frame.LOOK_RIGHT
        delay(900)
        frame = Frame.BLINK
        delay(110)
        frame = Frame.IDLE
    }

    // Breathing: hop one sprite pixel up and back, in steps rather than smoothly, like an old console.
    LaunchedEffect(Unit) {
        while (true) {
            delay(700)
            bob = if (bob == 0) -1 else 0
        }
    }

    Image(
        bitmap = frames.getValue(frame),
        contentDescription = "Zeno",
        filterQuality = FilterQuality.None, // keep pixels crisp when scaled up
        modifier = modifier
            .size(pixel * 32)
            .offset(y = pixel * bob),
    )
}
