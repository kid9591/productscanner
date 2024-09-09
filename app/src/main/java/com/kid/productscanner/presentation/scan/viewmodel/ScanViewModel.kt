package com.kid.productscanner.presentation.scan.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kid.productscanner.repository.ScannerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class ScanViewModel(private val scannerRepository: ScannerRepository) : ViewModel() {
    suspend fun getPartNumbersOf(trackingNumber: String): List<String> =
        viewModelScope.async(Dispatchers.IO) {
            scannerRepository.getPartNumbersOf(trackingNumber)
        }.await()

    fun setSelectedTrackingNumber(trackingNumber: String) {
        selectedTrackingNumberLiveData.value = trackingNumber
    }

    val selectedTrackingNumber
        get() = selectedTrackingNumberLiveData.value
    val selectedTrackingNumberLiveData = MutableLiveData("")
}