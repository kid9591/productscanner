package com.kid.productscanner.repository.cache.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kid.productscanner.repository.cache.room.entity.Pack

@Dao
interface PackDao {
    @Insert
    fun insert(pack: Pack)

    @Update
    fun update(pack: Pack): Int

    @Update(onConflict = OnConflictStrategy.ABORT)
    fun update(packs: List<Pack>): Int

    @Insert
    fun insertAll(packs: List<Pack>)

    @Query("SELECT DISTINCT trackingNumber FROM pack")
    fun getDistinctTrackingNumbers(): List<String>

    @Query("SELECT partNumber FROM pack WHERE trackingNumber = :trackingNumber GROUP BY partNumber\n" +
            "ORDER BY LENGTH(partNumber) ASC\n" +
            "LIMIT 1")
    fun findShortestPartNumber(trackingNumber: String): String

    @Query("SELECT partNumber FROM pack\n" +
            "ORDER BY LENGTH(partNumber) ASC\n" +
            "LIMIT 1")
    fun findShortestPartNumber(): String

    @Query("SELECT * FROM pack WHERE trackingNumber = :trackingNumber")
    fun getPackBelongsTo(trackingNumber: String): List<Pack>

    @Query("SELECT * FROM Pack WHERE quantityReceived IS NOT NULL AND quantityReceived != ''")
    fun getChangedPacks(): List<Pack>

    @Query("SELECT * FROM Pack")
    fun getAllPacks(): List<Pack>
}