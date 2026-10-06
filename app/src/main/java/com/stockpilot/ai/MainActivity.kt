package com.stockpilot.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.stockpilot.ai.di.ProvideAppContainer
import com.stockpilot.ai.ui.navigation.StockPilotApp
import com.stockpilot.ai.ui.theme.StockPilotNavy
import com.stockpilot.ai.ui.theme.StockPilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val version = BuildConfig.VERSION_NAME
        setContent {
            StockPilotTheme {
                ProvideAppContainer {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = StockPilotNavy,
                    ) {
                        StockPilotApp(appVersion = version)
                    }
                }
            }
        }
    }
}