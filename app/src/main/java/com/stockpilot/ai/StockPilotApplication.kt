package com.stockpilot.ai

import android.app.Application
import com.stockpilot.ai.di.AppInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StockPilotApplication : Application() {

    @Inject lateinit var appInitializer: AppInitializer

    override fun onCreate() {
        super.onCreate()
        appInitializer.start()
    }
}