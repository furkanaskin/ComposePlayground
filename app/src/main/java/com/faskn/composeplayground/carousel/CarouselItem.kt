package com.faskn.composeplayground.carousel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.faskn.composeplayground.ui.theme.TechBlack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CarouselItem(
    reviewer: ComposeReviewer,
    index: Int,
    lazyListState: LazyListState,
    itemSize: Dp,
    radiusPx: Float,
    density: Density
) {
    Box(
        modifier = Modifier
            .size(itemSize)
            .graphicsLayer {
                val itemInfo =
                    lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                if (itemInfo != null) {
                    val itemCenter = itemInfo.offset + itemInfo.size / 2f
                    val viewportCenter =
                        lazyListState.layoutInfo.viewportStartOffset + lazyListState.layoutInfo.viewportSize.width / 2f
                    val pageOffsetFraction =
                        ((itemCenter - viewportCenter) / (itemInfo.size.toFloat() + density.density))

                    val angleRad = pageOffsetFraction * PI * 0.25f
                    translationX = (radiusPx * sin(angleRad)).toFloat()
                    translationY = (radiusPx * (1 - cos(angleRad * 2))).toFloat()
                    alpha = 1f - abs(pageOffsetFraction).coerceIn(0f, 1f)
                }
            }
            .clip(CircleShape)
            .background(TechBlack.copy(alpha = 0.25f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = reviewer.profileImage,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.75f
                    scaleY = 1.75f
                    translationY = itemSize.toPx() * 0.25f
                },
            contentScale = ContentScale.Crop,
            contentDescription = reviewer.name
        )
    }
}

