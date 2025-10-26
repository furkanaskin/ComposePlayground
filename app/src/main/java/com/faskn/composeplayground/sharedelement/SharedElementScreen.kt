package com.faskn.composeplayground.sharedelement

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class SharedElementAnimationType(val route: String) {
    data object Slide : SharedElementAnimationType("shared_element_slide")
    data object Scale : SharedElementAnimationType("shared_element_scale")
}

@Composable
fun SharedElementScreen() {
    val navController = rememberNavController()

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = SharedElementAnimationType.Slide.route
        ) {
            composable(SharedElementAnimationType.Slide.route) {
                SharedElementToolbarSlideAnimation(
                    onNavigateToScale = {
                        navController.navigate(SharedElementAnimationType.Scale.route) {
                            popUpTo(SharedElementAnimationType.Slide.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(SharedElementAnimationType.Scale.route) {
                SharedElementToolbarScaleAnimation(
                    onNavigateToSlide = {
                        navController.navigate(SharedElementAnimationType.Slide.route) {
                            popUpTo(SharedElementAnimationType.Scale.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}