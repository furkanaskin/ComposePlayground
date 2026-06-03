package com.faskn.composeplayground.telemetry.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Throttle Pos")
data class ThrottleEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "Brake Pos")
data class BrakeEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "Steering Pos")
data class SteeringEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "Lap")
data class LapEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val lapNumber: Int
)

@Entity(tableName = "GPS Time")
data class GpsTimeEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "Best LapTime")
data class BestLapTimeEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "Current LapTime")
data class CurrentLapTimeEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "metadata")
data class MetadataEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String
)

@Entity(tableName = "GPS Latitude")
data class GpsLatitudeEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "GPS Longitude")
data class GpsLongitudeEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "Path Lateral")
data class PathLateralEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "Gear")
data class GearEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val value: Int
)

@Entity(tableName = "Ground Speed")
data class GroundSpeedEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "Last Sector1")
data class LastSector1Entity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "Last Sector2")
data class LastSector2Entity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "Best Sector1")
data class BestSector1Entity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "Best Sector2")
data class BestSector2Entity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "Lap Dist")
data class LapDistEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "ABSLevel")
data class AbsLevelEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val value: Int
)

@Entity(tableName = "Brake Bias Rear")
data class BrakeBiasRearEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val value: Float
)

@Entity(tableName = "TCCut")
data class TCCutEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val value: Int
)

@Entity(tableName = "TCLevel")
data class TCLevelEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val value: Int
)

@Entity(tableName = "TCSlipAngle")
data class TCSlipAngleEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "ts") val ts: Float,
    @ColumnInfo(name = "value") val value: Int
)

@Entity(tableName = "G Force Lat")
data class GForceLatEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long? = null,
    @ColumnInfo(name = "value") val value: Float
)