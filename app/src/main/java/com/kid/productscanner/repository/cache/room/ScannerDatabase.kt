package com.kid.productscanner.repository.cache.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kid.productscanner.repository.cache.room.entity.Excel
import com.kid.productscanner.repository.cache.room.dao.ExcelDao
import com.kid.productscanner.repository.cache.room.entity.Pack
import com.kid.productscanner.repository.cache.room.dao.PackageDao

@Database(entities = [Excel::class, Pack::class], version = 1)
abstract class ScannerDatabase : RoomDatabase() {
    abstract fun excelDao(): ExcelDao
    abstract fun packageDao(): PackageDao
}
