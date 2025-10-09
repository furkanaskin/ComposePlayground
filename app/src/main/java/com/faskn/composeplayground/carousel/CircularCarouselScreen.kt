package com.faskn.composeplayground.carousel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.faskn.composeplayground.ui.theme.CarouselGradientEnd
import com.faskn.composeplayground.ui.theme.CarouselGradientStart
import com.faskn.composeplayground.ui.theme.TechBlack
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CircularCarouselScreen() {
    val coroutineScope = rememberCoroutineScope()
    val reviewers = getReviewers()
    val configuration = LocalWindowInfo.current
    val density = LocalDensity.current

    val screenDimensions = remember(configuration, density) {
        ScreenDimensions(
            width = configuration.containerSize.width.dp / density.density,
            height = configuration.containerSize.height.dp / density.density
        )
    }

    val carouselConfig = remember(screenDimensions) { CarouselConfig.from(screenDimensions) }

    val radiusPx = with(density) { carouselConfig.radiusDp.toPx() }
    val sidePadding = (screenDimensions.width - carouselConfig.itemSize) / 2

    val totalItems = Int.MAX_VALUE
    val startIndex = totalItems / 2

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    val currentReviewer = remember {
        derivedStateOf {
            val rawIndex = lazyListState.firstVisibleItemIndex - startIndex
            val safeIndex = ((rawIndex % reviewers.size) + reviewers.size) % reviewers.size
            reviewers[safeIndex]
        }
    }

    val handlePageDirection: (CarouselDirection) -> Unit = { direction ->
        coroutineScope.launch {
            val current = lazyListState.firstVisibleItemIndex
            val target = if (direction == CarouselDirection.NEXT) current + 1 else current - 1
            lazyListState.animateScrollToItem(target)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TechBlack),
        contentAlignment = Alignment.Center
    ) {
        CircularCarouselList(
            lazyListState = lazyListState,
            reviewers = reviewers,
            totalItems = totalItems,
            startIndex = startIndex,
            sidePadding = sidePadding,
            itemSize = carouselConfig.itemSize,
            radiusPx = radiusPx,
            longestEdge = screenDimensions.longestEdge,
            density = density
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            ReviewSection(
                reviewer = currentReviewer.value,
                longestEdge = screenDimensions.longestEdge,
                onPageDirection = handlePageDirection
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CircularCarouselList(
    lazyListState: LazyListState,
    reviewers: List<ComposeReviewer>,
    totalItems: Int,
    startIndex: Int,
    sidePadding: Dp,
    itemSize: Dp,
    radiusPx: Float,
    longestEdge: Dp,
    density: Density
) {
    LazyRow(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val circleSize = with(density) { longestEdge.toPx() }
                val centerX = size.width / 2f
                val centerY = size.height / 2f + (radiusPx * CIRCLE_VERTICAL_OFFSET_MULTIPLIER)

                val gradient = Brush.verticalGradient(
                    colors = listOf(CarouselGradientStart, CarouselGradientEnd),
                    startY = centerY + circleSize / 8f,
                    endY = centerY - (circleSize * 2.5f) / 2f
                )

                drawCircle(
                    brush = gradient,
                    radius = circleSize / 2f,
                    center = Offset(centerX, centerY)
                )
            },
        contentPadding = PaddingValues(horizontal = sidePadding),
        verticalAlignment = Alignment.CenterVertically,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
    ) {
        items(count = totalItems, key = { it }) { index ->
            val actualIndex = (index - startIndex).mod(reviewers.size)
            val reviewer = reviewers[actualIndex]

            CarouselItem(
                reviewer = reviewer,
                index = index,
                lazyListState = lazyListState,
                itemSize = itemSize,
                radiusPx = radiusPx,
                density = density
            )
        }
    }
}