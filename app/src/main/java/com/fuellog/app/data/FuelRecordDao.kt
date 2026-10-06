package com.fuellog.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelRecordDao {

    @Query("SELECT * FROM fuel_records ORDER BY date ASC, id ASC")
    fun observeAll(): Flow<List<FuelRecord>>

    @Query("SELECT * FROM fuel_records WHERE id = :id")
    suspend fun findById(id: Long): FuelRecord?

    @Insert
    suspend fun insert(record: FuelRecord): Long

    @Update
    suspend fun update(record: FuelRecord)

    @Delete
    suspend fun delete(record: FuelRecord)

    @Query("DELETE FROM fuel_records")
    suspend fun deleteAll()
}
