package com.faskn.composeplayground.navigation

import com.faskn.composeplayground.navigation.Routes.PRODUCT_LIST

object Routes {
    const val HOME = "home"
    const val CREDIT_CARD = "credit_card"
    const val PRODUCT_LIST = "product_list"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
}

sealed class Screen(val route: String) {
    data object Home : Screen(Routes.HOME)
    data object CreditCard : Screen(Routes.CREDIT_CARD)
    data object ProductList : Screen(PRODUCT_LIST)
    data object ProductDetail : Screen(Routes.PRODUCT_DETAIL) {
        fun createRoute(productId: Int) = "product_detail/$productId"
    }
    data object AGSLSample : Screen("agsl_sample")
}