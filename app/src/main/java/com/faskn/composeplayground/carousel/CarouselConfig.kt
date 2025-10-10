package com.faskn.composeplayground.carousel

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.faskn.composeplayground.R

const val ITEM_SIZE_MULTIPLIER = 0.33f
const val MIN_ITEM_SIZE_DP = 120
const val RADIUS_MULTIPLIER = 0.75f

enum class CarouselDirection { PREV, NEXT }

data class ScreenDimensions(
    val width: Dp,
    val height: Dp
) {
    val shortestEdge: Dp get() = minOf(width, height)
}

data class CarouselConfig(
    val itemSize: Dp,
    val radiusDp: Dp
) {
    companion object {
        fun from(dimensions: ScreenDimensions): CarouselConfig {
            // Screen width = 1x,
            // itemSize = width * 0.33x,
            // total = 3 items on screen
            val itemSize = (dimensions.width * ITEM_SIZE_MULTIPLIER)
                .coerceAtLeast(MIN_ITEM_SIZE_DP.dp)
            val radiusDp = itemSize * RADIUS_MULTIPLIER
            return CarouselConfig(itemSize, radiusDp)
        }
    }
}

fun getDevelopers() = listOf(
    ComposeDeveloper(
        id = 1,
        name = "Alex Chen",
        profileImage = R.drawable.androidify1,
        review = "\"Compose makes UI fast and fun.\""
    ),
    ComposeDeveloper(
        id = 2,
        name = "Julia Lee",
        profileImage = R.drawable.androidify2,
        review = "\"State management is super simple.\""
    ),
    ComposeDeveloper(
        id = 3,
        name = "Sarah Kim",
        profileImage = R.drawable.androidify3,
        review = "\"Animations are smooth and easy.\""
    ),
    ComposeDeveloper(
        id = 4,
        name = "Emily Watson",
        profileImage = R.drawable.androidify4,
        review = "\"Material Design looks fantastic!\""
    ),
    ComposeDeveloper(
        id = 5,
        name = "David Park",
        profileImage = R.drawable.androidify5,
        review = "\"Reusable components boost productivity.\""
    ),
    ComposeDeveloper(
        id = 6,
        name = "Marcus Rivera",
        profileImage = R.drawable.androidify6,
        review = "\"Compose lets me build Android UIs faster and with more fun!\""
    )
)
