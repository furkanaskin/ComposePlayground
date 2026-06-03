package com.faskn.composeplayground.telemetry.domain

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.faskn.composeplayground.telemetry.TelemetryConstants
import com.faskn.composeplayground.telemetry.TelemetryConstants.FALLBACK_TRACK_PERIMETER
import com.faskn.composeplayground.telemetry.TelemetryConstants.LAT_G_CORNER_MIN
import com.faskn.composeplayground.telemetry.TelemetryConstants.LMU_REF_LAT_COS
import com.faskn.composeplayground.telemetry.TelemetryConstants.MAX_NORMALIZED_DISTANCE
import com.faskn.composeplayground.telemetry.TelemetryConstants.METER_TO_LAT_LON
import com.faskn.composeplayground.telemetry.TelemetryConstants.MIN_BRAKE_FRAMES
import com.faskn.composeplayground.telemetry.TelemetryConstants.MIN_TRACK_WIDTH_METERS
import com.faskn.composeplayground.telemetry.TelemetryConstants.STEERING_ACTIVE_MIN
import com.faskn.composeplayground.telemetry.TelemetryConstants.STEERING_CORNER_THRESHOLD
import com.faskn.composeplayground.telemetry.TelemetryConstants.STEERING_STRAIGHT_THRESHOLD
import com.faskn.composeplayground.telemetry.TelemetryConstants.STEERING_TRIM_PERCENT
import com.faskn.composeplayground.telemetry.TelemetryConstants.STEERING_WINDOW
import com.faskn.composeplayground.telemetry.data.LapEntity
import com.faskn.composeplayground.telemetry.data.LapSectorData
import com.faskn.composeplayground.telemetry.data.LapSectorResult
import com.faskn.composeplayground.telemetry.data.RaceEngineerJson
import com.faskn.composeplayground.telemetry.data.SectorInfo
import com.faskn.composeplayground.telemetry.data.TelemetryAssets.SPA_TRACK_JSON_PATH
import com.faskn.composeplayground.telemetry.data.TelemetryDao
import com.faskn.composeplayground.telemetry.data.TelemetryDatabase
import com.faskn.composeplayground.telemetry.data.TelemetryFrame
import com.faskn.composeplayground.telemetry.data.TrackData
import com.faskn.composeplayground.telemetry.data.TrackLoadResult
import com.faskn.composeplayground.telemetry.data.TrackProjection
import com.faskn.composeplayground.telemetry.data.TrackTurn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

class TelemetryProcessor(private val context: Context) {

