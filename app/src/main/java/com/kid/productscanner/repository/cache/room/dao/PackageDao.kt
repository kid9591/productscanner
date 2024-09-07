package com.kid.productscanner.repository.cache.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import com.kid.productscanner.repository.cache.room.entity.Pack

@Dao
interface PackageDao {
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
}