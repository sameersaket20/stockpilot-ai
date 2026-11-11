package com.stockpilot.ai.domain.agent

data class GuardrailVerdict(
    val allowed: Boolean,
    val issues: List<String> = emptyList(),
)

/** Deterministic checks before the safety model reviews the draft answer. */
object ResponseGuardrails {
    private val forbiddenDataMutation = Regex(
        pattern = """\b(delete|drop|truncate|insert\s+into|update\s+set|alter\s+table|remove\s+from\s+database)\b""",
        option = RegexOption.IGNORE_CASE,
    )

    private val forbiddenSql = Regex(
        pattern = """\b(exec(ute)?\s+sql|run\s+query\s*:)\b""",
        option = RegexOption.IGNORE_CASE,
    )

    fun validate(draftAnswer: String, factsBundle: String): GuardrailVerdict {
        val issues = mutableListOf<String>()
        if (draftAnswer.isBlank()) {
            issues += "Empty assistant answer"
        }
        if (forbiddenDataMutation.containsMatchIn(draftAnswer)) {
            issues += "Answer suggests database mutation"
        }
        if (forbiddenSql.containsMatchIn(draftAnswer)) {
            issues += "Answer suggests arbitrary SQL"
        }
        if (draftAnswer.length > 8_000) {
            issues += "Answer too long"
        }
        return GuardrailVerdict(allowed = issues.isEmpty(), issues = issues)
    }
}