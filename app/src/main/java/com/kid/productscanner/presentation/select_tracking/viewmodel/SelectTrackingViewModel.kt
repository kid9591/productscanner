package com.kid.productscanner.presentation.select_tracking.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kid.productscanner.repository.ScannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectTrackingViewModel @Inject constructor(private val scannerRepository: ScannerRepository) : ViewModel() {

    suspend fun getDistinctTrackingNumbers(): List<String> =
        viewModelScope.async(Dispatchers.IO) {
            scannerRepository.getDistinctTrackingNumbers()
        }.await()

    fun findShortestPartNumber(trackingNumber: String) =
        viewModelScope.launch(Dispatchers.IO) {
            selectedTrackingNumberLiveData.postValue(trackingNumber)
            shortestPartNumberLiveData.postValue(
                scannerRepository.findShortestPartNumber(
                    trackingNumber
                )
            )
        }

    val selectedTrackingNumber
        get() = selectedTrackingNumberLiveData.value
    var selectedTrackingNumberLiveData = MutableLiveData("")

    val shortestPartNumber
        get() = shortestPartNumberLiveData.value
    var shortestPartNumberLiveData = MutableLiveData("")

}