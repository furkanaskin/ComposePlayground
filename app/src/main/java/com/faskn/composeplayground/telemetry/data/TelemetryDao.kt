package com.faskn.composeplayground.telemetry.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface TelemetryDao {
    @Query("SELECT * FROM `Lap` ORDER BY rowid ASC")
    suspend fun getAllLapValues(): List<LapEntity>

    @Query("SELECT * FROM `Gear` ORDER BY rowid ASC")
    suspend fun getAllGearValues(): List<GearEntity>

    @Query("SELECT * FROM `Best LapTime` ORDER BY rowid ASC")
    suspend fun getBestLapTimes(): List<BestLapTimeEntity>

    @Query("SELECT * FROM `Current LapTime` ORDER BY rowid ASC")
    suspend fun getCurrentLapTimes(): List<CurrentLapTimeEntity>

    @Query("SELECT ts FROM `Lap` ORDER BY ts ASC")
    suspend fun getLapTimestamps(): List<Float>

    @Query("SELECT value FROM metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadataValue(key: String): String?

    @Query("SELECT * FROM `Last Sector1` ORDER BY rowid ASC")
    suspend fun getLastSector1Values(): List<LastSector1Entity>

    @Query("SELECT * FROM `Last Sector2` ORDER BY rowid ASC")
    suspend fun getLastSector2Values(): List<LastSector2Entity>

    @Query("SELECT * FROM `Best Sector1` ORDER BY rowid ASC")
    suspend fun getBestSector1Values(): List<BestSector1Entity>

    @Query("SELECT * FROM `Best Sector2` ORDER BY rowid ASC")
    suspend fun getBestSector2Values(): List<BestSector2Entity>

    @Query("SELECT value FROM `ABSLevel` ORDER BY rowid ASC LIMIT 1")
    suspend fun getFirstAbsLevel(): Int?

    @Query("SELECT value FROM `Brake Bias Rear` ORDER BY rowid ASC LIMIT 1")
    suspend fun getFirstBrakeBiasRear(): Float?

    @Query("SELECT value FROM `TCCut` ORDER BY rowid ASC LIMIT 1")
    suspend fun getFirstTCCut(): Int?

    @Query("SELECT value FROM `TCLevel` ORDER BY rowid ASC LIMIT 1")
    suspend fun getFirstTCLevel(): Int?

    @Query("SELECT value FROM `TCSlipAngle` ORDER BY rowid ASC LIMIT 1")
    suspend fun getFirstTCSlipAngle(): Int?

    @Query("SELECT value FROM `Throttle Pos` ORDER BY rowid ASC")
    suspend fun getAllThrottleValues(): List<Float>

    @Query("SELECT value FROM `Brake Pos` ORDER BY rowid ASC")
    suspend fun getAllBrakeValues(): List<Float>

    @Query("SELECT value FROM `Steering Pos` ORDER BY rowid ASC")
    suspend fun getAllSteeringValues(): List<Float>

    @Query("SELECT value FROM `Ground Speed` ORDER BY rowid ASC")
    suspend fun getAllGroundSpeedValues(): List<Float>

    @Query("SELECT value FROM `G Force Lat` ORDER BY rowid ASC")
    suspend fun getAllGForceLatValues(): List<Float>

    @Query("SELECT value FROM `GPS Time` ORDER BY rowid ASC")
    suspend fun getAllGpsTimeValues(): List<Float>

    @Query("SELECT value FROM `GPS Latitude` ORDER BY rowid ASC")
    suspend fun getAllGpsLatitude(): List<Float>

    @Query("SELECT value FROM `GPS Longitude` ORDER BY rowid ASC")
    suspend fun getAllGpsLongitude(): List<Float>

    @Query("SELECT value FROM `GPS Latitude` LIMIT :count OFFSET :offset")
    suspend fun getGpsLatitude(offset: Int, count: Int): List<Float>

    @Query("SELECT value FROM `GPS Longitude` LIMIT :count OFFSET :offset")
    suspend fun getGpsLongitude(offset: Int, count: Int): List<Float>

    @Query("SELECT value FROM `Path Lateral` LIMIT :count OFFSET :offset")
    suspend fun getPathLateral(offset: Int, count: Int): List<Float>

    @Query("SELECT value FROM `Path Lateral` ORDER BY rowid ASC")
    suspend fun getAllPathLateral(): List<Float>

    @Query("SELECT value FROM `Lap Dist` ORDER BY rowid ASC")
    suspend fun getAllLapDistValues(): List<Float>

    @Query("SELECT MAX(value) FROM `Ground Speed`")
    suspend fun getTopGroundSpeed(): Float?
}
