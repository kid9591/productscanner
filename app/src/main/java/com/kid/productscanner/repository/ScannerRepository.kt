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
        scannerDatabase.packDao().insertAll(packs)
    }

    fun insertExcel(excel: Excel): Long =
        scannerDatabase.excelDao().insert(excel)

    fun deleteAllExcel() {
        scannerDatabase.excelDao().deleteAll()
    }

    fun getDistinctTrackingNumbers(): List<String> =
        scannerDatabase.packDao().getDistinctTrackingNumbers()

    fun findShortestPartNumber(trackingNumber: String): String =
        scannerDatabase.packDao().findShortestPartNumber(trackingNumber)

    fun findShortestPartNumber(): String =
        scannerDatabase.packDao().findShortestPartNumber()

    fun getPackBelongsTo(trackingNumber: String): List<Pack> =
        scannerDatabase.packDao().getPackBelongsTo(trackingNumber)

    fun updatePack(pack: Pack) =
        scannerDatabase.packDao().update(pack)

    fun updatePacks(packs: List<Pack>) = scannerDatabase.packDao().update(packs)

    fun getChangedPacks(): List<Pack> =
        scannerDatabase.packDao().getChangedPacks()

    fun getAllPacks(): List<Pack> =
        scannerDatabase.packDao().getAllPacks()
}