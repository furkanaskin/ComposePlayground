package com.faskn.composeplayground.telemetry.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ThrottleEntity::class,
        BrakeEntity::class,
        SteeringEntity::class,
        LapEntity::class,
        GpsTimeEntity::class,
        BestLapTimeEntity::class,
        CurrentLapTimeEntity::class,
        MetadataEntity::class,
        GpsLatitudeEntity::class,
        GpsLongitudeEntity::class,
        PathLateralEntity::class,
        GearEntity::class,
        GroundSpeedEntity::class,
        LastSector1Entity::class,
        LastSector2Entity::class,
        BestSector1Entity::class,
        BestSector2Entity::class,
        LapDistEntity::class,
        AbsLevelEntity::class,
        BrakeBiasRearEntity::class,
        TCCutEntity::class,
        TCLevelEntity::class,
        TCSlipAngleEntity::class,
        GForceLatEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TelemetryDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao

    companion object {
        fun createDatabase(context: Context, name: String, assetPath: String): TelemetryDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TelemetryDatabase::class.java,
                name
            )
                .createFromAsset(assetPath)
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
        }
    }
}
