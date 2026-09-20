package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.model.BatteryTelemetry
import com.example.model.UserSettings

class BatteryAlertManager(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var hasAlertedLowBattery = false
    private var hasAlertedFullCharge = false
    private var hasAlertedHighTemp = false

    companion object {
        const val CHANNEL_ID = "battfo_battery_alerts"
        const val NOTIFICATION_ID_LOW = 1001
        const val NOTIFICATION_ID_FULL = 1002
        const val NOTIFICATION_ID_TEMP = 1003
        const val NOTIFICATION_ID_SLOW = 1004
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Configurable threshold alerts for battery level and temperature."
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun checkAndNotify(telemetry: BatteryTelemetry, settings: UserSettings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // Low battery alert
        if (settings.lowBatteryAlertEnabled && !telemetry.isCharging) {
            if (telemetry.percentage <= settings.lowBatteryThreshold) {
                if (!hasAlertedLowBattery) {
                    sendNotification(
                        NOTIFICATION_ID_LOW,
                        "Low Battery Alert",
                        "Battery level has reached ${telemetry.percentage}%."
                    )
                    hasAlertedLowBattery = true
                }
            } else {
                hasAlertedLowBattery = false
            }
        } else {
            hasAlertedLowBattery = false
        }

        // Target charge / Full charge alert
        if (settings.fullChargeAlertEnabled && telemetry.isCharging) {
            if (telemetry.percentage >= settings.fullChargeThreshold) {
                if (!hasAlertedFullCharge) {
                    sendNotification(
                        NOTIFICATION_ID_FULL,
                        "Charge Target Reached",
                        "Battery has reached target level of ${telemetry.percentage}%."
                    )
                    hasAlertedFullCharge = true
                }
            }
        } else {
            hasAlertedFullCharge = false
        }

        // High temperature alert
        if (settings.highTempAlertEnabled && telemetry.temperatureC != null) {
            if (telemetry.temperatureC >= settings.highTempThresholdC) {
                if (!hasAlertedHighTemp) {
                    sendNotification(
                        NOTIFICATION_ID_TEMP,
                        "High Battery Temperature",
                        "Battery temperature is ${String.format("%.1f", telemetry.temperatureC)}°C, exceeding your threshold."
                    )
                    hasAlertedHighTemp = true
                }
            } else if (telemetry.temperatureC < settings.highTempThresholdC - 2.0f) {
                hasAlertedHighTemp = false
            }
        }
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            notificationManager.notify(id, builder.build())
        } catch (_: SecurityException) {}
    }
}
