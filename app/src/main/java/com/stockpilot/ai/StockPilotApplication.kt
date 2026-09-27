package com.stockpilot.ai

import android.app.Application
import com.stockpilot.ai.data.local.StockPilotDatabase
import com.stockpilot.ai.data.repository.InventoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StockPilotApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            val db = StockPilotDatabase.build(this@StockPilotApplication)
            InventoryRepository(this@StockPilotApplication, db.productDao()).ensureSeeded()
        }
    }
}
