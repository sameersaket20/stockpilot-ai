package com.stockpilot.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.ai.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: InventoryRepository,
) : ViewModel() {

    fun resetSeedInventory() {
        viewModelScope.launch {
            repository.resetSeedData()
        }
    }
}