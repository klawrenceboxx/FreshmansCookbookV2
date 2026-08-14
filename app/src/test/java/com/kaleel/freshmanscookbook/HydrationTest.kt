package com.kaleel.freshmanscookbook

import com.kaleel.freshmanscookbook.data.*
import org.junit.Assert.assertEquals
import org.junit.Test

class HydrationTest {
    @Test
    fun allInputUnitsResolveToCanonicalMilliliters() {
        assertEquals(500.0, WaterConversion.toMilliliters(500.0, WaterUnit.MILLILITERS), 0.0)
        assertEquals(1000.0, WaterConversion.toMilliliters(1.0, WaterUnit.LITERS), 0.0)
        assertEquals(500.0, WaterConversion.toMilliliters(2.0, WaterUnit.CUPS), 0.0)
    }

    @Test
    fun canadianMetricCupIsUsedConsistently() {
        assertEquals(250.0, WaterConversion.MILLILITERS_PER_CUP, 0.0)
        assertEquals(8.0, WaterConversion.fromMilliliters(2000.0, WaterDisplayUnit.CUPS), 0.0)
    }

    @Test
    fun displayUnitDoesNotChangeStoredQuantity() {
        val stored = 2000.0
        assertEquals(2.0, WaterConversion.fromMilliliters(stored, WaterDisplayUnit.LITERS), 0.0)
        assertEquals(8.0, WaterConversion.fromMilliliters(stored, WaterDisplayUnit.CUPS), 0.0)
        assertEquals(2000.0, stored, 0.0)
    }

    @Test
    fun aggregationAndDeletionUseRemainingCanonicalLogs() {
        val first = WaterLogEntity("1", 1000.0, 1.0, WaterUnit.LITERS, "My bottle", 1L)
        val second = WaterLogEntity("2", 500.0, 2.0, WaterUnit.CUPS, null, 2L)
        assertEquals(1500.0, WaterLogSummary.totalMl(listOf(first, second)), 0.0)
        assertEquals(1000.0, WaterLogSummary.totalMl(listOf(first)), 0.0)
    }
}
