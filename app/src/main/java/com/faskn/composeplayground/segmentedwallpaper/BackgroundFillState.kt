package com.faskn.composeplayground.segmentedwallpaper

import android.graphics.Bitmap

sealed class BackgroundFillState {
    data object Idle : BackgroundFillState()
    data class Loading(val message: String = "Processing...") : BackgroundFillState()
    data class Success(val bitmap: Bitmap) : BackgroundFillState()
    data class Error(val message: String) : BackgroundFillState()
}
