package com.stockpilot.ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stockpilot.ai.ui.assistant.AssistantRoute
import com.stockpilot.ai.ui.inventory.InventoryRoute
import com.stockpilot.ai.ui.inventory.ProductFocusRoute
import com.stockpilot.ai.ui.models.ModelManagerRoute
import com.stockpilot.ai.ui.settings.SettingsRoute

object Routes {
    const val INVENTORY = "inventory"
    const val INVENTORY_WITH_PRODUCT = "inventory?productId={productId}"
    const val ASSISTANT = "assistant"
    const val MODELS = "models"
    const val SETTINGS = "settings"
    const val PRODUCT_FOCUS = "product/{productId}"

    fun productFocus(productId: Long): String = "product/$productId"

    fun inventory(productId: Long? = null): String = if (productId != null && productId > 0L) {
        "inventory?productId=$productId"
    } else {
        INVENTORY
    }
}

@Composable
fun StockPilotApp(
    navController: NavHostController = rememberNavController(),
    appVersion: String,
) {
    NavHost(navController = navController, startDestination = Routes.INVENTORY) {
        composable(
            route = Routes.INVENTORY_WITH_PRODUCT,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val productId = entry.arguments?.getLong("productId") ?: -1L
            InventoryRoute(
                highlightProductId = productId,
                onOpenAssistant = { navController.navigate(Routes.ASSISTANT) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.ASSISTANT) {
            AssistantRoute(
                onBack = { navController.popBackStack() },
                onOpenProduct = { productId ->
                    navController.navigate(Routes.productFocus(productId))
                },
            )
        }
        composable(
            route = Routes.PRODUCT_FOCUS,
            arguments = listOf(
                navArgument("productId") { type = NavType.LongType },
            ),
        ) { entry ->
            val productId = entry.arguments?.getLong("productId") ?: -1L
            ProductFocusRoute(
                productId = productId,
                onBack = { navController.popBackStack() },
            )
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