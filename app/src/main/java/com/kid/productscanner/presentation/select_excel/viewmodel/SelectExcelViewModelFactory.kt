package com.kid.productscanner.presentation.select_excel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kid.productscanner.repository.ScannerRepository

class SelectExcelViewModelFactory(private val repository: ScannerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectExcelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SelectExcelViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}