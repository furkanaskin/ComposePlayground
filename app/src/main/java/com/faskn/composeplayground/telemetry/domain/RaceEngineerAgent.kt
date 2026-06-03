package com.faskn.composeplayground.telemetry.domain

import android.content.Context
import android.util.Log
import com.faskn.composeplayground.telemetry.TelemetryUtils
import com.faskn.composeplayground.telemetry.TelemetryUtils.format
import com.faskn.composeplayground.telemetry.data.DataRequest
import com.faskn.composeplayground.telemetry.data.EngineerResponse
import com.faskn.composeplayground.telemetry.data.LapSectorData
import com.faskn.composeplayground.telemetry.data.RaceEngineerParser
import com.faskn.composeplayground.telemetry.data.ReplayMarker
import com.faskn.composeplayground.telemetry.data.TelemetryAssets.RACE_ENGINEER_PROMPT_PATH
import com.faskn.composeplayground.telemetry.data.TelemetryFrame
import com.faskn.composeplayground.telemetry.data.TrackData
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.delay

class RaceEngineerAgent(
    private val context: Context,
    private val apiKey: String = "YOUR_KEY_HERE"
) {

    private var chat: Chat? = null

    private fun logAiMessage(role: String, text: String) {
        Log.d("RaceEngineerAI", "[$role]: $text")
    }

    fun initialize(systemInstructionText: String) {
        val model = GenerativeModel(
            modelName = "gemini-3.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            },
            systemInstruction = content { text(systemInstructionText) }
        )
        chat = model.startChat()
    }

    suspend fun sendMessage(
        userText: String,
        selectedLap: Int,
        userTelemetry: List<TelemetryFrame>,
        rivalBestLapTelemetry: List<TelemetryFrame>,
        userLapStartTimes: Map<Int, Float>,
        rivalBestLapStartTime: Float,
        onMessageUpdate: (text: String, markers: List<ReplayMarker>, isLoading: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentChat = chat ?: return
        logAiMessage("USER", userText)

        try {
            val stream = currentChat.sendMessageStream(userText)
            var accumulatedRaw = ""
            var hasCreatedMessagePlaceholder = false
            var lastTypedText = ""
            var currentMarkers = emptyList<ReplayMarker>()

            stream.collect { chunk ->
                accumulatedRaw += chunk.text ?: ""
                when (val parsed = RaceEngineerParser.parseResponse(accumulatedRaw)) {
                    is EngineerResponse.Answer -> {
                        if (!hasCreatedMessagePlaceholder) {
                            onMessageUpdate("", emptyList(), true)
                            hasCreatedMessagePlaceholder = true
                        }
                        currentMarkers = parsed.markers
                        lastTypedText = streamTypedAnswer(parsed, lastTypedText, onMessageUpdate)
                    }

                    is EngineerResponse.NeedData -> {
                        onMessageUpdate("Analyzing telemetry data...", emptyList(), true)
                        hasCreatedMessagePlaceholder = true
                    }

                    else -> {}
                }
            }

            logAiMessage("ENGINEER_RAW", accumulatedRaw)
            val finalParsed = RaceEngineerParser.parseResponse(accumulatedRaw)

            if (finalParsed is EngineerResponse.NeedData) {
                val segmentData = buildTelemetrySegment(
                    finalParsed.request,
                    selectedLap,
                    userTelemetry,
                    rivalBestLapTelemetry,
                    userLapStartTimes,
                    rivalBestLapStartTime
                )
                logAiMessage("SYSTEM_DATA_SENT", segmentData)

                val dataStream = currentChat.sendMessageStream("TELEMETRY_DATA:\n$segmentData")
                var dataAccumulatedRaw = ""
                var dataLastTypedText = ""

                dataStream.collect { chunk ->
                    dataAccumulatedRaw += chunk.text ?: ""
                    val currentParsed = RaceEngineerParser.parseResponse(dataAccumulatedRaw)
                    if (currentParsed is EngineerResponse.Answer) {
                        currentMarkers = currentParsed.markers
                        dataLastTypedText =
                            streamTypedAnswer(currentParsed, dataLastTypedText, onMessageUpdate)
                    }
                }
                lastTypedText = dataLastTypedText
            }

            // Final update to clear loading state
            onMessageUpdate(lastTypedText, currentMarkers, false)

        } catch (e: Exception) {
            onError("Error: ${e.message}")
        }
    }

    private suspend fun streamTypedAnswer(
        parsed: EngineerResponse.Answer,
        lastTypedText: String,
        onMessageUpdate: (String, List<ReplayMarker>, Boolean) -> Unit
    ): String {
        val fullText = parsed.text
        val words = fullText.split(" ")
        val lastWords = lastTypedText.split(" ")
        var currentLastTypedText = lastTypedText

        for (i in lastWords.size until words.size) {
            val currentText = words.take(i + 1).joinToString(" ")
            onMessageUpdate(currentText, parsed.markers, true)
            currentLastTypedText = currentText
            delay(60)
        }

        if (currentLastTypedText != fullText) {
            onMessageUpdate(fullText, parsed.markers, true)
            currentLastTypedText = fullText
        }
        return currentLastTypedText
    }

    fun buildSystemPrompt(
        trackName: String,
        weather: String,
        sessionType: String,
        driverName: String,
        userBestTime: Float,
        rivalBestTime: Float,
        selectedLap: Int,
        userActiveLaps: List<Int>,
        trackJsonData: TrackData?,
        lapTimesMap: Map<Int, Float>,
        lapSectorDataMap: Map<Int, LapSectorData>,
        userSteeringRange: Pair<Float, Float>,
        rivalSteeringRange: Pair<Float, Float>,
        trackPerimeter: Int,
        rivalBestSectors: Triple<Float, Float, Float>?
    ): String {
        val s1user = lapSectorDataMap.values.map { it.s1.time }.filter { it > 0 }.minOrNull() ?: 0f
        val s2user = lapSectorDataMap.values.map { it.s2.time }.filter { it > 0 }.minOrNull() ?: 0f
        val s3user = lapSectorDataMap.values.map { it.s3.time }.filter { it > 0 }.minOrNull() ?: 0f

        val s1rival = rivalBestSectors?.first ?: 0f
        val s2rival = rivalBestSectors?.second ?: 0f
        val s3rival = rivalBestSectors?.third ?: 0f

        val lapSummary = lapTimesMap.entries
            .filter { it.value > 0 }
            .sortedBy { it.key }
            .joinToString("\n") { (lap, time) ->
                val sectors = lapSectorDataMap[lap]
                "  Lap $lap: ${TelemetryUtils.mmsss(time)} | S1=${sectors?.s1?.time?.format(3) ?: "-"} S2=${
                    sectors?.s2?.time?.format(
                        3
                    ) ?: "-"
                } S3=${sectors?.s3?.time?.format(3) ?: "-"}"
            }

        val trackLayoutCsv = trackJsonData?.let { data ->
            buildString {
                appendLine("type,name,start,end")
                data.turn.forEach { appendLine("turn,${it.name ?: it.number},${(it.start * trackPerimeter).toInt()},${(it.end * trackPerimeter).toInt()}") }
                data.straight.forEach { appendLine("straight,${it.name},${(it.start * trackPerimeter).toInt()},${(it.end * trackPerimeter).toInt()}") }
                data.sector.forEach { appendLine("sector,${it.name},,${(it.marker * trackPerimeter).toInt()}") }
            }
        } ?: "No ground-truth track layout available."

        val promptTemplate = context.assets.open(RACE_ENGINEER_PROMPT_PATH)
            .bufferedReader().use { it.readText() }

        return promptTemplate
            .replace("{{trackName}}", trackName)
            .replace("{{trackPerimeter}}", trackPerimeter.toString())
            .replace("{{weather}}", weather)
            .replace("{{sessionType}}", sessionType)
            .replace("{{driverName}}", driverName)
            .replace("{{userBestTime}}", TelemetryUtils.mmsss(userBestTime))
            .replace("{{rivalBestTime}}", TelemetryUtils.mmsss(rivalBestTime))
            .replace("{{selectedLap}}", selectedLap.toString())
            .replace("{{userActiveLaps}}", userActiveLaps.joinToString(", "))
            .replace("{{trackLayoutCsv}}", trackLayoutCsv)
            .replace("{{lapSummary}}", lapSummary)
            .replace("{{s1user}}", s1user.format(3))
            .replace("{{s2user}}", s2user.format(3))
            .replace("{{s3user}}", s3user.format(3))
            .replace("{{s1rival}}", s1rival.format(3))
            .replace("{{s2rival}}", s2rival.format(3))
            .replace("{{s3rival}}", s3rival.format(3))
            .replace("{{userSteeringRangeStart}}", userSteeringRange.first.toInt().toString())
            .replace("{{userSteeringRangeEnd}}", userSteeringRange.second.toInt().toString())
            .replace("{{rivalSteeringRangeStart}}", rivalSteeringRange.first.toInt().toString())
            .replace("{{rivalSteeringRangeEnd}}", rivalSteeringRange.second.toInt().toString())
    }

    fun buildTelemetrySegment(
        request: DataRequest,
        selectedLap: Int,
        userTelemetry: List<TelemetryFrame>,
        rivalBestLapTelemetry: List<TelemetryFrame>,
        userLapStartTimes: Map<Int, Float>,
        rivalBestLapStartTime: Float
    ): String {
        val lapToUse = request.lap ?: selectedLap
        val userFrames = userTelemetry.asSequence()
            .mapIndexed { index, frame -> index to frame }
            .filter { (_, it) -> it.lap == lapToUse }
            .filter { (_, it) -> it.lapDistance in request.lapDistStart..request.lapDistEnd }
            .toList()
        val rivalFrames = rivalBestLapTelemetry.asSequence()
            .mapIndexed { index, frame -> index to frame }
            .filter { (_, it) -> it.lapDistance in request.lapDistStart..request.lapDistEnd }
            .toList()

        val sectionLength = request.lapDistEnd - request.lapDistStart
        val sampleCount = (sectionLength / 50).toInt().coerceIn(10, 30)

        val userSampled = userFrames.evenSample(sampleCount)
        val rivalSampled = rivalFrames.evenSample(sampleCount)

        val activeChannels = CHANNEL_SPECS.filter { it.id in request.channels }

        return buildString {
            // CSV Header
            append("driver,dist,frame")
            activeChannels.forEach { append(",${it.header}") }
            appendLine()

            // Helper to append a CSV row
            fun appendFrame(driver: String, f: TelemetryFrame, lapStart: Float, globalIdx: Int?) {
                append("$driver,${f.lapDistance.toInt()}${if (globalIdx != null) ",${globalIdx}" else ""}")
                activeChannels.forEach { append(",${it.extract(f, lapStart)}") }
                appendLine()
            }

            userSampled.forEach { (idx, it) ->
                appendFrame(
                    "USER",
                    it,
                    userLapStartTimes[it.lap] ?: 0f,
                    idx
                )
            }
            rivalSampled.forEach { (_, it) ->
                appendFrame(
                    "RIVAL",
                    it,
                    rivalBestLapStartTime,
                    null // use user's idx
                )
            }
        }
    }

    private fun <T> List<T>.evenSample(n: Int): List<T> {
        if (size <= n) return this
        val step = size.toFloat() / n
        return (0 until n).map { this[(it * step).toInt()] }
    }

    private data class ChannelSpec(
        val id: String,
        val header: String,
        val extract: (TelemetryFrame, Float) -> String
    )

    companion object {
        private val CHANNEL_SPECS = listOf(
            ChannelSpec("speed", "spd") { f, _ -> f.speed.toInt().toString() },
            ChannelSpec("brake", "brk") { f, _ -> f.brake.toInt().toString() },
            ChannelSpec("throttle", "thr") { f, _ -> f.throttle.toInt().toString() },
            ChannelSpec("steering", "str") { f, _ -> f.steering.toInt().toString() },
            ChannelSpec("gLat", "g") { f, _ -> f.gLat.format(2) },
            ChannelSpec("lapTime", "t") { f, lapStart -> (f.timeSeconds - lapStart).format(3) },
            ChannelSpec("steeringSmoothness", "smth") { f, _ ->
                f.steeringSmoothness.toInt().toString()
            },
            ChannelSpec("trailBrakingScore", "trail") { f, _ ->
                f.trailBrakingScore.toInt().toString()
            },
            ChannelSpec("gear", "gear") { f, _ -> f.gear.toString() },
            ChannelSpec("pathLateral", "lat") { f, _ -> f.lateral.format(2) }
        )
    }
}
