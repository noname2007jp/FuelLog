package com.fuellog.app.util

import com.fuellog.app.data.FuelRecord
import java.time.LocalDate

/** Imports FuelLog CSV files, including the previous export format. */
object CsvImporter {
    data class Result(
        val records: List<FuelRecord>,
        val skippedRows: Int,
        val errors: List<String>
    )

    fun parse(csv: String): Result {
        val rows = parseRows(csv.removePrefix("\uFEFF"))
            .filter { row -> row.any { it.isNotBlank() } }
        if (rows.size < 2) return Result(emptyList(), 0, listOf("CSVにデータ行がありません"))

        val header = rows.first().map { normalizeHeader(it) }
        fun column(vararg names: String): Int = header.indexOfFirst { h -> names.any { h.contains(normalizeHeader(it)) } }
        val dateCol = column("日付", "date")
        val odometerCol = column("オドメーター", "odometer")
        val tripACol = column("トリップA", "tripa", "trip a")
        val tripBCol = column("トリップB", "区間距離", "tripb", "trip b", "trip")
        val fuelCol = column("給油量", "fuel")
        val costCol = column("金額", "cost")
        val priceCol = column("単価", "unit price", "unitprice")
        val vehicleEconomyCol = column("車載燃費", "車両燃費", "vehicle fuel economy", "vehiclefueleconomy")
        if (dateCol < 0 || fuelCol < 0) {
            return Result(emptyList(), 0, listOf("日付と給油量の列が見つかりません"))
        }

        val records = mutableListOf<FuelRecord>()
        val errors = mutableListOf<String>()
        var skipped = 0
        rows.drop(1).forEachIndexed { index, row ->
            fun value(col: Int): String? = row.getOrNull(col)?.trim()?.takeIf { it.isNotEmpty() }
            val date = value(dateCol)
            val liters = value(fuelCol)?.toDoubleCsv()
            if (date == null || !isValidDate(date) || liters == null || liters <= 0.0) {
                skipped++
                errors += "${index + 2}行目: 日付または給油量が不正です"
            } else {
                records += FuelRecord(
                    date = date,
                    fuelLiters = liters,
                    costYen = value(costCol)?.toDoubleCsv()?.toInt() ?: 0,
                    unitPrice = value(priceCol)?.toDoubleCsv(),
                    odometerKm = value(odometerCol)?.toDoubleCsv(),
                    tripAKm = value(tripACol)?.toDoubleCsv(),
                    tripKm = value(tripBCol)?.toDoubleCsv(),
                    vehicleFuelEconomyKmPerLiter = value(vehicleEconomyCol)?.toDoubleCsv(),
                    createdAt = System.currentTimeMillis()
                )
            }
        }
        return Result(records, skipped, errors.take(20))
    }

    private fun isValidDate(value: String): Boolean =
        try { LocalDate.parse(value); true } catch (_: Exception) { false }

    private fun String.toDoubleCsv(): Double? =
        trim().replace(",", "").replace("，", "").replace("．", ".")
            .replace("－", "-").toDoubleOrNull()

    private fun normalizeHeader(value: String): String =
        value.trim().lowercase().replace(" ", "").replace("（", "(").replace("）", ")")

    private fun parseRows(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < csv.length) {
            val ch = csv[i]
            when {
                ch == '"' && quoted && i + 1 < csv.length && csv[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> {
                    row += field.toString()
                    field.setLength(0)
                }
                (ch == '\n' || ch == '\r') && !quoted -> {
                    if (ch == '\r' && i + 1 < csv.length && csv[i + 1] == '\n') i++
                    row += field.toString()
                    rows += row
                    row = mutableListOf()
                    field.setLength(0)
                }
                else -> field.append(ch)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString()
            rows += row
        }
        return rows
    }
}