    suspend fun loadTrackJsonData(): TrackData? = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets
                .open(SPA_TRACK_JSON_PATH)
                .bufferedReader().use { it.readText() }
            RaceEngineerJson.decodeFromString<TrackData>(jsonString)
        } catch (e: Exception) {
            Log.e("TelemetryProcessor", "Error loading track JSON", e)
            null
        }
    }

    suspend fun loadTrackInternal(db: TelemetryDatabase, trackData: TrackData?): TrackLoadResult =
        withContext(Dispatchers.IO) {
            val dao = db.telemetryDao()
            val gpsTimes = dao.getAllGpsTimeValues()
            if (gpsTimes.isEmpty()) return@withContext TrackLoadResult(emptyList(), null, 0f)
            val startTime = gpsTimes.first()

            val lapTs = dao.getLapTimestamps()
            if (lapTs.size < 2) return@withContext TrackLoadResult(emptyList(), null, 0f)

            val startTs = if (lapTs.size >= 3) lapTs[1] else lapTs[0]
            val endTs = if (lapTs.size >= 3) lapTs[2] else lapTs[1]

            val offset = ((startTs - startTime) * 10).toInt().coerceAtLeast(0)
            val count = ((endTs - startTs) * 10).toInt().coerceAtLeast(0)

            val latsRaw = dao.getGpsLatitude(offset, count) // 10Hz
            val lonsRaw = dao.getGpsLongitude(offset, count) // 10Hz
            val pathLateral = dao.getPathLateral(offset, count) // 10Hz

            val validIdx = latsRaw.indices.filter { latsRaw[it] != 0f && lonsRaw[it] != 0f }
            if (validIdx.size < 2) return@withContext TrackLoadResult(emptyList(), null, 0f)

            val lats = validIdx.map { latsRaw[it] }
            val lons = validIdx.map { lonsRaw[it] }
            val laterals = validIdx.map { if (it < pathLateral.size) pathLateral[it] else 0f }

            val rawPoints = lats.indices.map { i -> Offset(lons[i] * LMU_REF_LAT_COS, lats[i]) }

            val minX = rawPoints.minOf { it.x }
            val maxX = rawPoints.maxOf { it.x }
            val minY = rawPoints.minOf { it.y }
            val maxY = rawPoints.maxOf { it.y }
            val maxSpan = maxOf(maxX - minX, maxY - minY).coerceAtLeast(1e-9f)
            val projection = TrackProjection(minX, maxX, minY, maxY, maxSpan, LMU_REF_LAT_COS)

            val centerPointsRaw =
                rawPoints.map { p -> projection.project(p.x / LMU_REF_LAT_COS, p.y) }
            val meterToNorm = 1f / (maxSpan * METER_TO_LAT_LON)

            val centerPoints = centerPointsRaw.indices.map { i ->
                val p = centerPointsRaw[i]
                val prev = centerPointsRaw[if (i > 0) i - 1 else centerPointsRaw.lastIndex]
                val next = centerPointsRaw[if (i < centerPointsRaw.lastIndex) i + 1 else 0]
                val dx = next.x - prev.x
                val dy = next.y - prev.y
                val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-9f)
                val nx = -dy / len
                val ny = dx / len
                Offset(p.x - nx * laterals[i] * meterToNorm, p.y - ny * laterals[i] * meterToNorm)
            }

            val trackWidthMeters = if (trackData != null) {
                //scale : 1 -> 8m, scale : 6 -> 15m
                val turnWidths = trackData.turn.map {
                    MIN_TRACK_WIDTH_METERS + (it.scale - 1) * (TelemetryConstants.DEFAULT_TRACK_WIDTH_METERS - MIN_TRACK_WIDTH_METERS) / 5f
                }
                val straightWidths =
                    List(trackData.straight.size) { TelemetryConstants.DEFAULT_TRACK_WIDTH_METERS }
                (turnWidths + straightWidths).average().toFloat()
            } else if (pathLateral.isNotEmpty()) {
                (pathLateral.max() - pathLateral.min()).coerceAtLeast(MIN_TRACK_WIDTH_METERS)
            } else {
                TelemetryConstants.DEFAULT_TRACK_WIDTH_METERS
            }

            TrackLoadResult(centerPoints, projection, trackWidthMeters * meterToNorm)
        }

    fun calculateScores(
        frames: List<TelemetryFrame>,
        trackJsonData: TrackData?
    ): List<TelemetryFrame> {
        if (frames.size < 2) return frames

        val trackTurns = trackJsonData?.turn ?: emptyList()
        val trackStraights = trackJsonData?.straight ?: emptyList()
        val trackPerimeter =
            frames.maxOfOrNull { it.lapDistance }?.takeIf { it > 100f } ?: FALLBACK_TRACK_PERIMETER

        val steeringDiffs = FloatArray(frames.size)
        for (i in 1 until frames.size) {
            steeringDiffs[i] = abs(frames[i].steering - frames[i - 1].steering)
        }

        val isFrameInTurn = BooleanArray(frames.size)
        val isFrameInStraight = BooleanArray(frames.size)

        fun getAbs(valFromData: Float) =
            if (valFromData <= MAX_NORMALIZED_DISTANCE) valFromData * trackPerimeter else valFromData

        frames.forEachIndexed { i, f ->
            val d = f.lapDistance
            isFrameInTurn[i] = trackTurns.any { d >= getAbs(it.start) && d <= getAbs(it.end) }
            isFrameInStraight[i] =
                trackStraights.any { d >= getAbs(it.start) && d <= getAbs(it.end) }
        }

        val trailScores = computeTrailBraking(frames, trackTurns, trackPerimeter)
        val steeringSmoothnessScores = computeSteeringSmoothness(
            frames = frames,
            steeringDiffs = steeringDiffs,
            isFrameInTurn = isFrameInTurn,
            isFrameInStraight = isFrameInStraight
        )

        return frames.mapIndexed { idx, frame ->
            frame.copy(
                steeringSmoothness = steeringSmoothnessScores[idx],
                trailBrakingScore = trailScores[idx]
            )
        }
    }

    private fun computeBrakeWindow(frames: List<TelemetryFrame>, start: Int, end: Int): Float? {
        var bCount = 0
        var tCount = 0
        for (i in start until end) {
            if (frames[i].brake > TelemetryConstants.BRAKE_ACTIVE_MIN) {
                bCount++
                if (abs(frames[i].gLat) > LAT_G_CORNER_MIN) {
                    tCount++
                }
            }
        }
        return if (bCount > MIN_BRAKE_FRAMES) {
            (tCount.toFloat() / bCount.toFloat() * 100f).coerceIn(0f, 100f)
        } else {
            null
        }
    }

    private fun computeTrailBraking(
        frames: List<TelemetryFrame>,
        trackTurns: List<TrackTurn>,
        trackPerimeter: Float
    ): FloatArray {
        val trailScores = FloatArray(frames.size) { -1f }
        var lastValidTrailScore = 85f

        fun getAbs(valFromData: Float) =
            if (valFromData <= MAX_NORMALIZED_DISTANCE) valFromData * trackPerimeter else valFromData

        val lapGroups = frames.indices.groupBy { frames[it].lap }
        lapGroups.forEach { (_, indices) ->
            trackTurns.forEach { turn ->
                val sStart = getAbs(turn.start)
                val sMarker = getAbs(turn.marker)

                val entryIdx = indices.find { frames[it].lapDistance >= sStart }
                val apexIdx = indices.find { frames[it].lapDistance >= sMarker }

                if (entryIdx != null && apexIdx != null && apexIdx > entryIdx) {
                    val score = computeBrakeWindow(frames, entryIdx, apexIdx)
                    if (score != null) {
                        lastValidTrailScore = score
                        for (i in entryIdx until apexIdx) {
                            if (i < trailScores.size) {
                                trailScores[i] = score
                            }
                        }
                    }
                }
            }
        }

        for (i in frames.indices) {
            if (trailScores[i] == -1f) {
                trailScores[i] = lastValidTrailScore
            }
        }

        return trailScores
    }

    private fun computeSteeringSmoothness(
        frames: List<TelemetryFrame>,
        steeringDiffs: FloatArray,
        isFrameInTurn: BooleanArray,
        isFrameInStraight: BooleanArray
    ): FloatArray {
        val smoothnessScores = FloatArray(frames.size)
        var lastSteeringScore = 100f
        val windowDeltas = FloatArray(STEERING_WINDOW + 1)

        for (idx in frames.indices) {
            val frame = frames[idx]
            val sStart = (idx - STEERING_WINDOW).coerceAtLeast(0)
            val windowSize = idx - sStart + 1

            for (j in 0 until windowSize) {
                windowDeltas[j] = steeringDiffs[sStart + j]
            }

            windowDeltas.sort(0, windowSize)
            val trimCount = (windowSize * STEERING_TRIM_PERCENT).toInt()
            val effectiveSize = (windowSize - trimCount).coerceAtLeast(1)
            var sum = 0f
            for (j in 0 until effectiveSize) {
                sum += windowDeltas[j]
            }
            val filteredAvg = sum / effectiveSize

            val inTurn = isFrameInTurn[idx]
            val inStraight = isFrameInStraight[idx]

            val threshold = when {
                inTurn -> STEERING_CORNER_THRESHOLD
                inStraight -> STEERING_STRAIGHT_THRESHOLD
                else -> (STEERING_CORNER_THRESHOLD + STEERING_STRAIGHT_THRESHOLD) / 2f
            }

            val steeringSmoothness = if (inTurn || abs(frame.steering) >= STEERING_ACTIVE_MIN) {
                (100f * (1f - (filteredAvg / threshold).coerceIn(0f, 1f)))
                    .also { lastSteeringScore = it }
            } else {
                val straightScore =
                    (100f * (1f - (filteredAvg / STEERING_STRAIGHT_THRESHOLD).coerceIn(0f, 1f)))
                straightScore.coerceAtLeast(lastSteeringScore).also { lastSteeringScore = it }
            }

            smoothnessScores[idx] = steeringSmoothness
        }

        return smoothnessScores
    }

    suspend fun processLapSectors(
        dao: TelemetryDao,
        lapEntities: List<LapEntity>,
        frames: List<TelemetryFrame>,
        rivalBestSectors: Triple<Float, Float, Float>?
    ): LapSectorResult = withContext(Dispatchers.IO) {
        val bestLaps = dao.getBestLapTimes()
        val currentLaps = dao.getCurrentLapTimes()
        val lastS1 = dao.getLastSector1Values()
        val lastS2 = dao.getLastSector2Values()

        val lapTimesMap = mutableMapOf<Int, Float>()
        val bestTimesMap = mutableMapOf<Int, Float>()

        val lapSectorTimes = lapEntities.mapNotNull { lap ->
            val lapEnd = frames.findLast { it.lap == lap.lapNumber }?.timeSeconds ?: 0f
            val lapTime = currentLaps.find { abs(it.ts - lapEnd) < 0.1f }?.value ?: 0f
            val bestTime = bestLaps.find { abs(it.ts - lapEnd) < 0.1f }?.value ?: 0f

            lapTimesMap[lap.lapNumber] = lapTime
            bestTimesMap[lap.lapNumber] = bestTime

            if (lapTime > 0) {
                val s1 = lastS1.find { abs(it.ts - lapEnd) < 0.1f }?.value ?: 0f
                val s12 = lastS2.find { abs(it.ts - lapEnd) < 0.1f }?.value ?: 0f
                if (s1 > 0 && s12 > 0) {
                    return@mapNotNull lap.lapNumber to Triple(
                        s1,
                        s12 - s1,
                        (lapTime - s12).coerceAtLeast(0f)
                    )
                }
            }
            null
        }.toMap()

        val sessionBestS1 =
            minOf(
                dao.getBestSector1Values().map { it.value }.filter { it > 0 }.minOrNull()
                    ?: Float.MAX_VALUE,
                lapSectorTimes.values.minOfOrNull { it.first } ?: Float.MAX_VALUE)
        val sessionBestS2 =
            minOf(
                dao.getBestSector2Values().map { it.value }.filter { it > 0 }.minOrNull()
                    ?: Float.MAX_VALUE,
                lapSectorTimes.values.minOfOrNull { it.second } ?: Float.MAX_VALUE)
        val sessionBestS3 = lapSectorTimes.values.minOfOrNull { it.third } ?: Float.MAX_VALUE

        fun sectorColor(cur: Float, sessionBest: Float, rival: Float?) = when {
            cur <= sessionBest + 0.005f -> 2 // Session Best
            rival == null || cur < rival -> 1 // Improved
            else -> 3 // Slower
        }

        val lapSectorDataMap = lapSectorTimes.mapValues { (_, times) ->
            LapSectorData(
                s1 = SectorInfo(
                    times.first,
                    sectorColor(times.first, sessionBestS1, rivalBestSectors?.first),
                    times.first - (rivalBestSectors?.first ?: times.first)
                ),
                s2 = SectorInfo(
                    times.second,
                    sectorColor(times.second, sessionBestS2, rivalBestSectors?.second),
                    times.second - (rivalBestSectors?.second ?: times.second)
                ),
                s3 = SectorInfo(
                    times.third,
                    sectorColor(times.third, sessionBestS3, rivalBestSectors?.third),
                    times.third - (rivalBestSectors?.third ?: times.third)
                )
            )
        }

        LapSectorResult(lapTimesMap, bestTimesMap, lapSectorDataMap)
    }
}