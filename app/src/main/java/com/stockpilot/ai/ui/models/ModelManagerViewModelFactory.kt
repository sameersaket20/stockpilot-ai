package com.stockpilot.ai.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stockpilot.ai.models.ModelImportManager
import com.stockpilot.ai.models.ModelStorage

class ModelManagerViewModelFactory(
    private val storage: ModelStorage,
    private val importManager: ModelImportManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ModelManagerViewModel::class.java)) {
            return ModelManagerViewModel(storage, importManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    }
}