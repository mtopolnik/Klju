package org.mtopol.klju.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.mtopol.klju.KljuViewModel
import org.mtopol.klju.R
import kotlin.math.abs

private const val SHEET_PAGE = 1
private const val MAX_TILT_DEGREES = 30f

/**
 * The sheet sits between two privacy covers: a swipe in either direction slides a cover
 * over it, and a swipe back reveals the sheet again.
 */
@Composable
fun KljuApp(vm: KljuViewModel) {
    val pager = rememberPagerState(initialPage = SHEET_PAGE) { 3 }
    // Keeping the neighbouring page composed preserves the sheet's state while a cover is shown.
    HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
        if (page == SHEET_PAGE) SheetScreen(vm) else PrivacyCover(tilt = { coverTilt(pager, page) })
    }
}

/**
 * The smiley's rotation on a cover [page]: it leans away from the sheet in proportion to
 * how far the cover has slid in, reaching [MAX_TILT_DEGREES] when the cover fills the screen.
 */
private fun coverTilt(pager: PagerState, page: Int): Float {
    val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
    val shown = 1f - abs(offset).coerceAtMost(1f)
    val direction = if (page < SHEET_PAGE) -1f else 1f
    return direction * MAX_TILT_DEGREES * shown
}

@Composable
private fun PrivacyCover(tilt: () -> Float) {
    Box(
        Modifier
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
