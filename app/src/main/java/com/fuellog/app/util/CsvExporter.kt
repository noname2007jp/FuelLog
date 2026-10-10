package com.fuellog.app.util

import com.fuellog.app.data.FuelEntry

object CsvExporter {

    fun build(entriesAsc: List<FuelEntry>): String {
        val sb = StringBuilder()
        sb.appendLine("日付,オドメーター(km),区間距離(km),給油量(L),燃費(km/L),金額(円),単価(円/L)")
        for (entry in entriesAsc) {
            val r = entry.record
            sb.appendLine(
                listOf(
                    r.date,
                    r.odometerKm?.let { Formatters.fmt1(it) } ?: "",
                    entry.distanceKm?.let { Formatters.fmt1(it) } ?: "",
                    Formatters.fmt2(r.fuelLiters),
                    entry.economyKmPerLiter?.let { Formatters.fmt1(it) } ?: "",
                    r.costYen.toString(),
                    r.unitPrice?.let { Formatters.fmt2(it) } ?: ""
                ).joinToString(",")
            )
        }
        return sb.toString()
    }
}
