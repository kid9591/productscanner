package com.kid.productscanner.repository

import androidx.lifecycle.LiveData
import com.kid.productscanner.repository.cache.room.ScannerDatabase
import com.kid.productscanner.repository.cache.room.entity.Excel
import com.kid.productscanner.repository.cache.room.entity.Pack

class ScannerRepository(private val scannerDatabase: ScannerDatabase) {

    fun getLastExcel(): LiveData<Excel?> {
        return scannerDatabase.excelDao().getLast()
    }

    fun insertPackages(packs: List<Pack>) {
        scannerDatabase.packageDao().insertAll(packs)
    }

    fun insertExcel(excel: Excel): Long =
        scannerDatabase.excelDao().insert(excel)

    fun deleteAllExcel() {
        scannerDatabase.excelDao().deleteAll()
    }
}