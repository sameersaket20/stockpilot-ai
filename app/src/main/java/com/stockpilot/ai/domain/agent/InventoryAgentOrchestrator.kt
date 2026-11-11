package com.stockpilot.ai.domain.agent

import com.stockpilot.ai.data.local.ProductEntity
import com.stockpilot.ai.domain.assistant.AssistantDeliveryMode
import com.stockpilot.ai.domain.assistant.AssistantProvenance
import com.stockpilot.ai.domain.assistant.AssistantReply
import com.stockpilot.ai.domain.assistant.AssistantReplyLinks
import com.stockpilot.ai.domain.assistant.ConversationContext
import com.stockpilot.ai.inference.GemmaInferenceEngine
import com.stockpilot.ai.inference.ModelStatus
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryAgentOrchestrator @Inject constructor(
    private val gemma: GemmaInferenceEngine,
    private val tools: InventoryToolRunner,
    private val safetyReview: AgentSafetyReview,
) {
    private val plannerInstruction = """
You are the StockPilot inventory query planner. Output ONLY one JSON object — no markdown prose.
Format: {"tool_calls":[{"tool":"...","arguments":{...}}]}
Plan read-only tool_calls using the schema below. Prefer small queries over full_inventory_read.
""".trimIndent() + "\n\n" + InventorySchemaForModel.plannerCatalog()

    private val narratorInstruction = """
You are StockPilot inventory assistant. Write a concise, natural reply for store staff.
FACTS_JSON contains results of the planned tool_calls (inventory_summary, query_products, etc.).
Before answering, read field_glossary and product_line_header inside FACTS_JSON.
Stock semantics (match the app UI):
- Quantity on hand = on_hand_qty (3rd pipe column) = units in the store right now.
- Target stock = target_shelf_qty = planning target only, NOT current quantity.
- availability OK/LOW/OUT = warning label; OUT means on_hand_qty is 0 — do not rank or compare quantities using this column.
- Out of stock / zero quantity = on_hand_qty is 0 only — use zero_on_hand_* in facts; LOW with qty > 0 is NOT out of stock.
Rules:
- Use ONLY numbers from FACTS_JSON. "In stock" / how much / highest / most → on_hand_qty, total_units, or highest_quantity_*.
- Out of stock / zero / 0 quantity → zero_on_hand_count and zero_on_hand_product_lines only — never cite LOW items.
- Never treat target_shelf_qty or availability as "how many we have".
- Do not claim database changes.
- Reorder suggestions → suggested_reorder_qty on product_lines unless compute_reorder overrides exist.
- Use product names when possible; matching SKUs/names become tappable rows under the message.
- Be brief (bullet list ok).
""".trimIndent()

    suspend fun respond(userMessage: String, context: ConversationContext): Pair<AssistantReply, ConversationContext> {
        if (!gemma.isReady()) {
            return unavailable(context, gemmaNotReadyDetail())
        }

        val planPrompt = buildString {
            appendLine("User request:")
            appendLine(userMessage.trim())
            appendLine()
            appendLine("Return only JSON {\"tool_calls\":[...]}.")
        }
        val planRaw = gemma.complete(plannerInstruction, planPrompt).getOrNull()
        val parsed = AgentPlanParser.parse(planRaw ?: "")
        val toolCalls = parsed.toolCalls.ifEmpty {
            listOf(
                AgentToolCall("inventory_summary", JSONObject()),
                AgentToolCall(
                    "query_products",
                    JSONObject().put("limit", 25),
                ),
            )
        }
        return runToolPipeline(userMessage, context, AgentPlan(toolCalls))
    }

    private suspend fun runToolPipeline(
        userMessage: String,
        context: ConversationContext,
        plan: AgentPlan,
    ): Pair<AssistantReply, ConversationContext> {
        val aggregatedProducts = mutableListOf<ProductEntity>()
        val toolResults = JSONArray()
        val toolNames = mutableListOf<String>()

        for (call in plan.toolCalls) {
            if (toolNames.size >= 6) break
            toolNames += call.tool
            val result = tools.invoke(call.tool, call.arguments)
            val entry = JSONObject()
                .put("tool", call.tool)
                .put("ok", result.ok)
            if (result.error != null) entry.put("error", result.error)
            if (result.ok) entry.put("facts", result.facts)
            toolResults.put(entry)
            aggregatedProducts += result.products
        }

        val factsJsonForModel = InventoryFactsCompressor.factsBundleForModel(toolResults, narrationProductFilter = null)
        val narratePrompt = buildString {
            appendLine(
                "Inventory data below. Column meanings are in field_glossary; " +
                    "each product row starts with product_line_header.",
            )
            appendLine("FACTS_JSON:")
            appendLine(factsJsonForModel)
            appendLine()
            appendLine("User request: ${userMessage.trim()}")
            appendLine("Keep the answer under 12 lines unless the user asked for everything.")
        }
        val narrateResult = gemma.complete(narratorInstruction, narratePrompt)
        val draft = narrateResult.getOrNull()?.trim()

        if (draft.isNullOrBlank()) {
            val reason = narrateResult.exceptionOrNull()?.message ?: "Gemma narration failed"
            val fullRead = tools.readEntireInventory()
            InventoryLocalResponder.lowStockFallback(userMessage, fullRead, context, reason)
                ?.let { return it }
            return toolFactsOnly(
                context,
                aggregatedProducts,
                toolResults,
                toolNames,
                detail = "Gemma narration failed",
                safetyApproved = null,
            )
        }

        val guardrail = ResponseGuardrails.validate(draft, factsJsonForModel)
        if (!guardrail.allowed) {
            return toolFactsOnly(
                context,
                aggregatedProducts,
                toolResults,
                toolNames,
                detail = guardrail.issues.joinToString("; "),
                safetyApproved = null,
            )
        }

        val safety = safetyReview.reviewReadOnly(userMessage, draft, guardrail.allowed)
        if (!safety.approved) {
            return toolFactsOnly(
                context,
                aggregatedProducts,
                toolResults,
                toolNames,
                detail = safety.reason ?: "Failed safety review",
                safetyApproved = false,
            )
        }

        val catalogForLinks = aggregatedProducts.ifEmpty {
            tools.readEntireInventory().products
        }
        val cited = AssistantReplyLinks.productsMentionedIn(draft, catalogForLinks)
        val linkProducts = if (cited.isNotEmpty()) cited else aggregatedProducts.distinctBy { it.id }
        val newContext = cited.lastOrNull()?.let { context.withProduct(it) }
            ?: aggregatedProducts.lastOrNull()?.let { context.withProduct(it) }
            ?: context
        val links = AssistantReplyLinks.spansForProducts(draft, linkProducts)
        return AssistantReply(
            text = draft,
            productLinks = links,
            referencedProducts = AssistantReplyLinks.toRefs(cited),
            provenance = AssistantProvenance(
                mode = AssistantDeliveryMode.ON_DEVICE_AGENT,
                detail = "Plan→query→answer. Tools: ${toolNames.joinToString()}",
                toolsUsed = toolNames,
                safetyApproved = true,
            ),
        ) to newContext
    }

    private fun unavailable(
        context: ConversationContext,
        detail: String,
    ): Pair<AssistantReply, ConversationContext> {
        val text = buildString {
            appendLine(detail)
            appendLine()
            append("Open Model Manager and confirm Gemma shows Ready before using the assistant.")
        }.trim()
        return AssistantReply(
            text = text,
            provenance = AssistantProvenance(
                mode = AssistantDeliveryMode.AGENT_UNAVAILABLE,
                detail = detail,
            ),
        ) to context
    }

    private fun gemmaNotReadyDetail(): String = when (gemma.status.value) {
        ModelStatus.Loading -> "Gemma is still loading on device (Model Manager)."
        ModelStatus.Error ->
            "Gemma failed to load: ${gemma.lastError.value ?: "see Model Manager"}."
        ModelStatus.NotInstalled -> "Gemma model not imported — import in Model Manager."
        ModelStatus.ImportedUnverified -> "Gemma imported but not initialized yet — wait or reopen the app."
        ModelStatus.Ready -> "Gemma not available for this turn."
    }

    private fun toolFactsOnly(
        context: ConversationContext,
        products: List<ProductEntity>,
        toolResults: JSONArray,
        toolNames: List<String>,
        detail: String,
        safetyApproved: Boolean?,
    ): Pair<AssistantReply, ConversationContext> {
        val summary = buildString {
            appendLine("Structured inventory data (read-only):")
            for (i in 0 until toolResults.length()) {
                val row = toolResults.optJSONObject(i) ?: continue
                appendLine("• ${row.optString("tool")}: ${row.optJSONObject("facts") ?: row.optString("error")}")
            }
        }.trim()
        val cited = AssistantReplyLinks.productsMentionedIn(summary, products)
        val newContext = cited.lastOrNull()?.let { context.withProduct(it) } ?: context
        val links = AssistantReplyLinks.spansForProducts(summary, cited.ifEmpty { products.distinctBy { it.id } })
        return AssistantReply(
            text = summary,
            productLinks = links,
            referencedProducts = AssistantReplyLinks.toRefs(cited),
            provenance = AssistantProvenance(
                mode = AssistantDeliveryMode.TOOL_FACTS_ONLY,
                detail = detail,
                toolsUsed = toolNames,
                safetyApproved = safetyApproved,
            ),
        ) to newContext
    }
}