package com.kid.productscanner.repository.cache.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Excel(
    val name: String,
    val importMillis: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)