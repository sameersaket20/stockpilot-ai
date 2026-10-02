package com.stockpilot.ai

import android.app.Application
import com.stockpilot.ai.di.AppContainer

class StockPilotApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}