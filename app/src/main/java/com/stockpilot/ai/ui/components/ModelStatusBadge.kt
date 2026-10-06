package com.stockpilot.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stockpilot.ai.inference.ModelStatus
import com.stockpilot.ai.ui.theme.StockPilotError
import com.stockpilot.ai.ui.theme.StockPilotSurfaceElevated
import com.stockpilot.ai.ui.theme.StockPilotTeal
import com.stockpilot.ai.ui.theme.StockPilotTextSecondary
import com.stockpilot.ai.ui.theme.StockPilotWarning

@Composable
fun ModelStatusBadge(status: ModelStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        ModelStatus.NotInstalled -> "Missing" to StockPilotTextSecondary
        ModelStatus.ImportedUnverified -> "Imported (unverified)" to StockPilotWarning
        ModelStatus.Loading -> "Loading" to StockPilotWarning
        ModelStatus.Ready -> "Ready" to StockPilotTeal
        ModelStatus.Error -> "Error" to StockPilotError
    }
    Text(
        text = label,
        modifier = modifier
            .background(StockPilotSurfaceElevated, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = color,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
fun DemoModeBanner(modifier: Modifier = Modifier) {
    Text(
        text = "DEMO MODE — no AI models loaded",
        modifier = modifier
            .background(StockPilotWarning.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = StockPilotWarning,
        style = MaterialTheme.typography.labelLarge,
    )
}