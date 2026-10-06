package com.fuellog.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FuelRepository(private val dao: FuelRecordDao) {

    /** 日付昇順で取得し、走行距離・燃費を計算して返す */
    fun observeEntries(): Flow<List<FuelEntry>> =
        dao.observeAll().map { records -> calculate(records) }

    suspend fun findById(id: Long): FuelRecord? = dao.findById(id)
    suspend fun insert(record: FuelRecord): Long = dao.insert(record)
    suspend fun update(record: FuelRecord) = dao.update(record)
    suspend fun delete(record: FuelRecord) = dao.delete(record)
    suspend fun deleteAll() = dao.deleteAll()

    companion object {
        /**
         * 走行距離の決定ルール:
         *  1. トリップメーター値があればそれを優先
         *  2. なければ前回レコードとのオドメーター差分
         * 燃費 = 走行距離 / 給油量
         */
        fun calculate(recordsAsc: List<FuelRecord>): List<FuelEntry> {
            val result = ArrayList<FuelEntry>(recordsAsc.size)
            var prevOdometer: Double? = null
            for (r in recordsAsc) {
                val distance = when {
                    r.tripKm != null && r.tripKm > 0 -> r.tripKm
                    r.odometerKm != null && prevOdometer != null && r.odometerKm > prevOdometer ->
                        r.odometerKm - prevOdometer
                    else -> null
                }
                if (r.odometerKm != null) prevOdometer = r.odometerKm
                val economy = if (distance != null && distance > 0 && r.fuelLiters > 0) {
                    distance / r.fuelLiters
                } else {
                    null
                }
                result += FuelEntry(r, distance, economy)
            }
            return result
        }
    }
}
