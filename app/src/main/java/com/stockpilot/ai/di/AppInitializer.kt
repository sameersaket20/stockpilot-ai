package com.stockpilot.ai.di

import com.stockpilot.ai.data.repository.InventoryRepository
import com.stockpilot.ai.models.ModelStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val modelStorage: ModelStorage,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            inventoryRepository.ensureSeeded()
            modelStorage.refreshFromDisk()
        }
    }
}