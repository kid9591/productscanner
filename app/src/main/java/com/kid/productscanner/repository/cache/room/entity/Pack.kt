package com.kid.productscanner.repository.cache.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = Excel::class,
        parentColumns = ["id"],
        childColumns = ["excelId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Pack(
    @PrimaryKey
    val bookingNo: Long,
    val projectName: String,
    val partNumber: String,
    val trackingNumber: String,
    var quantityReceived: String,
    var dateReceivedMillis: Long,
    val excelId: Int
)