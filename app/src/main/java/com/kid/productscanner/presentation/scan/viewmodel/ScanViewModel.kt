package com.kid.productscanner.presentation.scan.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kid.productscanner.repository.ScannerRepository
import com.kid.productscanner.repository.cache.room.entity.Pack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ScanViewModel(private val scannerRepository: ScannerRepository) : ViewModel() {

    val selectedTrackingNumber
        get() = selectedTrackingNumberLiveData.value
    val selectedTrackingNumberLiveData = MutableLiveData("")
    fun setSelectedTrackingNumber(trackingNumber: String) {
        selectedTrackingNumberLiveData.value = trackingNumber
    }

    suspend fun updatePack(pack: Pack) =
        viewModelScope.async(Dispatchers.IO) {
            scannerRepository.updatePack(pack) == 1
        }.await()

    val packsBelongsToTrackingNumber
        get() = packsBelongsToTrackingNumberLiveData.value
    val packsBelongsToTrackingNumberLiveData = MutableLiveData<List<Pack>>()

    fun getPackBelongsTo(trackingNumber: String) =
        viewModelScope.launch(Dispatchers.IO) {
            val packs = scannerRepository.getPackBelongsTo(trackingNumber)
            packsBelongsToTrackingNumberLiveData.postValue(packs)
        }

    fun findPackWith(partNumber: String): Pack? =
        packsBelongsToTrackingNumber?.firstOrNull { it.partNumber.trim() == partNumber.trim() }
            ?: packsBelongsToTrackingNumber?.firstOrNull {
                partNumber.replace("O", "0").trim().lowercase() == it.partNumber.trim().lowercase()
            }
            ?: packsBelongsToTrackingNumber?.firstOrNull {
                partNumber.replace("0", "O").trim().lowercase() == it.partNumber.trim().lowercase()
            }

    fun findInAllPacksWith(partNumber: String): Pack? =
        allPacks?.firstOrNull { it.partNumber.trim() == partNumber.trim() }
            ?: allPacks?.firstOrNull {
                partNumber.replace("O", "0").trim().lowercase() == it.partNumber.trim().lowercase()
            }
            ?: allPacks?.firstOrNull {
                partNumber.replace("0", "O").trim().lowercase() == it.partNumber.trim().lowercase()
            }

    val allPacks
        get() = allPacksLiveData.value
    val allPacksLiveData = MutableLiveData<List<Pack>>()

    fun getAllPacks() =
        viewModelScope.launch(Dispatchers.IO) {
            val packs = scannerRepository.getAllPacks()
            allPacksLiveData.postValue(packs)
        }

    var shortestPartNumber: String = ""
    fun findShortestPartNumber() =
        viewModelScope.launch(Dispatchers.IO) {
            shortestPartNumber = scannerRepository.findShortestPartNumber()
            Log.d("chi.trinh", "findShortestPartNumber: $shortestPartNumber")
        }
}