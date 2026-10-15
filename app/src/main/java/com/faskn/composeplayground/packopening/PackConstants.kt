package com.faskn.composeplayground.packopening

/**
 * Single source of truth for trading card booster pack, collectible card, and AGSL shader layout dimensions.
 */
object PackConstants {
    const val PACK_WIDTH_DP = 300f
    const val PACK_HEIGHT_DP = 480f

    // Matching AGSL shader parameters in PUFFY_AGSL
    const val AGSL_PUFF_END = 0.08f
    const val AGSL_SEAM_OFFSET = 0.014f

    /**
     * Calculates the exact width of the booster pack's inner puffy cavity matching AGSL shader math.
     */
    fun calculatePuffyPocketWidth(packWidthDp: Float = PACK_WIDTH_DP): Float {
        return packWidthDp * (1f - 2f * AGSL_PUFF_END)
    }

    /**
     * Calculates the exact height of the booster pack's inner puffy cavity matching AGSL shader math.
     */
    fun calculatePuffyPocketHeight(
        packHeightDp: Float = PACK_HEIGHT_DP,
        stripFraction: Float = 0.10f,
    ): Float {
        return packHeightDp * (1f - (stripFraction - AGSL_SEAM_OFFSET) - AGSL_PUFF_END)
    }


    // Default collectible card dimensions derived directly from standard 300x480 booster pack dimensions
    val DEFAULT_CARD_WIDTH_DP = calculatePuffyPocketWidth(PACK_WIDTH_DP)
    val DEFAULT_CARD_HEIGHT_DP = calculatePuffyPocketHeight(PACK_HEIGHT_DP)
}

/**
 * Single source of truth for where the booster pack's shadow ridges are, and therefore where the collectible card sits.
 */
object PackGeometry {
    const val SEAM_OFFSET = 0.014f   // top seam sits this far above the tear line (fraction of pack height)
    const val PUFF_START = 0.025f    // flat until here (fraction of pack width from the edge)
    const val PUFF_END = 0.085f      // fully puffed from here

    /** Where the bulge is steepest, as a position between PUFF_START (0) and PUFF_END (1). */
    const val RIDGE_T = 0.32f

    /** Ridge distance from the pack edge (fraction of pack width). */
    const val RIDGE = PUFF_START + (PUFF_END - PUFF_START) * RIDGE_T

    class CardRect(val leftDp: Float, val topDp: Float, val widthDp: Float, val heightDp: Float)

    /** The collectible card's rectangle inside the booster pack, in dp from the pack's top-left corner. */
    fun cardRect(packWidthDp: Float, packHeightDp: Float, stripFraction: Float): CardRect {
        val ridgeDp = RIDGE * packWidthDp                 // same distance on every side
        val top = (stripFraction - SEAM_OFFSET) * packHeightDp + ridgeDp
        val bottom = packHeightDp - ridgeDp
        return CardRect(
            leftDp = ridgeDp,
            topDp = top,
            widthDp = packWidthDp - 2f * ridgeDp,
            heightDp = bottom - top,
        )
    }
}
