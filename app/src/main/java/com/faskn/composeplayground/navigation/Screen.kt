package com.faskn.composeplayground.navigation

object Routes {
    const val HOME = "home"
    const val CREDIT_CARD = "credit_card"
}

sealed class Screen(val route: String) {
    data object Home : Screen(Routes.HOME)
    data object CreditCard : Screen(Routes.CREDIT_CARD)
}