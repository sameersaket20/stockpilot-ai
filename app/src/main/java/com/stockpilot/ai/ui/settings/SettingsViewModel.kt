package com.stockpilot.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.ai.data.repository.InventoryRepository
import com.stockpilot.ai.inference.ModelStatus
import com.stockpilot.ai.models.ModelStorage
import com.stockpilot.ai.models.ModelType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: InventoryRepository,
    modelStorage: ModelStorage,
) : ViewModel() {
    val gemmaStatus: StateFlow<ModelStatus> = modelStorage.status(ModelType.GEMMA)

    fun resetSeedInventory() {
        viewModelScope.launch {
            repository.resetSeedData()
        }
    }
}