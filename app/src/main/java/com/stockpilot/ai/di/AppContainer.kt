package com.stockpilot.ai.di

import android.content.Context
import com.stockpilot.ai.data.local.StockPilotDatabase
import com.stockpilot.ai.data.repository.InventoryRepository
import com.stockpilot.ai.domain.assistant.AssistantOrchestrator
import com.stockpilot.ai.domain.inventory.InventoryCommandExecutor
import com.stockpilot.ai.inference.DemoInferenceEngine
import com.stockpilot.ai.models.ModelImportManager
import com.stockpilot.ai.models.ModelStorage
import com.stockpilot.ai.speech.AndroidTtsManager
import com.stockpilot.ai.speech.AudioRecorder
import com.stockpilot.ai.speech.UninstalledSpeechEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database = StockPilotDatabase.build(appContext)
    val inventoryRepository = InventoryRepository(appContext, database.productDao())
    val inventoryCommandExecutor = InventoryCommandExecutor(inventoryRepository)

    val modelStorage = ModelStorage(appContext)
    val modelImportManager = ModelImportManager(appContext, modelStorage)

    val demoInferenceEngine = DemoInferenceEngine()
    val speechEngine = UninstalledSpeechEngine(modelStorage)
    val assistantOrchestrator = AssistantOrchestrator(demoInferenceEngine, inventoryCommandExecutor)

    val audioRecorder = AudioRecorder(appContext)
    val ttsManager = AndroidTtsManager(appContext)

    init {
        applicationScope.launch(Dispatchers.IO) {
            inventoryRepository.ensureSeeded()
            modelStorage.refreshFromDisk()
        }
        ttsManager.initialize()
    }
}