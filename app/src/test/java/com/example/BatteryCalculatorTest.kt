package com.example

import com.example.data.local.entity.BatterySnapshotEntity
import com.example.data.telemetry.BatteryCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryCalculatorTest {

    @Test
    fun testCalculatePowerWatts_validValues() {
        // 4000 mV and 1000 mA -> 4.0 W
        val power = BatteryCalculator.calculatePowerWatts(voltageMv = 4000, currentMa = 1000)
        assertNotNull(power)
        assertEquals(4.0, power!!, 0.01)

        // Negative current (discharging): 3800 mV and -500 mA -> 1.9 W
        val dischargePower = BatteryCalculator.calculatePowerWatts(voltageMv = 3800, currentMa = -500)
        assertNotNull(dischargePower)
        assertEquals(1.9, dischargePower!!, 0.01)
    }

    @Test
    fun testCalculatePowerWatts_nullOrZero() {
        assertNull(BatteryCalculator.calculatePowerWatts(voltageMv = null, currentMa = 500))
        assertNull(BatteryCalculator.calculatePowerWatts(voltageMv = 4000, currentMa = null))
        assertNull(BatteryCalculator.calculatePowerWatts(voltageMv = 0, currentMa = 500))
        assertNull(BatteryCalculator.calculatePowerWatts(voltageMv = 4000, currentMa = 0))
    }

    @Test
    fun testFormatEstimatedDuration() {
        assertEquals("Calculating...", BatteryCalculator.formatEstimatedDuration(null))
        assertEquals("Calculating...", BatteryCalculator.formatEstimatedDuration(-10L))
        assertEquals("< 1m", BatteryCalculator.formatEstimatedDuration(45L))
        assertEquals("12m", BatteryCalculator.formatEstimatedDuration(750L))
        assertEquals("2h 15m", BatteryCalculator.formatEstimatedDuration(8100L))
    }

    @Test
    fun testCalculateRatePerHourFromSnapshots() {
        val now = 1700000000000L
        val oneHourAgo = now - 3600000L

        // 80% to 70% in 1 hour -> -10.0 %/hr
        val newest = BatterySnapshotEntity(
            id = 1,
            timestamp = now,
            percentage = 70,
            status = "DISCHARGING",
            plugType = "NONE",
            voltageMv = 3800,
            temperatureC = 30f,
            currentNowMa = -400,
            currentAverageMa = -400,
            powerWatts = 1.52,
            isScreenOn = true
        )
        val oldest = BatterySnapshotEntity(
            id = 2,
            timestamp = oneHourAgo,
            percentage = 80,
            status = "DISCHARGING",
            plugType = "NONE",
            voltageMv = 3900,
            temperatureC = 30f,
            currentNowMa = -400,
            currentAverageMa = -400,
            powerWatts = 1.56,
            isScreenOn = true
        )

        val rate = BatteryCalculator.calculateRatePerHourFromSnapshots(listOf(newest, oldest))
        assertNotNull(rate)
        assertEquals(-10.0, rate!!, 0.01)

        // Too short window (< 1 minute) should return null
        val shortOldest = oldest.copy(timestamp = now - 30_000L)
        val shortRate = BatteryCalculator.calculateRatePerHourFromSnapshots(listOf(newest, shortOldest))
        assertNull(shortRate)
    }
}
