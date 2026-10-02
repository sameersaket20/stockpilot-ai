package com.stockpilot.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stockpilot.ai.ui.theme.StockPilotSurface
import com.stockpilot.ai.ui.theme.StockPilotTextSecondary

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Card(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription ?: "$label $value"
        },
        colors = CardDefaults.cardColors(containerColor = StockPilotSurface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, color = StockPilotTextSecondary, style = MaterialTheme.typography.labelLarge)
            Text(value, color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }
    }
}