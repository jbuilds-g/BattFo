package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BatterySnapshotDao
import com.example.data.local.dao.ChargingSessionDao
import com.example.data.local.entity.BatterySnapshotEntity
import com.example.data.local.entity.ChargingSessionEntity

@Database(
    entities = [BatterySnapshotEntity::class, ChargingSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BattFoDatabase : RoomDatabase() {
    abstract fun batterySnapshotDao(): BatterySnapshotDao
    abstract fun chargingSessionDao(): ChargingSessionDao

    companion object {
        @Volatile
        private var INSTANCE: BattFoDatabase? = null

        fun getInstance(context: Context): BattFoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BattFoDatabase::class.java,
                    "battfo_battery.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
