package com.stockpilot.ai.domain.agent

import org.json.JSONArray
import org.json.JSONObject

data class AgentToolCall(val tool: String, val arguments: JSONObject)

data class AgentPlan(
    val toolCalls: List<AgentToolCall>,
)

object AgentPlanParser {
    fun parse(raw: String): AgentPlan {
        val jsonText = extractJsonObject(raw) ?: return AgentPlan(emptyList())
        return try {
            val root = JSONObject(jsonText)
            val calls = root.optJSONArray("tool_calls") ?: JSONArray()
            val list = buildList {
                for (i in 0 until calls.length()) {
                    val item = calls.optJSONObject(i) ?: continue
                    val tool = item.optString("tool", item.optString("name", ""))
                    if (tool.isBlank()) continue
                    val args = item.optJSONObject("arguments")
                        ?: item.optJSONObject("args")
                        ?: JSONObject()
                    add(AgentToolCall(tool, args))
                }
            }
            AgentPlan(list)
        } catch (_: Exception) {
            AgentPlan(emptyList())
        }
    }

    fun extractJsonObject(raw: String): String? {
        val trimmed = raw.trim()
        val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE).find(trimmed)
        if (fenced != null) return fenced.groupValues[1].trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        return null
    }
}