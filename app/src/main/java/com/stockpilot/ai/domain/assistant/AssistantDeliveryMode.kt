package com.stockpilot.ai.domain.assistant

/**
 * How the assistant produced a visible answer. Shown in UI so reviewers know whether on-device AI ran.
 */
enum class AssistantDeliveryMode {
    /** Planner → read-only tools → narrator → guardrails → safety review (all completed). */
    ON_DEVICE_AGENT,

    /** Tools ran on device; final wording is structured facts (narration or review did not ship). */
    TOOL_FACTS_ONLY,

    /** Gemma did not complete an agent turn — no answer pretending to be AI. */
    AGENT_UNAVAILABLE,
}

data class AssistantProvenance(
    val mode: AssistantDeliveryMode,
    val detail: String,
    val toolsUsed: List<String> = emptyList(),
    val safetyApproved: Boolean? = null,
) {
    fun label(): String = when (mode) {
        AssistantDeliveryMode.ON_DEVICE_AGENT ->
            "On-device AI (Gemma): loaded data, reviewed"
        AssistantDeliveryMode.TOOL_FACTS_ONLY ->
            "On-device data only: ${detail.ifBlank { "AI narration or review blocked" }}"
        AssistantDeliveryMode.AGENT_UNAVAILABLE ->
            "AI unavailable: ${detail.ifBlank { "Gemma not ready" }}"
    }
}