package com.kid.productscanner.presentation.select_tracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kid.productscanner.repository.ScannerRepository

class SelectTrackingViewModelFactory(private val repository: ScannerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectTrackingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SelectTrackingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}