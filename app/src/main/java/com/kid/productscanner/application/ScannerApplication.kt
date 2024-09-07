package com.kid.productscanner.application

import android.app.Application
import androidx.room.Room
import com.kid.productscanner.repository.cache.room.ScannerDatabase

class ScannerApplication : Application() {

    lateinit var scannerDatabase: ScannerDatabase

    override fun onCreate() {
        super.onCreate()

        scannerDatabase = Room.databaseBuilder(
            applicationContext,
            ScannerDatabase::class.java, "scanner_db"
        ).build()
    }
}