package org.mtopol.klju.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.mtopol.klju.KljuViewModel
import org.mtopol.klju.R
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sign

private const val MAX_TILT_DEGREES = 30f

// The fling threshold and snap spring of a Compose pager, so the swipe feels like one.
private val MinFlingVelocity = 400.dp
private val SettleSpec = spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 0.001f)

/**
 * The sheet and a privacy cover alternate on an endless horizontal strip. A swipe in either
 * direction slides the one on screen off that way and brings the other in behind it, so the
 * cover both comes in and goes away with a swipe either way. As on a pager, a swipe can
 * start before the previous one has settled and carry on to the next screen.
 */
@Composable
fun KljuApp(vm: KljuViewModel) {
    // An accidental back press mid-game would drop the player out of the app, so back does nothing.
    BackHandler {}
    // Whether the cover is the front screen: the one filling at least half the view.
    var coverShown by rememberSaveable { mutableStateOf(false) }
    // The side the cover came in from: -1 for left, 1 for right. It holds until the cover is
    // entirely off screen.
    var coverSide by rememberSaveable { mutableIntStateOf(1) }
    // How far the front screen has moved off center, in screen widths: positive when it moves
    // left, bringing the other screen in from the right. It stays within ±0.5, since past
    // that the other screen becomes the front one.
    var offset by remember { mutableFloatStateOf(0f) }
    var settling by remember { mutableStateOf(false) }
    var width by remember { mutableIntStateOf(1) }
    val minFlingVelocity = with(LocalDensity.current) { MinFlingVelocity.toPx() }
    val dragState = rememberDraggableState { delta ->
        var next = offset - delta / width
        // With the sheet in front and the cover entirely off screen, the cover takes the side
        // it starts coming in from.
        if (!coverShown && next != 0f && sign(next) != sign(offset)) coverSide = sign(next).toInt()
        while (abs(next) > 0.5f) {
            next -= sign(next)
            coverShown = !coverShown
        }
        offset = next
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { width = it.width.coerceAtLeast(1) }
            .draggable(
                dragState,
                Orientation.Horizontal,
                // A touch catches the screens mid-slide.
                startDragImmediately = settling,
                onDragStopped = { velocity ->
                    val start = offset
                    // A fling goes on to the next screen in its direction; a slower release
                    // goes to the front screen.
                    val target = when {
                        velocity < -minFlingVelocity -> floor(start) + 1
                        velocity > minFlingVelocity -> ceil(start) - 1
                        else -> 0f
                    }
                    settling = true
                    try {
                        // A new drag cancels this, leaving the screens wherever the touch caught them.
                        dragState.drag {
                            var reached = start
                            animate(start, target, -velocity / width, SettleSpec) { value, _ ->
                                dragBy((reached - value) * width)
                                reached = value
                            }
                        }
                        // The target screen is now in front; this clears the rounding residue.
                        offset = 0f
                    } finally {
                        settling = false
                    }
                },
            ),
    ) {
        // A screen's horizontal position, in screen widths. The front screen moves with the
        // swipe, and the other one follows it in from the side the swipe opens up.
        fun position(front: Boolean) = if (front) -offset else (if (offset < 0) -1f else 1f) - offset

        // Both screens stay composed, so the sheet keeps its state while the cover is shown.
        Box(Modifier.fillMaxSize().graphicsLayer { translationX = position(!coverShown) * size.width }) {
            SheetScreen(vm)
        }
        PrivacyCover(
            Modifier.graphicsLayer { translationX = position(coverShown) * size.width },
            tilt = { coverTilt(coverSide, shown = if (coverShown) 1 - abs(offset) else abs(offset)) },
        )
    }
}

/**
 * The smiley's rotation when the cover came in from [side] and fills the [shown] fraction of
 * the screen. It leans toward that side, away from the sheet, reaching [MAX_TILT_DEGREES]
 * when the cover fills the screen. The cover keeps its side until it is gone, so the smiley
 * rolls back the same way whichever direction the swipe that uncovers the sheet goes.
 */
private fun coverTilt(side: Int, shown: Float) = side * MAX_TILT_DEGREES * shown

@Composable
private fun PrivacyCover(modifier: Modifier = Modifier, tilt: () -> Float) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.menacing_smiley),
            contentDescription = null,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer { rotationZ = tilt() },
        )
    }
}
