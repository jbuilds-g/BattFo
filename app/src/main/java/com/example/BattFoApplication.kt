package com.example

import android.app.Application
import com.example.data.repository.BatteryRepository

class BattFoApplication : Application() {
    val batteryRepository: BatteryRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        BatteryRepository(applicationContext)
    }
}
