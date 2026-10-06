package com.fuellog.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_records")
data class FuelRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 給油日 yyyy-MM-dd(初期値は読み取った日) */
    val date: String,
    /** 給油量 (L) */
    val fuelLiters: Double,
    /** 金額 (円) */
    val costYen: Int = 0,
    /** 単価 (円/L) */
    val unitPrice: Double? = null,
    /** オドメーター: 総走行距離 (km) — メモとして全期間保持 */
    val odometerKm: Double? = null,
    /** トリップメーター: 区間距離 (km) */
    val tripKm: Double? = null,
    /** 撮影したレシート写真のパス */
    val receiptPhotoPath: String? = null,
    /** 撮影したメーター写真のパス */
    val meterPhotoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** 走行距離・燃費の計算結果を付加した表示用モデル */
data class FuelEntry(
    val record: FuelRecord,
    /** 燃費計算に使用した走行距離(トリップ優先 / なければオド差分) */
    val distanceKm: Double?,
    val economyKmPerLiter: Double?
)
