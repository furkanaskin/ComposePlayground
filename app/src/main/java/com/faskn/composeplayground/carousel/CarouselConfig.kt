package com.faskn.composeplayground.carousel

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.faskn.composeplayground.R

const val ITEM_SIZE_MULTIPLIER = 0.33f
const val MIN_ITEM_SIZE_DP = 120
const val RADIUS_MULTIPLIER = 0.75f
const val CIRCLE_VERTICAL_OFFSET_MULTIPLIER = 3f

enum class CarouselDirection { PREV, NEXT }


data class ScreenDimensions(
    val width: Dp,
    val height: Dp
) {
    val shortestEdge: Dp get() = minOf(width, height)
    val longestEdge: Dp get() = maxOf(width, height)
}

data class CarouselConfig(
    val itemSize: Dp,
    val radiusDp: Dp
) {
    companion object {
        fun from(dimensions: ScreenDimensions): CarouselConfig {
            val itemSize = (dimensions.shortestEdge * ITEM_SIZE_MULTIPLIER)
                .coerceAtLeast(MIN_ITEM_SIZE_DP.dp)
            val radiusDp = itemSize * RADIUS_MULTIPLIER
            return CarouselConfig(itemSize, radiusDp)
        }
    }
}

fun getReviewers() = listOf(
    ComposeReviewer(
        id = 1,
        name = "Alex Chen",
        profileImage = R.drawable.androidify1,
        review = "Compose makes UI fast and fun."
    ),
    ComposeReviewer(
        id = 2,
        name = "Marcus Rivera",
        profileImage = R.drawable.androidify2,
        review = "State management is super simple."
    ),
    ComposeReviewer(
        id = 3,
        name = "Sarah Kim",
        profileImage = R.drawable.androidify3,
        review = "Animations are smooth and easy."
    ),
    ComposeReviewer(
        id = 4,
        name = "Emily Watson",
        profileImage = R.drawable.androidify4,
        review = "Material Design looks great here."
    ),
    ComposeReviewer(
        id = 5,
        name = "David Park",
        profileImage = R.drawable.androidify5,
        review = "Reusable components boost productivity."
    )
)


