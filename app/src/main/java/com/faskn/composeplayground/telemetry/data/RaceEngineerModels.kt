package com.faskn.composeplayground.telemetry.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class ChatMessage(
    val role: Role,
    val text: String,
    val isLoading: Boolean = false,
    val markers: List<ReplayMarker> = emptyList()
)

@Serializable
data class ReplayMarker(
    val lap: Int = 0,
    val dist: Int = 0,
    val frame: Int = 0
)

@Serializable
enum class Role {
    @SerialName("user")
    USER,

    @SerialName("engineer")
    ENGINEER
}

sealed class EngineerResponse {
    data class NeedData(val request: DataRequest) : EngineerResponse()
    data class Answer(val text: String, val markers: List<ReplayMarker> = emptyList()) :
        EngineerResponse()

    data object Error : EngineerResponse()
}

@Serializable
data class DataRequest(
    val lapDistStart: Float,
    val lapDistEnd: Float,
    val channels: List<String>,
    val lap: Int? = null
)

internal val RaceEngineerJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

@Serializable
internal data class RawEngineerResponse(
    val status: String? = null,
    val answer: String? = null,
    val markers: List<ReplayMarker>? = null,
    val dataRequest: DataRequest? = null
)

object RaceEngineerParser {
    fun parseResponse(rawJson: String): EngineerResponse {
        val cleanJson = rawJson.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val raw = RaceEngineerJson.decodeFromString<RawEngineerResponse>(cleanJson)
            when (raw.status) {
                "need_data" -> {
                    raw.dataRequest?.let { EngineerResponse.NeedData(it) } ?: EngineerResponse.Error
                }

                "answer" -> {
                    EngineerResponse.Answer(raw.answer ?: "", raw.markers ?: emptyList())
                }

                else -> {
                    EngineerResponse.Error
                }
            }
        } catch (_: Exception) {
            EngineerResponse.Error
        }
    }
}
