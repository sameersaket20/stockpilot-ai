package com.stockpilot.ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stockpilot.ai.ui.assistant.AssistantRoute
import com.stockpilot.ai.ui.inventory.InventoryRoute
import com.stockpilot.ai.ui.models.ModelManagerRoute
import com.stockpilot.ai.ui.settings.SettingsRoute

object Routes {
    const val INVENTORY = "inventory"
    const val ASSISTANT = "assistant"
    const val MODELS = "models"
    const val SETTINGS = "settings"
}

@Composable
fun StockPilotApp(
    navController: NavHostController = rememberNavController(),
    appVersion: String,
) {
    NavHost(navController = navController, startDestination = Routes.INVENTORY) {
        composable(Routes.INVENTORY) {
            InventoryRoute(
                onOpenAssistant = { navController.navigate(Routes.ASSISTANT) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.ASSISTANT) {
            AssistantRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.MODELS) {
            ModelManagerRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onOpenModels = { navController.navigate(Routes.MODELS) },
                appVersion = appVersion,
            )
        }
    }
}