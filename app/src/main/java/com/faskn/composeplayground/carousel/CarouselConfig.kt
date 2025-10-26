package com.faskn.composeplayground.carousel

import androidx.compose.ui.unit.Dp
import com.faskn.composeplayground.R
import kotlinx.collections.immutable.persistentListOf

const val ITEM_SIZE_MULTIPLIER = 0.33333334f // 1/3 of screen width for LazyRow, don't change
const val ITEM_PADDING_MULTIPLIER = 0.60f // 0.60f means ~40% padding from shortest edge
const val ITEM_NORMALIZATION_MULTIPLIER = 3f
const val RADIUS_MULTIPLIER = 0.75f
const val VISUAL_ITEM_SIZE_MULTIPLIER =
    ITEM_PADDING_MULTIPLIER * ITEM_SIZE_MULTIPLIER * ITEM_NORMALIZATION_MULTIPLIER

enum class CarouselDirection { PREV, NEXT }

data class ScreenDimensions(
    val width: Dp,
    val height: Dp
) {
    val shortestEdge: Dp get() = minOf(width, height)
}

data class CarouselConfig(
    val itemSize: Dp,
    val visualItemSize: Dp,
    val visualItemSizeNoPadding: Dp = visualItemSize / ITEM_PADDING_MULTIPLIER,
    val radiusDp: Dp
) {
    companion object {
        fun from(dimensions: ScreenDimensions): CarouselConfig {
            // itemSize: LazyRow slot width for snap operations
            val itemSize = dimensions.width * ITEM_SIZE_MULTIPLIER

            // visualItemSize: actual display size with padding applied
            val visualItemSize = dimensions.shortestEdge * VISUAL_ITEM_SIZE_MULTIPLIER

            val radiusDp = itemSize * RADIUS_MULTIPLIER

            return CarouselConfig(
                itemSize = itemSize,
                visualItemSize = visualItemSize,
                radiusDp = radiusDp
            )
        }
    }
}

fun getDevelopers() = persistentListOf(
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
