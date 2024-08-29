package com.kid.productscanner.room.dao

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

class Package(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "projectName") val projectName: String?,
    @ColumnInfo(name = "partNumber") val partNumber: String?,
    @ColumnInfo(name = "trackingNumber") val trackingNumber: Int?
)