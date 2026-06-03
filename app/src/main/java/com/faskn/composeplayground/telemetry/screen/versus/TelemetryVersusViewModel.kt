package com.faskn.composeplayground.telemetry.screen.versus

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faskn.composeplayground.telemetry.TelemetryUtils
import com.faskn.composeplayground.telemetry.data.TelemetryAssets.RIVAL_KEY
import com.faskn.composeplayground.telemetry.data.TelemetryAssets.RIVAL_PATH
import com.faskn.composeplayground.telemetry.data.TelemetryAssets.USER_KEY
import com.faskn.composeplayground.telemetry.data.TelemetryAssets.USER_PATH
import com.faskn.composeplayground.telemetry.data.VersusUiState
import com.faskn.composeplayground.telemetry.data.repository.TelemetryRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TelemetryVersusViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TelemetryRepositoryImpl(application.applicationContext)

    var uiState by mutableStateOf<VersusUiState>(VersusUiState.Idle)
        private set

    fun startValidation() {
        viewModelScope.launch {
            uiState = withContext(Dispatchers.IO) {
                val d1 = repository.getTelemetryDb(USER_KEY, USER_PATH)
                val d2 = repository.getTelemetryDb(RIVAL_KEY, RIVAL_PATH)

                val track1 = repository.getTrackName(d1)
                val track2 = repository.getTrackName(d2)

                if (track1.isEmpty() || track1 != track2 || track1 == "Unknown Track") {
                    return@withContext VersusUiState.Error("Tracks should be the same for side-by-side analysis")
                }

                val leftBest = repository.getBestLapTime(d1)
                val rightBest = repository.getBestLapTime(d2)

                VersusUiState.Success(
                    trackName = track1,
                    leftDriverName = repository.getDriverName(d1).split(" ").first(),
                    rightDriverName = "Rival",
                    leftBestLapTime = TelemetryUtils.lapTime(leftBest),
                    rightBestLapTime = TelemetryUtils.lapTime(rightBest),
                    leftBestLapRaw = leftBest,
                    rightBestLapRaw = rightBest
                )
            }
        }
    }
}