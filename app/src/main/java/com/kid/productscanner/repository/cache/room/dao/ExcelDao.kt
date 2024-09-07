package com.kid.productscanner.repository.cache.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kid.productscanner.repository.cache.room.entity.Excel

@Dao
interface ExcelDao {
    @Query("SELECT * FROM excel ORDER BY id DESC LIMIT 1")
    fun getLast(): LiveData<Excel?>

    @Insert
    fun insert(excel: Excel): Long

    @Query("DELETE FROM excel")
    fun deleteAll()
}