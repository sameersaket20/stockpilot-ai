package com.stockpilot.ai.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stockpilot.ai.ui.theme.StockPilotTeal
import com.stockpilot.ai.ui.theme.StockPilotTextSecondary

@Composable
fun OfflinePill(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = StockPilotTeal,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Offline • local data",
            modifier = Modifier.padding(start = 4.dp),
            color = StockPilotTextSecondary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}