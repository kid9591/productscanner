package com.kid.productscanner.presentation.scan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kid.productscanner.presentation.select_tracking.viewmodel.SelectTrackingViewModel
import com.kid.productscanner.repository.ScannerRepository

class ScanViewModelFactory(private val repository: ScannerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}