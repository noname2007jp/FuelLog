package com.fuellog.app

import android.app.Application
import com.fuellog.app.data.AppDatabase
import com.fuellog.app.data.FuelRepository
import java.io.File

class FuelLogApp : Application() {

    val repository: FuelRepository by lazy {
        FuelRepository(AppDatabase.get(this).fuelRecordDao())
    }

    /** 撮影写真の保存先(アプリ専用領域) */
    val photoDir: File by lazy {
        File(filesDir, "photos").apply { mkdirs() }
    }
}
