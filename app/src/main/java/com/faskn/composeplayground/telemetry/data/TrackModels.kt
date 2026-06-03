package com.faskn.composeplayground.telemetry.data

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable

@Serializable
data class TrackData(
    val name: String,
    val trackId: String,
    val country: String,
    val length: Int,
    val pitentry: Float,
    val pitexit: Float,
    val turn: List<TrackTurn>,
    val straight: List<TrackStraight>,
    val sector: List<TrackSector>,
    val time: List<TrackTimeInfo>? = null
)

@Serializable
data class TrackTurn(
    val number: Int,
    val start: Float,
    val marker: Float,
    val end: Float,
    val direction: Int,
    val scale: Int,
    val name: String? = null
)

@Serializable
data class TrackStraight(
    val name: String,
    val start: Float,
    val end: Float
)

@Serializable
data class TrackSector(
    val name: String,
    val marker: Float
)

@Serializable
data class TrackTimeInfo(
    val gt3: List<PenaltyInfo>? = null,
    val gte: List<PenaltyInfo>? = null,
    val lmp2: List<PenaltyInfo>? = null,
    val hyper: List<PenaltyInfo>? = null
)

@Serializable
data class PenaltyInfo(
    val name: String,
    val time: Int
)

data class TrackLoadResult(
    val centerPoints: List<Offset>,
    val projection: TrackProjection?,
    val widthNormalized: Float
)

data class LapSectorResult(
    val lapTimes: Map<Int, Float>,
    val bestTimes: Map<Int, Float>,
    val sectorData: Map<Int, LapSectorData>
)