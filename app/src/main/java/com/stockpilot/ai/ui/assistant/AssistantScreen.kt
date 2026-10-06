package com.stockpilot.ai.ui.assistant

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpilot.ai.di.LocalAppContainer
import com.stockpilot.ai.ui.components.DemoModeBanner
import com.stockpilot.ai.ui.theme.StockPilotNavy
import com.stockpilot.ai.ui.theme.StockPilotSurface
import com.stockpilot.ai.ui.theme.StockPilotSurfaceElevated
import com.stockpilot.ai.ui.theme.StockPilotTeal

@Composable
fun AssistantRoute(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: AssistantViewModel = viewModel(
        factory = AssistantViewModelFactory(
            container.assistantOrchestrator,
            container.speechEngine,
            container.audioRecorder,
            container.ttsManager,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasMicPermission = container.audioRecorder.hasMicPermission()
    AssistantScreen(state, viewModel, onBack, hasMicPermission)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    state: AssistantUiState,
    viewModel: AssistantViewModel,
    onBack: () -> Unit,
    hasMicPermission: Boolean,
) {
    val listState = rememberLazyListState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onMicPermissionResult(granted) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = StockPilotNavy,
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StockPilotNavy),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Inventory Assistant") },
                actions = {
                    IconButton(
                        onClick = viewModel::toggleTtsMute,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            if (state.ttsMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            contentDescription = if (state.ttsMuted) "Unmute speech" else "Mute speech",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            DemoModeBanner(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            state.ttsWarning?.let {
                Text(
                    it,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (state.micPermissionDenied) {
                Text(
                    "Microphone denied — typed input still works.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(message)
                }
                if (state.isProcessing) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Processing…", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "How many blue T-shirts?",
                    "What needs restocking?",
                    "How many should I order?",
                ).forEach { prompt ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.sendSuggested(prompt) },
                        label = { Text(prompt) },
                    )
                }
            }
            if (state.recording) {
                RecordingIndicator()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about stock or reorder math") },
                    singleLine = false,
                    maxLines = 3,
                )
                IconButton(
                    onClick = {
                        viewModel.onMicPressed(hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = if (state.recording) "Stop recording" else "Record voice",
                        tint = if (state.recording) MaterialTheme.colorScheme.error else StockPilotTeal,
                    )
                }
                IconButton(
                    onClick = viewModel::sendMessage,
                    enabled = state.input.isNotBlank() && !state.isProcessing,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    val bg = when (message.role) {
        ChatRole.USER -> StockPilotSurfaceElevated
        ChatRole.ASSISTANT -> StockPilotSurface
        ChatRole.SYSTEM -> Color.Transparent
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .fillMaxWidth(if (message.role == ChatRole.SYSTEM) 1f else 0.88f)
                .background(bg, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun RecordingIndicator() {
    val transition = rememberInfiniteTransition(label = "rec")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse",
    )
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(MaterialTheme.colorScheme.error, CircleShape),
        )
        Text("Recording… tap mic to stop", modifier = Modifier.padding(start = 8.dp))
    }
}