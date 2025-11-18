package com.faskn.composeplayground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.faskn.composeplayground.agsl.AGSLSampleScreen
import com.faskn.composeplayground.carousel.CircularCarouselScreen
import com.faskn.composeplayground.creditcard.CardCollapsingPagerScreen
import com.faskn.composeplayground.explodablechips.ExplodableChipsScreen
import com.faskn.composeplayground.home.HomePage
import com.faskn.composeplayground.navigation.Screen
import com.faskn.composeplayground.product.view.ProductDetailScreen
import com.faskn.composeplayground.product.view.ProductListScreen
import com.faskn.composeplayground.segmentedwallpaper.SegmentedWallpaperScreen
import com.faskn.composeplayground.shadows.ShadowsScreen
import com.faskn.composeplayground.sharedelement.SharedElementScreen
import com.faskn.composeplayground.sidepanel.SidePanelScreen
import com.faskn.composeplayground.ui.theme.ComposePlaygroundTheme
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ComposePlaygroundTheme {
                PlaygroundApp()
            }
        }

        // Enable Firebase App Check
        // Add your debug provider to Firebase for emulator
        // Check : https://firebase.google.com/docs/app-check/android/debug-provider?authuser=0#emulator
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundApp() {
    val navController = rememberNavController()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
        ) {
            composable(Screen.Home.route) {
                HomePage(
                    onTutorialClick = { screen ->
                        navController.navigate(screen.route)
                    },
                    padding = innerPadding
                )
            }

            composable(Screen.CreditCard.route) {
                CardCollapsingPagerScreen(innerPadding)
            }

            composable(Screen.ProductList.route) {
                ProductListScreen(
                    padding = innerPadding,
                    navController = navController,
                )
            }

            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("productId") { type = NavType.IntType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getInt("productId") ?: -1

                ProductDetailScreen(
                    productId = productId,
                    navController = navController,
                    padding = innerPadding
                )
            }

            composable(Screen.AGSLSample.route) {
                AGSLSampleScreen()
            }
            composable(Screen.CircularCarousel.route) {
                CircularCarouselScreen()
            }

            composable(Screen.Shadows.route) {
                ShadowsScreen()
            }

            composable(Screen.SidePanel.route) {
                SidePanelScreen()
            }

            composable(Screen.ExplodableChips.route) {
                ExplodableChipsScreen()
            }

            composable(Screen.SharedElementToolbarTransition.route) {
                SharedElementScreen()
            }
            composable(Screen.SegmentedWallpaper.route) {
                SegmentedWallpaperScreen()
            }
        }
    }
}