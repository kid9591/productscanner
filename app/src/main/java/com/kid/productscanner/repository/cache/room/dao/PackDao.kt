package com.kid.productscanner.repository.cache.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kid.productscanner.repository.cache.room.entity.Pack

@Dao
interface PackDao {
//    @Query("SELECT * FROM package ORDER BY id DESC LIMIT 1")
//    fun loadLast(): Excel

    @Insert
    fun insert(pack: Pack)
//
//    @Delete
//    fun delete(excel: Excel)

    @Update
    fun update(pack: Pack)

    @Insert
    fun insertAll(packs: List<Pack>)

    @Query("SELECT DISTINCT trackingNumber FROM pack")
    fun getDistinctTrackingNumbers(): List<String>

    @Query("SELECT partNumber FROM pack WHERE trackingNumber = :trackingNumber GROUP BY partNumber\n" +
            "ORDER BY LENGTH(partNumber) ASC\n" +
            "LIMIT 1")
    fun findShortestPartNumber(trackingNumber: String): String

    @Query("SELECT partNumber FROM pack WHERE trackingNumber = :trackingNumber")
    fun getPartNumbersOf(trackingNumber: String): List<String>
}