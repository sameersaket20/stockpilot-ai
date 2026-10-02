package com.stockpilot.ai.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.stockpilot.ai.StockPilotApplication

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

@Composable
fun ProvideAppContainer(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as StockPilotApplication).container
    CompositionLocalProvider(LocalAppContainer provides container) {
        content()
    }
}