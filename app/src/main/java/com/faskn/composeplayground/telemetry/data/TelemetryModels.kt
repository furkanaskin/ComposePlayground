package com.faskn.composeplayground.telemetry.data

import androidx.compose.ui.geometry.Offset
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

data class TelemetryUiState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val currentProgress: Float = 0f,
    val selectedLap: Int = 0,
    val availableLaps: PersistentList<Int> = persistentListOf(),
    val bestLapTime: String = "-",
    val lastLapTime: String = "-",
    val lapStatus: String = "",
    val trackWidthNormalized: Float = 0f,
    val lapSectorData: PersistentMap<Int, LapSectorData> = persistentMapOf(),
    val currentLiveSectors: LapSectorData? = null,
    val currentDelta: Float = 0f,
    val currentLapTime: String = "-",
    val lapCompletion: LapCompletionState? = null,
    val userDriverName: String = "YOU",
    val rivalDriverName: String = "Rival",
    val sessionType: String = "Qualify",
    val weather: String = "Clear",
    val trackName: String = "Spa-Francorchamps",
    val trackPoints: PersistentList<Offset> = persistentListOf(),
    val trackMarkers: PersistentList<TrackMarker> = persistentListOf(),
    val lapStartIndices: PersistentMap<Int, Int> = persistentMapOf(),
    val userConfig: CarConfig? = null,
    val rivalConfig: CarConfig? = null,
    val showConfig: Boolean = false,
    val chatMessages: PersistentList<ChatMessage> = persistentListOf(),
    val isChatLoading: Boolean = false,
    val userTelemetry: PersistentList<TelemetryFrame> = persistentListOf(),
    val rivalBestLapTelemetry: PersistentList<TelemetryFrame> = persistentListOf(),
    val userSteeringRange: Pair<Float, Float> = -90f to 90f,
    val rivalSteeringRange: Pair<Float, Float> = -90f to 90f,
)

data class TelemetryFrame(
    val throttle: Float,
    val brake: Float,
    val steering: Float = 0f,
    val gear: Int = 0,
    val speed: Float = 0f,
    val lap: Int,
    val timeSeconds: Float,
    val lapDistance: Float = 0f,
    val position: Offset? = null,
    val lateral: Float = 0f,
    val gLat: Float = 0f,
    val steeringSmoothness: Float = 100f,
    val trailBrakingScore: Float = 100f
) {
    companion object {
        fun lerp(a: TelemetryFrame, b: TelemetryFrame, t: Float): TelemetryFrame {
            return TelemetryFrame(
                throttle = a.throttle.lerp(b.throttle, t),
                brake = a.brake.lerp(b.brake, t),
                steering = a.steering.lerp(b.steering, t),
                gear = discrete(a.gear, b.gear, t),
                speed = a.speed.lerp(b.speed, t),
                lap = discrete(a.lap, b.lap, t),
                timeSeconds = a.timeSeconds.lerp(b.timeSeconds, t),
                lapDistance = a.lapDistance.lerp(b.lapDistance, t),
                position = if (a.position != null && b.position != null)
                    Offset(
                        a.position.x.lerp(b.position.x, t),
                        a.position.y.lerp(b.position.y, t)
                    ) else a.position ?: b.position,
                lateral = a.lateral.lerp(b.lateral, t),
                gLat = a.gLat.lerp(b.gLat, t),
                steeringSmoothness = a.steeringSmoothness.lerp(b.steeringSmoothness, t),
                trailBrakingScore = a.trailBrakingScore.lerp(b.trailBrakingScore, t)
            )
        }

        private fun Float.lerp(other: Float, t: Float) = this + (other - this) * t
        private fun <T> discrete(a: T, b: T, t: Float) = if (t < 0.5f) a else b
    }
}

data class SectorInfo(
    val time: Float,
    val color: Int,
    val delta: Float = 0f
){
    companion object{
        val ZERO = SectorInfo(0f,0,0f)
    }
}

data class LapSectorData(
    val s1: SectorInfo,
    val s2: SectorInfo,
    val s3: SectorInfo
)

data class CarConfig(
    val abs: String = "-",
    val brakeBias: String = "-",
    val tc: String = "-",
    val topSpeed: Float = 0f
)

data class TrackMarker(
    val label: String,
    val position: Offset,
    val angle: Float,
    val normal: Offset
)

data class LapCompletionState(
    val time: String,
    val isBest: Boolean
)

data class TrackProjection(
    val minX: Float, val maxX: Float,
    val minY: Float, val maxY: Float,
    val maxSpan: Float, val cosLat: Float
) {
    fun project(lon: Float, lat: Float): Offset {
        val x = lon * cosLat
        // X and Y are intentionally swapped so the track renders horizontally
        return Offset(x = 1f - (lat - minY) / maxSpan, y = 1f - (x - minX) / maxSpan)
    }
}

sealed class VersusUiState {
    data object Idle : VersusUiState()
    data object Validating : VersusUiState()
    data class Success(
        val trackName: String,
        val leftDriverName: String,
        val rightDriverName: String,
        val leftBestLapTime: String,
        val rightBestLapTime: String,
        val leftBestLapRaw: Float,
        val rightBestLapRaw: Float
    ) : VersusUiState()

    data class Error(val message: String) : VersusUiState()
}