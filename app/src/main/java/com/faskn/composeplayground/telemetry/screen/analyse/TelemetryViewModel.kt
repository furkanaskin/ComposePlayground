package com.faskn.composeplayground.telemetry.screen.analyse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faskn.composeplayground.telemetry.TelemetryConstants
import com.faskn.composeplayground.telemetry.TelemetryUtils
import com.faskn.composeplayground.telemetry.TelemetryUtils.lerpAt
import com.faskn.composeplayground.telemetry.TelemetryUtils.minPositive
import com.faskn.composeplayground.telemetry.data.ChatMessage
import com.faskn.composeplayground.telemetry.data.ReplayMarker
import com.faskn.composeplayground.telemetry.data.Role
import com.faskn.composeplayground.telemetry.data.SectorInfo
import com.faskn.composeplayground.telemetry.data.TelemetryAssets
import com.faskn.composeplayground.telemetry.data.TelemetryDatabase
import com.faskn.composeplayground.telemetry.data.TelemetryFrame
import com.faskn.composeplayground.telemetry.data.TelemetryUiState
import com.faskn.composeplayground.telemetry.data.TrackData
import com.faskn.composeplayground.telemetry.data.TrackMarker
import com.faskn.composeplayground.telemetry.data.TrackProjection
import com.faskn.composeplayground.telemetry.data.repository.TelemetryRepositoryImpl
import com.faskn.composeplayground.telemetry.domain.FrameInterpolator
import com.faskn.composeplayground.telemetry.domain.LapCompletionController
import com.faskn.composeplayground.telemetry.domain.RaceEngineerAgent
import com.faskn.composeplayground.telemetry.domain.TelemetryProcessor
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class TelemetryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TelemetryRepositoryImpl(application.applicationContext)
    private val processor = TelemetryProcessor(application.applicationContext)
    private val raceEngineerAgent = RaceEngineerAgent(application.applicationContext)
    private val frameInterpolator = FrameInterpolator()
    private val lapCompletionController = LapCompletionController(viewModelScope)

    private val _uiState = MutableStateFlow(TelemetryUiState())
    val uiState = _uiState.asStateFlow()

    private inline fun updateState(reducer: TelemetryUiState.() -> TelemetryUiState) {
        _uiState.update { it.reducer() }
    }

    private var userLapStartIndices: Map<Int, Int> = emptyMap()

    private var lapTimesMap = mutableMapOf<Int, Float>()
    private var bestTimesMap = mutableMapOf<Int, Float>()
    private val userLapStartTimes = mutableMapOf<Int, Float>()

    private var rivalBestLapStartTime = 0f
    private var rivalBestLapEndTime = 0f
    private var rivalBestSectors: Triple<Float, Float, Float>? = null

    private var trackJsonData: TrackData? = null

    init {
        loadData()
    }

    // ── Public Actions ────────────────────────────────────────────────────────

    fun selectLap(lap: Int, autoPlay: Boolean = true) {
        val idx = userLapStartIndices[lap] ?: return
        updateState {
            copy(
                currentProgress = idx.toFloat(),
                selectedLap = lap,
                isPlaying = autoPlay
            )
        }
        refreshLapMetadata()
        updateDelta()
    }

    fun updateIndex(index: Float) {
        val userTelemetry = _uiState.value.userTelemetry
        if (userTelemetry.isEmpty()) return
        updateState {
            copy(
                currentProgress = index.coerceIn(
                    0f,
                    userTelemetry.lastIndex.toFloat()
                )
            )
        }
        syncLapToIndex()
        updateLiveSectors()
        updateDelta()
    }

    fun togglePlayback() {
        val userTelemetry = _uiState.value.userTelemetry
        if (!(_uiState.value.isPlaying) && _uiState.value.currentProgress >= userTelemetry.lastIndex) {
            updateState { copy(currentProgress = 0f) }
        }
        updateState { copy(isPlaying = !isPlaying) }
    }

    fun toggleConfig() {
        updateState { copy(showConfig = !showConfig) }
    }

    fun sendChatMessage(userText: String) {
        viewModelScope.launch {
            updateState {
                copy(
                    chatMessages = (chatMessages + ChatMessage(
                        Role.USER,
                        userText
                    )).toPersistentList(),
                    isChatLoading = true
                )
            }

            raceEngineerAgent.sendMessage(
                userText = userText,
                selectedLap = _uiState.value.selectedLap,
                userTelemetry = _uiState.value.userTelemetry,
                rivalBestLapTelemetry = _uiState.value.rivalBestLapTelemetry,
                userLapStartTimes = userLapStartTimes,
                rivalBestLapStartTime = rivalBestLapStartTime,
                onMessageUpdate = { text, markers, isLoading ->
                    updateLastEngineerMessage(text, markers, isLoading)
                },
                onError = { error ->
                    updateState {
                        copy(
                            chatMessages = (chatMessages + ChatMessage(
                                Role.ENGINEER,
                                error
                            )).toPersistentList(),
                            isChatLoading = false
                        )
                    }
                }
            )

            updateState { copy(isChatLoading = false) }
        }
    }

    private fun updateLastEngineerMessage(
        text: String,
        markers: List<ReplayMarker> = emptyList(),
        isLoading: Boolean = false
    ) {
        updateState {
            val last = chatMessages.lastOrNull()
            if (last?.role == Role.ENGINEER) {
                // Update the existing engineer message
                copy(
                    chatMessages = chatMessages.set(
                        chatMessages.lastIndex,
                        last.copy(text = text, markers = markers, isLoading = isLoading)
                    )
                )
            } else {
                // Add a new engineer message
                copy(
                    chatMessages = chatMessages.add(
                        ChatMessage(role = Role.ENGINEER, text = text, markers = markers, isLoading = isLoading)
                    )
                )
            }
        }
    }

    fun advanceIndex(by: Float) {
        val userTelemetry = _uiState.value.userTelemetry
        if (userTelemetry.isEmpty()) return
        val currentProgress = _uiState.value.currentProgress
        val next = currentProgress + by
        val currentLap = userTelemetry.getOrNull(currentProgress.toInt())?.lap ?: 0
        val nextLap = userTelemetry.getOrNull(next.toInt())?.lap ?: currentLap

        if (nextLap > currentLap && currentLap != TelemetryConstants.OUT_LAP_NUMBER) {
            triggerLapCompletion(currentLap)
        }

        if (next >= userTelemetry.lastIndex) {
            updateState {
                copy(
                    currentProgress = userTelemetry.lastIndex.toFloat(),
                    isPlaying = false
                )
            }
        } else {
            updateState { copy(currentProgress = next) }
            syncLapToIndex()
        }
        updateLiveSectors()
        updateDelta()
    }

    private fun triggerLapCompletion(lap: Int) {
        val lapTime = lapTimesMap[lap] ?: return
        if (lapTime <= 0 && lap != TelemetryConstants.OUT_LAP_NUMBER) return

        val isBest = lapTime == (lapTimesMap.values.asSequence().filter { it > 0 }.minOrNull()
            ?: Float.MAX_VALUE)

        val lastLapSectors = _uiState.value.lapSectorData[lap]
        updateState { copy(currentLiveSectors = lastLapSectors) }

        lapCompletionController.triggerLapCompletion(
            lapTime = lapTime,
            isBest = isBest,
            onUpdate = { state ->
                updateState { copy(lapCompletion = state) }
            },
            onFinish = {
                updateLiveSectors()
            }
        )
    }

    private fun updateDelta() {
        val userFrame = getInterpolatedFrame(isUser = true)
        if (userFrame == null || userFrame.lap == TelemetryConstants.OUT_LAP_NUMBER) {
            updateState { copy(currentDelta = 0f, currentLapTime = "-") }
            return
        }

        val userLapStartTs = userLapStartTimes[userFrame.lap] ?: run {
            updateState { copy(currentDelta = 0f, currentLapTime = "-") }
            return
        }
        val userElapsed = userFrame.timeSeconds - userLapStartTs
        val currentLapTime = TelemetryUtils.mmsss(userElapsed)

        val rivalTelemetry = _uiState.value.rivalBestLapTelemetry
        if (rivalTelemetry.isEmpty()) {
            updateState { copy(currentDelta = 0f, currentLapTime = currentLapTime) }
            return
        }

        val targetDistance = userFrame.lapDistance
        val rivalAtSamePos = frameInterpolator.getRivalFrameAtAbsoluteDist(targetDistance, rivalTelemetry)
            ?: run {
                updateState {
                    copy(
                        currentDelta = 0f,
                        currentLapTime = currentLapTime
                    )
                }; return
            }

        val rivalElapsed = rivalAtSamePos.timeSeconds - rivalBestLapStartTime
        updateState {
            copy(
                currentDelta = userElapsed - rivalElapsed,
                currentLapTime = currentLapTime
            )
        }
    }

    fun getInterpolatedFrame(isUser: Boolean): TelemetryFrame? {
        val userTelemetry = _uiState.value.userTelemetry
        val rivalTelemetry = _uiState.value.rivalBestLapTelemetry
        return frameInterpolator.getInterpolatedFrame(
            isUser = isUser,
            progress = _uiState.value.currentProgress,
            userTelemetry = userTelemetry,
            rivalBestLapTelemetry = rivalTelemetry,
            userLapStartTimes = userLapStartTimes,
            rivalBestLapStartTime = rivalBestLapStartTime,
            rivalBestLapEndTime = rivalBestLapEndTime
        )
    }

    private fun syncLapToIndex() {
        val userTelemetry = _uiState.value.userTelemetry
        if (userTelemetry.isEmpty()) return
        val lap = userTelemetry[_uiState.value.currentProgress.toInt()].lap
        if (lap != _uiState.value.selectedLap) {
            updateState { copy(selectedLap = lap) }
            refreshLapMetadata()
        }
    }

    private fun refreshLapMetadata() {
        val selectedLap = _uiState.value.selectedLap
        val lastCompletedLap = selectedLap - 1
        updateState {
            copy(
                lapStatus = if (selectedLap == TelemetryConstants.OUT_LAP_NUMBER) "Out-lap" else "Timed Lap",
                lastLapTime = lapTimesMap[lastCompletedLap]?.takeIf { time -> time > 0 }
                    ?.let(TelemetryUtils::mmsss) ?: "-",
                bestLapTime = bestTimesMap[lastCompletedLap]?.takeIf { time -> time > 0 }
                    ?.let(TelemetryUtils::mmsss) ?: "-"
            )
        }
    }

    private fun updateLiveSectors() {
        val userTelemetry = _uiState.value.userTelemetry
        if (userTelemetry.isEmpty() || trackJsonData == null) return

        if (lapCompletionController.isPreviewingLastLapSectors) return

        val currentProgress = _uiState.value.currentProgress
        val currentFrame = userTelemetry[currentProgress.toInt()]
        val trackPerimeter = userTelemetry.maxOfOrNull { it.lapDistance } ?: 1f
        val progressPct = currentFrame.lapDistance / trackPerimeter

        val sectors = trackJsonData?.sector ?: return
        var currentSectorIndex = sectors.indexOfFirst { progressPct <= it.marker }
        if (currentSectorIndex == -1) currentSectorIndex = sectors.lastIndex

        val currentLap = currentFrame.lap
        val lapSectors = _uiState.value.lapSectorData[currentLap]

        updateState {
            copy(
                currentLiveSectors = when (currentSectorIndex) {
                    0 -> lapSectors?.copy(
                        s1 = SectorInfo.ZERO,
                        s2 = SectorInfo.ZERO,
                        s3 = SectorInfo.ZERO,
                    )

                    1 -> lapSectors?.copy(
                        s2 = SectorInfo.ZERO,
                        s3 = SectorInfo.ZERO,
                    )

                    2 -> lapSectors?.copy(
                        s3 = SectorInfo.ZERO,
                    )

                    else -> lapSectors
                }
            )
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                trackJsonData = processor.loadTrackJsonData()

                var loadedUserTelemetry = emptyList<TelemetryFrame>()
                withContext(Dispatchers.IO) {
                    val userDbInstance =
                        repository.getTelemetryDb(
                            TelemetryAssets.USER_KEY,
                            TelemetryAssets.USER_PATH
                        )
                    val rivalDbInstance =
                        repository.getTelemetryDb(
                            TelemetryAssets.RIVAL_KEY,
                            TelemetryAssets.RIVAL_PATH
                        )

                    val trackResult = processor.loadTrackInternal(userDbInstance, trackJsonData)
                    val points = trackResult.centerPoints
                    val projection = trackResult.projection
                    val width = trackResult.widthNormalized
                    updateState {
                        copy(
                            trackPoints = points.toPersistentList(),
                            trackWidthNormalized = width
                        )
                    }

                    val userDriverName = repository.getDriverName(userDbInstance).uppercase()
                    val rivalDriverName = repository.getDriverName(rivalDbInstance)
                    val sessionType = repository.getSessionType(userDbInstance)
                    val weather = repository.getWeather(userDbInstance)
                    val trackName = repository.getTrackName(userDbInstance)

                    val rivalSessionTelemetry =
                        loadRivalData(rivalDbInstance, projection)
                    loadedUserTelemetry =
                        loadUserData(userDbInstance, projection)

                    val userMinS = loadedUserTelemetry.minOfOrNull { it.steering } ?: -90f
                    val userMaxS = loadedUserTelemetry.maxOfOrNull { it.steering } ?: 90f
                    val loadedUserSteeringRange = userMinS to userMaxS

                    val rivalMinS = rivalSessionTelemetry.minOfOrNull { it.steering } ?: -90f
                    val rivalMaxS = rivalSessionTelemetry.maxOfOrNull { it.steering } ?: 90f
                    val loadedRivalSteeringRange = rivalMinS to rivalMaxS

                    updateState {
                        copy(
                            userConfig = repository.getCarConfig(userDbInstance),
                            rivalConfig = repository.getCarConfig(rivalDbInstance),
                            userDriverName = userDriverName,
                            rivalDriverName = rivalDriverName,
                            sessionType = sessionType,
                            weather = weather,
                            trackName = trackName,
                            userTelemetry = loadedUserTelemetry.toPersistentList(),
                            userSteeringRange = loadedUserSteeringRange,
                            rivalSteeringRange = loadedRivalSteeringRange
                        )
                    }

                    val markers = mutableListOf<TrackMarker>()
                    if (loadedUserTelemetry.isNotEmpty()) {
                        val sfIdx =
                            loadedUserTelemetry.indexOfFirst { it.lap == 1 }.takeIf { it != -1 }
                                ?: 0
                        markers.add(
                            TelemetryUtils.createMarker(
                                "S/F",
                                sfIdx,
                                loadedUserTelemetry,
                                projection
                            )
                        )

                        trackJsonData?.sector?.forEach { sector ->
                            if (sector.name != "Sector 3") {
                                val trackPerimeter =
                                    loadedUserTelemetry.maxOfOrNull { it.lapDistance } ?: 1f
                                val targetDist = sector.marker * trackPerimeter
                                val markerIdx =
                                    loadedUserTelemetry.indexOfFirst { it.lapDistance >= targetDist }
                                        .takeIf { it != -1 }
                                if (markerIdx != null) {
                                    markers.add(
                                        TelemetryUtils.createMarker(
                                            sector.name.replace("Sector ", "S"),
                                            markerIdx,
                                            loadedUserTelemetry,
                                            projection
                                        )
                                    )
                                }
                            }
                        }
                    }
                    updateState { copy(trackMarkers = markers.toPersistentList()) }

                    val rivalDao = rivalDbInstance.telemetryDao()
                    val bestEntry = rivalDao.getBestLapTimes().minPositive()

                    var loadedRivalBestLapTelemetry = persistentListOf<TelemetryFrame>()
                    if (bestEntry != null) {
                        val bestEntryTime = bestEntry.value
                        val bestEntryTs = bestEntry.ts
                        val endFrame =
                            rivalSessionTelemetry.lastOrNull { frame -> frame.timeSeconds <= bestEntryTs }
                        val bestLapNum = endFrame?.lap ?: 0
                        val rivalLapEntities = rivalDao.getAllLapValues()

                        rivalBestLapStartTime =
                            rivalLapEntities.find { e -> e.lapNumber == bestLapNum }?.ts ?: 0f
                        rivalBestLapEndTime =
                            rivalLapEntities.find { e -> e.lapNumber == bestLapNum + 1 }?.ts
                                ?: (rivalBestLapStartTime + bestEntryTime)

                        loadedRivalBestLapTelemetry = rivalSessionTelemetry
                            .filter { e -> e.lap == bestLapNum }
                            .fold(mutableListOf<TelemetryFrame>()) { acc, frame ->
                                if (acc.isEmpty() || frame.lapDistance >= acc.last().lapDistance) acc.add(
                                    frame
                                )
                                acc
                            }.toPersistentList()
                    }
                    updateState { copy(rivalBestLapTelemetry = loadedRivalBestLapTelemetry) }
                }

                val availableLaps = loadedUserTelemetry.map { it.lap }.distinct().sorted()
                updateState {
                    copy(
                        availableLaps = availableLaps.toPersistentList(),
                        selectedLap = availableLaps.firstOrNull() ?: 0,
                        lapStartIndices = userLapStartIndices.toPersistentMap()
                    )
                }
                refreshLapMetadata()

                val userBestTime =
                    lapTimesMap.values.filter { value -> value > 0 }.minOrNull() ?: 0f
                val rivalBestTime = _uiState.value.rivalBestLapTelemetry.lastOrNull()?.let {
                    it.timeSeconds - rivalBestLapStartTime
                } ?: 0f

                val trackPerimeter = loadedUserTelemetry.maxOfOrNull { it.lapDistance }?.toInt() ?: 6979

                val systemPrompt = raceEngineerAgent.buildSystemPrompt(
                    trackName = _uiState.value.trackName,
                    weather = _uiState.value.weather,
                    sessionType = _uiState.value.sessionType,
                    driverName = _uiState.value.userDriverName,
                    userBestTime = userBestTime,
                    rivalBestTime = rivalBestTime,
                    selectedLap = _uiState.value.selectedLap,
                    userActiveLaps = userLapStartIndices.keys.sorted(),
                    trackJsonData = trackJsonData,
                    lapTimesMap = lapTimesMap,
                    lapSectorDataMap = _uiState.value.lapSectorData,
                    userSteeringRange = _uiState.value.userSteeringRange,
                    rivalSteeringRange = _uiState.value.rivalSteeringRange,
                    trackPerimeter = trackPerimeter,
                    rivalBestSectors = rivalBestSectors
                )

                raceEngineerAgent.initialize(systemPrompt)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadUserData(
        db: TelemetryDatabase,
        projection: TrackProjection?
    ): List<TelemetryFrame> {
        val processedFrames = processFrames(db, projection)
        val dao = db.telemetryDao()
        val lapEntities = dao.getAllLapValues()

        lapEntities.forEach { userLapStartTimes[it.lapNumber] = it.ts }
        userLapStartIndices = lapEntities.associateBy({ it.lapNumber }) { lap ->
            processedFrames.indexOfFirst { it.lap == lap.lapNumber }.coerceAtLeast(0)
        }

        val sectorResult =
            processor.processLapSectors(dao, lapEntities, processedFrames, rivalBestSectors)
        val lapTimes = sectorResult.lapTimes
        val bestTimes = sectorResult.bestTimes
        val lapSectorData = sectorResult.sectorData
        lapTimesMap.putAll(lapTimes)
        bestTimesMap.putAll(bestTimes)
        updateState { copy(lapSectorData = lapSectorData.toPersistentMap()) }

        return processedFrames
    }

    private suspend fun loadRivalData(
        db: TelemetryDatabase,
        projection: TrackProjection?
    ): List<TelemetryFrame> {
        val processedFrames = processFrames(db, projection)
        val dao = db.telemetryDao()

        val bestEntry = dao.getBestLapTimes().minPositive()
        if (bestEntry != null) {
            val bestEntryTime = bestEntry.value
            val bestEntryTs = bestEntry.ts
            val s1 = dao.getLastSector1Values()
                .find { entity -> abs(entity.ts - bestEntryTs) < 0.1f }?.value
                ?: 0f
            val s12 = dao.getLastSector2Values()
                .find { entity -> abs(entity.ts - bestEntryTs) < 0.1f }?.value
                ?: 0f
            rivalBestSectors = Triple(s1, s12 - s1, bestEntryTime - s12)
        }
        return processedFrames
    }

    private suspend fun processFrames(
        db: TelemetryDatabase,
        projection: TrackProjection?
    ): List<TelemetryFrame> {
        val dao = db.telemetryDao()
        val times = dao.getAllGpsTimeValues()
        val lapEntities = dao.getAllLapValues()
        if (times.isEmpty() || lapEntities.isEmpty()) return emptyList()

        val throttles = dao.getAllThrottleValues()
        val brakes = dao.getAllBrakeValues()
        val steerings = dao.getAllSteeringValues()
        val lats = dao.getAllGpsLatitude()
        val lons = dao.getAllGpsLongitude()
        val gears = dao.getAllGearValues()
        val speeds = dao.getAllGroundSpeedValues()
        val laterals = dao.getAllPathLateral()
        val lapDists = dao.getAllLapDistValues()
        val gLatValues = dao.getAllGForceLatValues()

        var lapIdx = 0
        var gearIdx = 0
        var previousLapNumber = -1

        val frames = times.mapIndexed { i, currT ->
            val gpsIdxFloat = i * TelemetryConstants.GPS_SAMPLING_MULTIPLIER
            val idx1 = gpsIdxFloat.toInt().coerceAtMost(lats.lastIndex)
            val idx2 = (idx1 + 1).coerceAtMost(lats.lastIndex)
            val frac = gpsIdxFloat - idx1

            val lat = lats.lerpAt(idx1, idx2, frac)
            val lon = lons.lerpAt(idx1, idx2, frac)
            val gLat = gLatValues.lerpAt(idx1, idx2, frac)

            while (lapIdx + 1 < lapEntities.size && lapEntities[lapIdx + 1].ts <= currT) lapIdx++
            while (gearIdx + 1 < gears.size && gears[gearIdx + 1].ts <= currT) gearIdx++

            val lapNumber = lapEntities[lapIdx].lapNumber
            val lapDist = if (lapNumber != previousLapNumber) {
                previousLapNumber = lapNumber
                0f
            } else {
                lapDists.lerpAt(idx1, idx2, frac)
            }

            TelemetryFrame(
                throttle = throttles.getOrElse((i * TelemetryConstants.TELEMETRY_SAMPLING_MULTIPLIER).toInt()) { 0f },
                brake = brakes.getOrElse((i * TelemetryConstants.TELEMETRY_SAMPLING_MULTIPLIER).toInt()) { 0f },
                steering = steerings.getOrElse(i) { 0f },
                gear = gears.getOrNull(gearIdx)?.value ?: 0,
                speed = speeds.getOrElse(i) { 0f },
                lap = lapNumber,
                timeSeconds = currT,
                lapDistance = lapDist,
                position = if (lat != 0f && lon != 0f) projection?.project(lon, lat) else null,
                lateral = laterals.getOrElse(idx1) { 0f },
                gLat = gLat
            )
        }

        return processor.calculateScores(frames, trackJsonData)
    }
}