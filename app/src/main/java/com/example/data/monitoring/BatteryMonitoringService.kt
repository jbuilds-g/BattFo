package com.example.data.monitoring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.BattFoApplication

class BatteryMonitoringService : Service() {
    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("BattFo is monitoring your battery")
            .setContentText("Battery alerts and usage history stay active in the background.")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        (application as BattFoApplication).batteryRepository
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Background Battery Monitoring", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Ongoing notification while BattFo monitors battery status."
                setShowBadge(false)
            })
        }
    }

    companion object {
        const val CHANNEL_ID = "battfo_background_monitoring"
        const val NOTIFICATION_ID = 1000
    }
}
