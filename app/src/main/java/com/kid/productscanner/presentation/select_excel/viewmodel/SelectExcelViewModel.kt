package com.kid.productscanner.presentation.select_excel.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.kid.productscanner.common.ColumnName
import com.kid.productscanner.repository.ScannerRepository
import com.kid.productscanner.repository.cache.room.entity.Excel
import com.kid.productscanner.repository.cache.room.entity.Pack
import com.kid.productscanner.utils.findIndexOfColumn
import com.kid.productscanner.utils.findLastNonEmptyRowIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.Workbook
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class SelectExcelViewModel @Inject constructor(private val scannerRepository: ScannerRepository) : ViewModel() {

    fun deleteAllExcel() {
        viewModelScope.launch(Dispatchers.IO) {
            scannerRepository.deleteAllExcel()
        }
    }

    fun insertPackages(workbook: Workbook, excelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {

            //Lấy sheet đầu tiên (file của a Dũng chỉ có 1 sheet)
            val sheet = workbook.getSheetAt(0)
            val titleRow = sheet.getRow(0)

            val bookingNoIndex = titleRow.findIndexOfColumn(ColumnName.BookingNo)
            val projectNameIndex = titleRow.findIndexOfColumn(ColumnName.ProjectName)
            val partNumberIndex = titleRow.findIndexOfColumn(ColumnName.PartNumber)
            val trackingNumberIndex = titleRow.findIndexOfColumn(ColumnName.TrackingNumber)

            val lastRowIndex = sheet.findLastNonEmptyRowIndex()

            val packs = mutableListOf<Pack>()
            for (i in 0..lastRowIndex) {
                val row = sheet.getRow(i)
                val bookingNo = row.getCell(bookingNoIndex).stringCellValue.toLongOrNull()
                val projectName = row.getCell(projectNameIndex).stringCellValue
                val partNumber = row.getCell(partNumberIndex).stringCellValue
                val trackingNumber = row.getCell(trackingNumberIndex).stringCellValue

                if (bookingNo != null) {
                    packs.add(
                        Pack(
                            row.rowNum,
                            bookingNo,
                            projectName,
                            partNumber,
                            trackingNumber,
                            "",
                            0L,
                            excelId
                        )
                    )
                }

                percentLiveData.postValue(
                    ((i.toDouble() / lastRowIndex * 100).roundToInt())
                )
            }

            scannerRepository.insertPackages(packs)
        }
    }

    fun reloadExcel() {
        lastExcelLiveData = scannerRepository.getLastExcel()
    }

    suspend fun insertExcel(excel: Excel): Long {
        return viewModelScope.async(Dispatchers.IO) {
            scannerRepository.insertExcel(excel)
        }.await()
    }

    @SuppressLint("SimpleDateFormat")
    fun updateChangedPacksTo(workbook: Workbook) {
        val sheet = workbook.getSheetAt(0)
        val titleRow = sheet.getRow(0)

        val quantityReceivedIndex = titleRow.findIndexOfColumn(ColumnName.QuantityReceived)
        val dateReceivedIndex = titleRow.findIndexOfColumn(ColumnName.DateReceived)

        scannerRepository.getChangedPacks().forEach { pack ->
            sheet.getRow(pack.excelRowNumber)?.apply {
                getCell(quantityReceivedIndex)?.setCellValue(pack.quantityReceived)
                val dateFormated = SimpleDateFormat("yyyyMMdd").format(Date(pack.dateReceivedMillis))
                getCell(dateReceivedIndex)?.setCellValue(dateFormated)
            }
        }
    }

    var lastExcelLiveData: LiveData<Excel?> = scannerRepository.getLastExcel()

    val lastExcelName
        get() = lastExcelNameLiveData.value
    val lastExcelNameLiveData = lastExcelLiveData.switchMap {
        liveData(viewModelScope.coroutineContext) {
            emit(it?.name ?: "")
        }
    }
    @SuppressLint("SimpleDateFormat")
    val lastExcelDateLiveData = lastExcelLiveData.switchMap {
        liveData(viewModelScope.coroutineContext) {
            val date = Date(it?.importMillis ?: 0L)
            emit(SimpleDateFormat("HH:mm - dd/MM/yyyy").format(date))
        }
    }

    val percentLiveData = MutableLiveData<Int>(0)
}