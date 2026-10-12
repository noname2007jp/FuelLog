package com.fuellog.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FuelRepositoryTest {

    @Test
    fun fuelEconomyUsesTripAAndDoesNotUseTripBAsDistance() {
        val records = listOf(
            FuelRecord(
                id = 1,
                date = "2026-09-01",
                fuelLiters = 30.0,
                odometerKm = 1000.0
            ),
            FuelRecord(
                id = 2,
                date = "2026-10-01",
                fuelLiters = 30.0,
                odometerKm = 1500.0,
                tripAKm = 420.0,
                tripKm = 987.6
            )
        )

        val current = FuelRepository.calculate(records).last()
        assertEquals(420.0, current.distanceKm!!, 0.0001)
        assertEquals(14.0, current.economyKmPerLiter!!, 0.0001)
    }

    @Test
    fun fuelEconomyFallsBackToOdometerDifferenceWhenTripAIsMissing() {
        val records = listOf(
            FuelRecord(
                id = 1,
                date = "2026-09-01",
                fuelLiters = 30.0,
                odometerKm = 1000.0
            ),
            FuelRecord(
                id = 2,
                date = "2026-10-01",
                fuelLiters = 30.0,
                odometerKm = 1500.0,
                tripKm = 987.6
            )
        )

        val current = FuelRepository.calculate(records).last()
        assertEquals(500.0, current.distanceKm!!, 0.0001)
        assertEquals(500.0 / 30.0, current.economyKmPerLiter!!, 0.0001)
    }
}
