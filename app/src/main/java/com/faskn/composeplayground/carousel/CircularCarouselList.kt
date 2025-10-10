package com.faskn.composeplayground.carousel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CircularCarouselList(
    lazyListState: LazyListState,
    developers: List<ComposeDeveloper>,
    totalItems: Int,
    startIndex: Int,
    itemSize: Dp,
    visualItemSize: Dp,
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
            val actualIndex = (index - startIndex).mod(developers.size)
            val developer = developers[actualIndex]

            CarouselItem(
                developer = developer,
                lazyListState = lazyListState,
                itemSize = itemSize,
                visualItemSize = visualItemSize,
                radiusPx = radiusPx,
                density = density
            )
        }
    }
}

@Composable
fun CarouselItem(
    developer: ComposeDeveloper,
    lazyListState: LazyListState,
    itemSize: Dp,
    radiusPx: Float,
    density: Density,
    visualItemSize: Dp
) {
    var itemOffset by remember { mutableFloatStateOf(0f) }
    var itemWidth by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(itemSize)
            .onPlaced { coordinates ->
                itemOffset = coordinates.positionInParent().x
                itemWidth = coordinates.size.width.toFloat()
            }
            .graphicsLayer {
                // Get viewport and item position
                val viewportWidth = lazyListState.layoutInfo.viewportSize.width.toFloat()
                val viewportCenter = viewportWidth / 2f
                val itemCenter = itemOffset + (itemWidth / 2f)
                val distanceFromCenter = itemCenter - viewportCenter
                // Calculate offset as a fraction of item width
                val pageOffsetFraction = distanceFromCenter / (itemWidth + density.density)
                // Map offset to angle for circular effect
                val angleRad = pageOffsetFraction * PI * 0.25f

                // Normalize the visual size
                val visualScale = visualItemSize / itemSize
                scaleX = visualScale
                scaleY = visualScale

                // X: horizontal position on circle (sin)
                translationX = (radiusPx * sin(angleRad)).toFloat()
                // Y: vertical position on circle (1-cos)
                translationY = (radiusPx * (1 - cos(angleRad * 2))).toFloat()

                // Fade out items near the edge
                // simply 1f - abs(pageOffsetFraction).coerceIn(0f,1f)
                // but sometimes fraction is not exactly 1f so just round it
                alpha = (1f - abs(pageOffsetFraction)
                    .coerceIn(0f, 0.75f)
                    .times(1.34f))
            }
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = developer.profileImage,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Ignore this part, just for better visual. Use headshot images
                    scaleX = 1.75f
                    scaleY = 1.75f
                    translationY = itemSize.toPx() * 0.25f
                },
            contentScale = ContentScale.Crop,
            contentDescription = developer.name
        )
    }
}
