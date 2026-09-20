package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "battery_snapshots",
    indices = [Index(value = ["timestamp"])]
)
data class BatterySnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val percentage: Int,
    val status: String,
    val plugType: String,
    val voltageMv: Int?,
    val temperatureC: Float?,
    val currentNowMa: Int?,
    val currentAverageMa: Int?,
    val powerWatts: Double?,
    val isScreenOn: Boolean
)
