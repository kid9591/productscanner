package com.kid.productscanner.select_excel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.kid.productscanner.repository.ScannerRepository
import com.kid.productscanner.repository.cache.room.entity.Excel
import com.kid.productscanner.repository.cache.room.entity.Pack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import android.view.View

class SelectExcelViewModel(private val scannerRepository: ScannerRepository) : ViewModel() {

    fun deleteAllExcel() {
        viewModelScope.launch(Dispatchers.IO) {
            scannerRepository.deleteAllExcel()
        }
    }

    fun insertPackages(packs: List<Pack>) {
        viewModelScope.launch(Dispatchers.IO) {
            scannerRepository.insertPackages(packs)
        }
    }

    suspend fun insertExcel(excel: Excel): Long {
        return viewModelScope.async(Dispatchers.IO) {
            scannerRepository.insertExcel(excel)
        }.await()
    }

//    init {
//        Log.d("Debug", "init: ")
//        viewModelScope.launch(Dispatchers.IO) {
//            scannerRepository.getLastExcel()?.let { excel ->
//                lastExcelLiveData.postValue(excel)
//            }
//        }
//    }

//    fun initialize() {
//        viewModelScope.launch(Dispatchers.IO) {
//            scannerRepository.getLastExcel()?.let { excel ->
//                lastExcelLiveData.postValue(excel)
//            }
//        }
//    }

    //    val lastExcelLiveData = MutableLiveData<Excel>()
    val lastExcelLiveData = liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
        scannerRepository.getLastExcel()?.let { excel ->
            emit(excel)
        }
    }

    val noExcelLiveData = lastExcelLiveData.switchMap {
        liveData(viewModelScope.coroutineContext) {
            val visiblity = if (it != null) View.GONE else View.VISIBLE
            emit(visiblity)
        }
    }
    val lastExcelNameLiveData = lastExcelLiveData.switchMap {
        liveData(viewModelScope.coroutineContext) {
            emit(it.name)
        }
    }
    val lastExcelDateLiveData = lastExcelLiveData.switchMap {
        liveData(viewModelScope.coroutineContext) {
            val date = Date(it.importMillis)
            emit(SimpleDateFormat.getDateTimeInstance().format(date))
        }
    }
}