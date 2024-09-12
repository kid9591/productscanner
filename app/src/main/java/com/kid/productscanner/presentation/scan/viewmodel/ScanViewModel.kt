package com.kid.productscanner.presentation.scan.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kid.productscanner.repository.ScannerRepository
import com.kid.productscanner.repository.cache.room.entity.Pack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ScanViewModel(private val scannerRepository: ScannerRepository) : ViewModel() {
    fun getPackBelongsTo(trackingNumber: String) =
        viewModelScope.launch(Dispatchers.IO) {
            val packs = scannerRepository.getPackBelongsTo(trackingNumber)
            packsBelongsToTrackingNumberLiveData.postValue(packs)
        }

    fun setSelectedTrackingNumber(trackingNumber: String) {
        selectedTrackingNumberLiveData.value = trackingNumber
    }

    suspend fun updatePack(pack: Pack) =
        viewModelScope.async(Dispatchers.IO) {
            scannerRepository.updatePack(pack) == 1
        }.await()

    fun findPackWith(partNumber: String): Pack? =
        packsBelongsToTrackingNumber?.firstOrNull { it.partNumber.trim() == partNumber.trim() }
            ?: packsBelongsToTrackingNumber?.firstOrNull {
                partNumber.replace("O", "0").trim().lowercase() == it.partNumber.trim().lowercase()
            }
            ?: packsBelongsToTrackingNumber?.firstOrNull {
                partNumber.replace("0", "O").trim().lowercase() == it.partNumber.trim().lowercase()
            }

    val selectedTrackingNumber
        get() = selectedTrackingNumberLiveData.value
    val selectedTrackingNumberLiveData = MutableLiveData("")

    val packsBelongsToTrackingNumber
        get() = packsBelongsToTrackingNumberLiveData.value
    val packsBelongsToTrackingNumberLiveData = MutableLiveData<List<Pack>>()
}