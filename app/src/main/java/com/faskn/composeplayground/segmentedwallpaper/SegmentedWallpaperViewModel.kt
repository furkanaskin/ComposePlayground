package com.faskn.composeplayground.segmentedwallpaper

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faskn.composeplayground.segmentedwallpaper.gemini.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SegmentedWallpaperViewModel : ViewModel() {

    private val geminiRepository = GeminiRepository()

    private val _uiState = MutableStateFlow<BackgroundFillState>(BackgroundFillState.Idle)
    val uiState: StateFlow<BackgroundFillState> = _uiState.asStateFlow()

    fun generativeFill(
        image: Bitmap,
        prompt: String
    ) {
        viewModelScope.launch {
            _uiState.value = BackgroundFillState.Loading("Processing image with AI...")

            geminiRepository.generativeFill(image, prompt)
                .onSuccess { bitmap ->
                    _uiState.value = BackgroundFillState.Success(bitmap)
                }
                .onFailure { error ->
                    _uiState.value = BackgroundFillState.Error(
                        error.message ?: "Unknown error occurred"
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = BackgroundFillState.Idle
    }
}

sealed class BackgroundFillState {
    data object Idle : BackgroundFillState()
    data class Loading(val message: String = "Processing...") : BackgroundFillState()
    data class Success(val bitmap: Bitmap) : BackgroundFillState()
    data class Error(val message: String) : BackgroundFillState()
}

