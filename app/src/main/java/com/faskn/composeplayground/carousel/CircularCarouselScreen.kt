package com.faskn.composeplayground.carousel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.center
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
    val totalItems = Int.MAX_VALUE
    val startIndex = totalItems / 2

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    var currentReviewer by remember { mutableStateOf<ComposeReviewer?>(null) }
    var centerItem by remember { mutableStateOf<LazyListItemInfo?>(null) }

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (visibleItems.isNotEmpty()) {
                    centerItem =
                        visibleItems.find { it.offset in 0..lazyListState.layoutInfo.viewportSize.center.x }

                    centerItem?.let { item ->
                        val rawIndex = item.index - startIndex + 1
                        val safeIndex =
                            ((rawIndex % reviewers.size) + reviewers.size) % reviewers.size
                        currentReviewer = reviewers[safeIndex]
                    }
                }
            }
    }

    val handlePageDirection: (CarouselDirection) -> Unit = { direction ->
        coroutineScope.launch {

            centerItem?.let { item ->
                val target = if (direction == CarouselDirection.NEXT) {
                    item.index + 1
                } else {
                    item.index - 1
                }

                lazyListState.animateScrollToItem(target)
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TechBlack)
            .drawBehind {
                val circleSize = screenDimensions.shortestEdge.toPx() * 1.5f
                val centerX = size.width / 2f
                val centerY = size.center.y + (circleSize / 4f)

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
        contentAlignment = Alignment.Center
    ) {
        CircularCarouselList(
            lazyListState = lazyListState,
            reviewers = reviewers,
            totalItems = totalItems,
            startIndex = startIndex,
            itemSize = carouselConfig.itemSize,
            radiusPx = radiusPx,
            density = density
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    bottom = with(density) {
                        val halfOfScreen = screenDimensions.height.toPx() / 2f
                        val bottomPaddingPx =
                            ((halfOfScreen) - (carouselConfig.radiusDp.toPx() * 2)).coerceAtLeast(0f)
                        WindowInsets.safeGestures.asPaddingValues()
                            .calculateBottomPadding() + bottomPaddingPx.toDp()
                    }
                )
        ) {
            ReviewSection(
                reviewer = currentReviewer,
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
    itemSize: Dp,
    radiusPx: Float,
    density: Density
) {
    LazyRow(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
    ) {
        items(count = totalItems, key = { it }) { index ->
            val rawIndex = index - startIndex
            val safeIndex = ((rawIndex % reviewers.size) + reviewers.size) % reviewers.size
            val reviewer = reviewers[safeIndex]

            CarouselItem(
                reviewer = reviewer,
                lazyListState = lazyListState,
                itemSize = itemSize,
                radiusPx = radiusPx,
                density = density
            )
        }
    }
}