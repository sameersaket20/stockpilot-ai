package com.stockpilot.ai.inference

import com.stockpilot.ai.domain.assistant.ConversationContext
import com.stockpilot.ai.domain.assistant.DemoIntentParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 1 placeholder — not on-device Gemma inference. Demo mode only.
 */
@Singleton
class DemoInferenceEngine @Inject constructor() : InferenceEngine {
    private val _status = MutableStateFlow(ModelStatus.NotInstalled)
    override val status: StateFlow<ModelStatus> = _status

    override suspend fun interpret(request: String, context: ConversationContext): IntentResult {
        val command = DemoIntentParser.parse(request, context)
        return IntentResult(command)
    }

    override suspend fun close() = Unit
}