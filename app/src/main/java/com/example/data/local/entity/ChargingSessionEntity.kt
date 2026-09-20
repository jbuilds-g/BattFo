package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "charging_sessions",
    indices = [Index(value = ["startTime"])]
)
data class ChargingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val startPercentage: Int,
    val endPercentage: Int,
    val plugType: String,
    val peakCurrentMa: Int = 0,
    val avgCurrentMa: Int = 0,
    val peakWattage: Double = 0.0,
    val avgWattage: Double = 0.0,
    val maxTemperatureC: Float = 0f,
    val isCompleted: Boolean = false
)
