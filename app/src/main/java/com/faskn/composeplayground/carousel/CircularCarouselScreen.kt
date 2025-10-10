package com.faskn.composeplayground.carousel

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.faskn.composeplayground.ui.theme.CarouselGradientEnd
import com.faskn.composeplayground.ui.theme.CarouselGradientStart
import com.faskn.composeplayground.ui.theme.TechBlack
import kotlinx.coroutines.launch

@Composable
fun CircularCarouselScreen() {
    val coroutineScope = rememberCoroutineScope()
    val developers = getDevelopers()
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val screenDimensions = remember(windowInfo, density) {
        ScreenDimensions(
            width = windowInfo.containerSize.width.dp / density.density,
            height = windowInfo.containerSize.height.dp / density.density
        )
    }

    val carouselConfig = remember(screenDimensions) { CarouselConfig.from(screenDimensions) }
    val radiusPx = with(density) { carouselConfig.radiusDp.toPx() }
    val totalItems = Int.MAX_VALUE
    val startIndex = totalItems / 2

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    var currentDeveloper by remember { mutableStateOf<ComposeDeveloper?>(null) }
    var centerItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val snapTolerancePx = carouselConfig.itemSize.value
    val snapToleranceRange = -snapTolerancePx..snapTolerancePx
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    LaunchedEffect(configuration.orientation) {
        // On orientation change, scroll to the center item to avoid misplacement
        lazyListState.scrollToItem(index = centerItemIndex ?: startIndex)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.layoutInfo }
            .collect { layoutInfo ->
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val centerItem = visibleItems.find { it.offset.toFloat() in snapToleranceRange }
                    centerItemIndex = centerItem?.index

                    centerItem?.let { item ->
                        val rawIndex = item.index - startIndex + 1
                        val safeIndex = rawIndex.mod(developers.size)
                        currentDeveloper = developers[safeIndex]
                    }
                }
            }
    }

    val handlePageDirection: (CarouselDirection) -> Unit = { direction ->
        centerItemIndex?.let { index ->
            coroutineScope.launch {
                val target = when (direction) {
                    CarouselDirection.NEXT -> index + 1
                    CarouselDirection.PREV -> index - 1
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
            }
    ) {
        HeaderText(isPortrait)

        CircularCarouselList(
            lazyListState = lazyListState,
            developers = developers,
            totalItems = totalItems,
            startIndex = startIndex,
            itemSize = carouselConfig.itemSize,
            visualItemSize = carouselConfig.visualItemSize,
            radiusPx = radiusPx,
            density = density
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    bottom = with(density) {
                        /*
                        * Place the review section below the carousel items
                        * If there is not enough space, set it to zero
                        * Use safe area insets for better UI
                        */
                        val halfOfScreen = screenDimensions.height.toPx() / 2f
                        val bottomPaddingPx =
                            (halfOfScreen - (carouselConfig.visualItemSize.toPx().div(2)))
                                .div(density.density)
                                .coerceAtLeast(0f)
                        WindowInsets.safeGestures.asPaddingValues()
                            .calculateBottomPadding() + bottomPaddingPx.toDp()
                    }
                )
        ) {
            ReviewSection(
                developer = currentDeveloper,
                onNavigationEvent = handlePageDirection
            )
        }

        ActionButton(isPortrait)
    }
}