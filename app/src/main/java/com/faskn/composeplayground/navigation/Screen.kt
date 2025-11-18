package com.faskn.composeplayground.navigation

import com.faskn.composeplayground.navigation.Routes.PRODUCT_LIST

object Routes {
    const val HOME = "home"
    const val CREDIT_CARD = "credit_card"
    const val PRODUCT_LIST = "product_list"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val CIRCULAR_CAROUSEL = "circular_carousel"
    const val SHADOWS = "shadows"
    const val SIDE_PANEL = "side_panel"
    const val EXPLODABLE_CHIPS = "explodable_chips"
    const val SHARED_ELEMENT_TOOLBAR_TRANSITION = "shared_element_toolbar_transition"
    const val SEGMENTED_WALLPAPER = "segmented_wallpaper"
}

sealed class Screen(val route: String) {
    data object Home : Screen(Routes.HOME)
    data object CreditCard : Screen(Routes.CREDIT_CARD)
    data object ProductList : Screen(PRODUCT_LIST)
    data object ProductDetail : Screen(Routes.PRODUCT_DETAIL) {
        fun createRoute(productId: Int) = "product_detail/$productId"
    }

    data object AGSLSample : Screen("agsl_sample")
    data object CircularCarousel : Screen(Routes.CIRCULAR_CAROUSEL)
    data object Shadows : Screen(Routes.SHADOWS)
    data object SidePanel : Screen(Routes.SIDE_PANEL)
    data object ExplodableChips : Screen(Routes.EXPLODABLE_CHIPS)
    data object SharedElementToolbarTransition : Screen(Routes.SHARED_ELEMENT_TOOLBAR_TRANSITION)
    data object SegmentedWallpaper : Screen(Routes.SEGMENTED_WALLPAPER)
}