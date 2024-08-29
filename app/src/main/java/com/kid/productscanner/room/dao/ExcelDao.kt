package com.kid.productscanner.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExcelDao {
    @Query("SELECT * FROM excel ORDER BY id DESC LIMIT 1")
    fun loadLast(): List<Excel>

    @Insert
    fun insert(excel: Excel)

    @Delete
    fun delete(excel: Excel)
}