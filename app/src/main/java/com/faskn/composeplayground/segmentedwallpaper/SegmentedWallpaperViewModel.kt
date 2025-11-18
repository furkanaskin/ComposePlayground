package com.faskn.composeplayground.segmentedwallpaper

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SegmentedWallpaperViewModel : ViewModel() {

    private val geminiRepository = GeminiRepository()

    private val _uiState = MutableStateFlow<BackgroundFillState>(BackgroundFillState.Idle)
    val uiState: StateFlow<BackgroundFillState> = _uiState.asStateFlow()

    fun generativeFill(
        image: Bitmap?,
        prompt: String
    ) {
        viewModelScope.launch {
            if (image == null)
                return@launch

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
