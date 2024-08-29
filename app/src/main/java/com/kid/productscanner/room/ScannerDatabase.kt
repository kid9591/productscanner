package com.kid.productscanner.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kid.productscanner.room.dao.Excel
import com.kid.productscanner.room.dao.ExcelDao

@Database(entities = [Excel::class], version = 1)
abstract class ScannerDatabase : RoomDatabase() {
    abstract fun excelDao(): ExcelDao
}
