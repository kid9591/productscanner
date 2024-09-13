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

    suspend fun updatePacks(packs: List<Pack>) =
        viewModelScope.async(Dispatchers.IO) {
            scannerRepository.updatePacks(packs)
        }.await()

    val packsBelongsToTrackingNumber
        get() = packsBelongsToTrackingNumberLiveData.value
    val packsBelongsToTrackingNumberLiveData = MutableLiveData<List<Pack>>()

    fun getPackBelongsTo(trackingNumber: String) =
        viewModelScope.launch(Dispatchers.IO) {
            val packs = scannerRepository.getPackBelongsTo(trackingNumber)
            packsBelongsToTrackingNumberLiveData.postValue(packs)
        }

    fun findInTrackingPacksWith(partNumber: String): List<Pack> =
        packsBelongsToTrackingNumber?.filter {
            it.partNumber.trim() == partNumber.trim()
        } ?: packsBelongsToTrackingNumber?.filter {
            partNumber.replace("O", "0").trim().lowercase() == it.partNumber.trim().lowercase()
        } ?: packsBelongsToTrackingNumber?.filter {
            partNumber.replace("0", "O").trim().lowercase() == it.partNumber.trim().lowercase()
        } ?: emptyList()

    fun findInAllPacksWith(partNumber: String): List<Pack> =
        allPacks?.filter {
            it.partNumber.trim() == partNumber.trim()
        } ?: allPacks?.filter {
            partNumber.replace("O", "0").trim().lowercase() == it.partNumber.trim().lowercase()
        } ?: allPacks?.filter {
            partNumber.replace("0", "O").trim().lowercase() == it.partNumber.trim().lowercase()
        } ?: emptyList()

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
        }

    val showNextPackButtonLiveData = MutableLiveData(false)
    val showBackPackButtonLiveData = MutableLiveData(false)
}