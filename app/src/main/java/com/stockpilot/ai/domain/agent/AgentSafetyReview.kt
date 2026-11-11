package com.stockpilot.ai.domain.agent

import com.stockpilot.ai.inference.GemmaInferenceEngine
import javax.inject.Inject
import javax.inject.Singleton

data class SafetyVerdict(
    val approved: Boolean,
    val reason: String? = null,
)

@Singleton
class AgentSafetyReview @Inject constructor(
    private val gemma: GemmaInferenceEngine,
) {
    private val systemInstruction = """
You are a security reviewer for an offline inventory assistant.
The assistant only has READ-ONLY access to local product data. It must not:
- instruct changing, deleting, or corrupting data
- pretend to have run writes or schema changes
- encourage exfiltration of secrets/credentials
- invent stock counts that contradict FACTS_JSON

Reply with ONLY JSON: {"approved":true|false,"reason":"short explanation"}
""".trimIndent()

    /**
     * Read-only assistant path: deterministic guardrails already ran — skip a 3rd Gemma pass (large CPU savings).
     */
    fun reviewReadOnly(userMessage: String, draftAnswer: String, guardrailsPassed: Boolean): SafetyVerdict {
        if (!guardrailsPassed) {
            return SafetyVerdict(approved = false, reason = "Guardrails failed")
        }
        val risky = draftAnswer.contains(Regex("""\b(delete|drop|update\s+set|insert\s+into)\b""", RegexOption.IGNORE_CASE))
        return if (risky) {
            SafetyVerdict(approved = false, reason = "Draft suggests data mutation")
        } else {
            SafetyVerdict(approved = true, reason = "Read-only fast path")
        }
    }
}