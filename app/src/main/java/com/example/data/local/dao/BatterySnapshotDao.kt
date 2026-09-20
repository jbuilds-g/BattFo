package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BatterySnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatterySnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: BatterySnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<BatterySnapshotEntity>)

    @Query("SELECT * FROM battery_snapshots ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSnapshot(): Flow<BatterySnapshotEntity?>

    @Query("SELECT * FROM battery_snapshots WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getSnapshotsSince(sinceTimestamp: Long): Flow<List<BatterySnapshotEntity>>

    @Query("SELECT * FROM battery_snapshots ORDER BY timestamp ASC")
    suspend fun getAllSnapshotsList(): List<BatterySnapshotEntity>

    @Query("SELECT * FROM battery_snapshots ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSnapshotsList(limit: Int): List<BatterySnapshotEntity>

    @Query("DELETE FROM battery_snapshots WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM battery_snapshots")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM battery_snapshots")
    suspend fun getCount(): Int
}
