package com.faskn.composeplayground.telemetry.data.repository

import android.content.Context
import com.faskn.composeplayground.telemetry.TelemetryUtils.format
import com.faskn.composeplayground.telemetry.data.CarConfig
import com.faskn.composeplayground.telemetry.data.TelemetryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

interface TelemetryRepository {
    suspend fun getTelemetryDb(name: String, assetPath: String): TelemetryDatabase
    suspend fun getDriverName(db: TelemetryDatabase): String
    suspend fun getSessionType(db: TelemetryDatabase): String
    suspend fun getWeather(db: TelemetryDatabase): String
    suspend fun getTrackName(db: TelemetryDatabase): String
    suspend fun getCarConfig(db: TelemetryDatabase): CarConfig
    suspend fun getBestLapTime(db: TelemetryDatabase): Float
}

private const val KEY_DRIVER_NAME = "DriverName"
private const val KEY_SESSION_TYPE = "SessionType"
private const val KEY_WEATHER = "WeatherConditions"
private const val KEY_TRACK_NAME = "TrackName"

class TelemetryRepositoryImpl(
    private val context: Context
) : TelemetryRepository {

    private val dbCache = ConcurrentHashMap<String, TelemetryDatabase>()

    override suspend fun getTelemetryDb(name: String, assetPath: String): TelemetryDatabase {
        val key = "$name|$assetPath"
        return dbCache[key] ?: withContext(Dispatchers.IO) {
            dbCache.getOrPut(key) {
                TelemetryDatabase.createDatabase(context, name, assetPath)
            }
        }
    }

    override suspend fun getDriverName(db: TelemetryDatabase): String = withContext(Dispatchers.IO) {
        db.telemetryDao().getMetadataValue(KEY_DRIVER_NAME) ?: "Unknown"
    }

    override suspend fun getSessionType(db: TelemetryDatabase): String = withContext(Dispatchers.IO) {
        db.telemetryDao().getMetadataValue(KEY_SESSION_TYPE) ?: "Qualify"
    }

    override suspend fun getWeather(db: TelemetryDatabase): String = withContext(Dispatchers.IO) {
        db.telemetryDao().getMetadataValue(KEY_WEATHER) ?: "Clear"
    }

    override suspend fun getTrackName(db: TelemetryDatabase): String = withContext(Dispatchers.IO) {
        db.telemetryDao().getMetadataValue(KEY_TRACK_NAME) ?: "Unknown Track"
    }

    override suspend fun getBestLapTime(db: TelemetryDatabase): Float = withContext(Dispatchers.IO) {
        db.telemetryDao().getBestLapTimes().map { it.value }.filter { it > 0 }.minOrNull() ?: 0f
    }

    override suspend fun getCarConfig(db: TelemetryDatabase): CarConfig = withContext(Dispatchers.IO) {
        val dao = db.telemetryDao()
        val abs = dao.getFirstAbsLevel()
        val biasRear = dao.getFirstBrakeBiasRear() ?: 0f
        val tcCut = dao.getFirstTCCut() ?: 0
        val tcLevel = dao.getFirstTCLevel() ?: 0
        val tcSlip = dao.getFirstTCSlipAngle() ?: 0
        val topSpeed = dao.getTopGroundSpeed() ?: 0f

        val biasRearPct = biasRear * 100f
        val biasFrontPct = 100f - biasRearPct

        CarConfig(
            abs = abs?.toString() ?: "-",
            brakeBias = "${biasFrontPct.format(1)}:${biasRearPct.format(1)}",
            tc = "$tcLevel ($tcCut/$tcSlip)",
            topSpeed = topSpeed
        )
    }
}
