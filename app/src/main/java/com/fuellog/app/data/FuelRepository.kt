package com.fuellog.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FuelRepository(private val dao: FuelRecordDao) {

    /** 日付昇順で取得し、走行距離・燃費を計算して返す */
    fun observeEntries(): Flow<List<FuelEntry>> =
        dao.observeAll().map { records -> calculate(records) }

    suspend fun findById(id: Long): FuelRecord? = dao.findById(id)
    suspend fun insert(record: FuelRecord): Long = dao.insert(record)
    suspend fun insertAll(records: List<FuelRecord>): List<Long> = dao.insertAll(records)
    suspend fun update(record: FuelRecord) = dao.update(record)
    suspend fun delete(record: FuelRecord) = dao.delete(record)
    suspend fun deleteAll() = dao.deleteAll()

    companion object {
        /**
         * 走行距離の決定ルール:
         *  1. トリップA（前回満タンからの走行距離）があればそれを優先
         *  2. なければ前回レコードとのオドメーター差分。トリップBはODO補完用で、燃費距離には使わない
         * 燃費 = 走行距離 / 給油量
         */
        fun calculate(recordsAsc: List<FuelRecord>): List<FuelEntry> {
            val result = ArrayList<FuelEntry>(recordsAsc.size)
            var prevOdometer: Double? = null
            for (r in recordsAsc) {
                val distance = when {
                    r.tripAKm != null && r.tripAKm > 0 -> r.tripAKm
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
