package com.stockpilot.ai.ui.assistant

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockpilot.ai.ui.theme.StockPilotNavy
import com.stockpilot.ai.ui.theme.StockPilotSurface
import com.stockpilot.ai.ui.theme.StockPilotSurfaceElevated
import com.stockpilot.ai.domain.assistant.AssistantDeliveryMode
import com.stockpilot.ai.ui.theme.StockPilotError
import com.stockpilot.ai.ui.theme.StockPilotTeal
import com.stockpilot.ai.ui.theme.StockPilotWarning

@Composable
fun AssistantRoute(
    onBack: () -> Unit,
    onOpenProduct: (Long) -> Unit,
) {
    val viewModel: AssistantViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AssistantScreen(state, viewModel, onBack, onOpenProduct, viewModel.hasMicPermission())
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(
    state: AssistantUiState,
    viewModel: AssistantViewModel,
    onBack: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    hasMicPermission: Boolean,
) {
    val listState = rememberLazyListState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onMicPermissionResult(granted) }

    var inputFocused by remember { mutableStateOf(false) }

    val lastTimelineKey = state.timeline.lastOrNull()?.stableKey
    var autoScrollSettled by remember { mutableStateOf(false) }
    LaunchedEffect(lastTimelineKey) {
        autoScrollSettled = false
    }
    LaunchedEffect(lastTimelineKey, state.isProcessing) {
        if (state.timeline.isEmpty() && !state.isProcessing) {
            autoScrollSettled = true
            return@LaunchedEffect
        }
        listState.scrollChatToAbsoluteBottom(animate = true)
        autoScrollSettled = true
    }

    LaunchedEffect(inputFocused) {
        if (!inputFocused) return@LaunchedEffect
        listState.scrollChatToAbsoluteBottom(animate = false)
        kotlinx.coroutines.delay(80)
        listState.scrollChatToAbsoluteBottom(animate = false)
    }

    var previousTimelineSize by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.timeline.size) {
        val added = state.timeline.size - previousTimelineSize
        if (added > 0 && previousTimelineSize > 0 && listState.firstVisibleItemIndex <= 3) {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            withFrameMillis { }
            listState.scrollToItem(first + added, offset)
        }
        previousTimelineSize = state.timeline.size
    }

    LaunchedEffect(listState, autoScrollSettled, inputFocused) {
        if (!autoScrollSettled || inputFocused) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .filter { it == 0 }
            .collect { viewModel.loadOlderMessages() }
    }

    Scaffold(
        containerColor = StockPilotNavy,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
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
                            if (state.ttsMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (state.ttsMuted) "Unmute speech" else "Mute speech",
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                color = StockPilotNavy,
                tonalElevation = 4.dp,
                modifier = Modifier.navigationBarsPadding().imePadding(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (state.recording) {
                        RecordingIndicator()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (state.recording) Arrangement.Center else Arrangement.Start,
                    ) {
                        if (!state.recording) {
                            OutlinedTextField(
                                value = state.input,
                                onValueChange = viewModel::onInputChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(min = 120.dp)
                                    .onFocusChanged { inputFocused = it.isFocused },
                                placeholder = { Text("Ask anything about your inventory") },
                                minLines = 1,
                                maxLines = 5,
                            )
                        }
                        RecordingMicButton(
                            recording = state.recording,
                            onClick = {
                                viewModel.onMicPressed(hasMicPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                        )
                        if (!state.recording) {
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
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            state.ttsWarning?.let {
                Text(
                    it,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            state.error?.let {
                Text(
                    it,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.micPermissionDenied) {
                Text(
                    "Microphone denied — typed input still works.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            ChatMessageList(
                timeline = state.timeline,
                hasMoreHistory = state.hasMoreHistory,
                isLoadingOlder = state.isLoadingOlder,
                isProcessing = state.isProcessing,
                processingStartedAtMs = state.processingStartedAtMs,
                listState = listState,
                onOpenProduct = onOpenProduct,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

private const val CHAT_ITEM_DATE_HEADER = 0
private const val CHAT_ITEM_MESSAGE = 1

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatMessageList(
    timeline: List<ChatListItem>,
    hasMoreHistory: Boolean,
    isLoadingOlder: Boolean,
    isProcessing: Boolean,
    processingStartedAtMs: Long?,
    listState: LazyListState,
    onOpenProduct: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.imeNestedScroll(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (hasMoreHistory || isLoadingOlder) {
            item(key = "chat-history-loader", contentType = CHAT_ITEM_DATE_HEADER) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoadingOlder) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            "Scroll up for earlier messages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        items(
            items = timeline,
            key = { it.stableKey },
            contentType = { item ->
                when (item) {
                    is ChatListItem.DateHeader -> CHAT_ITEM_DATE_HEADER
                    is ChatListItem.MessageItem -> CHAT_ITEM_MESSAGE
                }
            },
        ) { item ->
            when (item) {
                is ChatListItem.DateHeader -> ChatDateHeader(item.label)
                is ChatListItem.MessageItem -> ChatBubble(item.message, onOpenProduct)
            }
        }
        if (isProcessing) {
            item(key = "chat-processing", contentType = CHAT_ITEM_DATE_HEADER) {
                ProcessingIndicator(processingStartedAtMs)
            }
        }
    }
}

@Composable
private fun ProcessingIndicator(processingStartedAtMs: Long?) {
    val elapsedMs = rememberProcessingElapsedMs(processingStartedAtMs)
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        val body = if (processingStartedAtMs != null) {
            "Working… ${ChatResponseTiming.formatDurationMs(elapsedMs)} elapsed (on-device Gemma)"
        } else {
            "Transcribing voice…"
        }
        Text(
            body,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun rememberProcessingElapsedMs(startedAtMs: Long?): Long {
    if (startedAtMs == null) return 0L
    var elapsed by remember(startedAtMs) { mutableLongStateOf(System.currentTimeMillis() - startedAtMs) }
    LaunchedEffect(startedAtMs) {
        while (true) {
            elapsed = System.currentTimeMillis() - startedAtMs
            delay(250)
        }
    }
    return elapsed
}

@Composable
private fun ChatDateHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = StockPilotSurfaceElevated,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, onOpenProduct: (Long) -> Unit) {
    val isUser = message.role == ChatRole.USER
    val bg = when (message.role) {
        ChatRole.USER -> StockPilotSurfaceElevated
        ChatRole.ASSISTANT -> StockPilotSurface
        ChatRole.SYSTEM -> Color.Transparent
    }
  val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val hasProductRows = message.role == ChatRole.ASSISTANT && message.referencedProducts.isNotEmpty()
    val bubbleModifier = when {
        message.role == ChatRole.SYSTEM -> Modifier.fillMaxWidth()
        hasProductRows -> Modifier.fillMaxWidth()
        else -> Modifier.widthIn(max = 340.dp)
    }
  Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = alignment,
  ) {
      Column(modifier = bubbleModifier) {
          if (message.role == ChatRole.ASSISTANT) {
              ChatResponseTiming.labelForCompleted(message.responseDurationMs)?.let { timingLabel ->
                  Text(
                      text = timingLabel,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.padding(bottom = 2.dp),
                  )
              }
              message.provenance?.let { prov ->
                  val color = when (prov.mode) {
                      AssistantDeliveryMode.ON_DEVICE_AGENT -> StockPilotTeal
                      AssistantDeliveryMode.TOOL_FACTS_ONLY -> StockPilotWarning
                      AssistantDeliveryMode.AGENT_UNAVAILABLE -> StockPilotError
                  }
                  Text(
                      text = prov.label(),
                      style = MaterialTheme.typography.labelSmall,
                      color = color,
                      modifier = Modifier.padding(bottom = 4.dp),
                  )
              }
          }
      val contentModifier = Modifier
          .background(bg, RoundedCornerShape(18.dp))
          .padding(horizontal = 14.dp, vertical = 10.dp)
      Column(modifier = contentModifier) {
          if (message.role == ChatRole.ASSISTANT && message.productLinks.isNotEmpty()) {
              LinkedChatText(
                  text = message.text,
                  links = message.productLinks,
                  onProductClick = onOpenProduct,
              )
          } else {
              Text(
                  text = message.text,
                  style = if (message.role == ChatRole.SYSTEM) {
                      MaterialTheme.typography.bodyMedium
                  } else {
                      MaterialTheme.typography.bodyLarge
                  },
                  color = if (message.role == ChatRole.SYSTEM) {
                      MaterialTheme.colorScheme.onSurfaceVariant
                  } else {
                      MaterialTheme.colorScheme.onSurface
                  },
              )
          }
          MessageTimestamp(
              timestampMs = message.timestampMs,
              alignEnd = isUser,
          )
      }
          if (hasProductRows) {
              ReferencedProductsList(
                  products = message.referencedProducts,
                  onOpenProduct = onOpenProduct,
              )
          }
      }
  }
}

@Composable
private fun MessageTimestamp(timestampMs: Long, alignEnd: Boolean) {
    val timeLabel = remember(timestampMs) { ChatDateTimeFormat.messageTime(timestampMs) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun RecordingMicButton(
    recording: Boolean,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "mic-rec")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mic-icon-alpha",
    )
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mic-icon-scale",
    )
    val ringScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mic-ring-scale",
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mic-ring-alpha",
    )
    val idleTint = StockPilotTeal
    val recTint = MaterialTheme.colorScheme.error
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp),
        ) {
            if (recording) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .scale(ringScale)
                        .alpha(ringAlpha)
                        .background(recTint, CircleShape),
                )
            }
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = if (recording) "Stop recording" else "Record voice",
                tint = if (recording) {
                    recTint.copy(alpha = pulseAlpha)
                } else {
                    idleTint
                },
                modifier = if (recording) {
                    Modifier.scale(pulseScale)
                } else {
                    Modifier
                },
            )
        }
    }
}

/** Puts the last row's bottom at the viewport bottom (not just the item's top edge). */
private suspend fun LazyListState.scrollChatToAbsoluteBottom(animate: Boolean = false) {
    repeat(2) { withFrameMillis { } }
    val lastIndex = layoutInfo.totalItemsCount - 1
    if (lastIndex < 0) return
    val tailOffset = Int.MAX_VALUE / 4
    if (animate) {
        animateScrollToItem(lastIndex, scrollOffset = tailOffset)
    } else {
        scrollToItem(lastIndex, scrollOffset = tailOffset)
    }
}

@Composable
private fun RecordingIndicator() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Recording… stops after ~2s of silence (tap mic to stop now)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}