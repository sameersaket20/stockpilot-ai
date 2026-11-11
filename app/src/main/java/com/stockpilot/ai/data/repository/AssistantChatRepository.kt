package com.stockpilot.ai.data.repository

import com.stockpilot.ai.data.local.AssistantMessageDao
import com.stockpilot.ai.data.local.AssistantMessageEntity
import com.stockpilot.ai.domain.assistant.AssistantDeliveryMode
import com.stockpilot.ai.domain.assistant.AssistantProvenance
import com.stockpilot.ai.domain.assistant.ChatProductLink
import com.stockpilot.ai.domain.assistant.ChatProductRef
import com.stockpilot.ai.ui.assistant.ChatMessage
import com.stockpilot.ai.ui.assistant.ChatRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantChatRepository @Inject constructor(
    private val dao: AssistantMessageDao,
) {
    fun observeMessages(): Flow<List<ChatMessage>> =
        dao.observeAll().map { list -> list.map(::toUi) }

    fun observeLastN(limit: Int): Flow<List<ChatMessage>> =
        dao.observeLastN(limit).map { list -> list.map(::toUi) }

    fun observeMessageCount(): Flow<Int> = dao.observeCount().distinctUntilChanged()

    suspend fun ensureWelcomeIfEmpty() {
        if (dao.count() > 0) return
        insert(
            ChatMessage(
                role = ChatRole.SYSTEM,
                text = "Answers are labeled: on-device AI (Gemma), data-only, or AI unavailable — never a hidden fallback. " +
                    "Import Gemma and wait for Ready in Model Manager.",
            ),
        )
    }

    suspend fun insert(message: ChatMessage): Long {
        val entity = AssistantMessageEntity(
            id = if (message.id != 0L) message.id else 0,
            role = message.role.name,
            text = message.text,
            productLinksJson = encodeLinks(message.productLinks),
            referencedProductsJson = encodeRefs(message.referencedProducts),
            provenanceJson = message.provenance?.let(::encodeProvenance),
            createdAtMs = message.timestampMs,
            responseDurationMs = message.responseDurationMs,
        )
        return dao.insert(entity)
    }

    private fun toUi(entity: AssistantMessageEntity): ChatMessage {
        val role = ChatRole.valueOf(entity.role)
        val isAssistant = role == ChatRole.ASSISTANT
        return ChatMessage(
            id = entity.id,
            role = role,
            text = entity.text,
            productLinks = if (isAssistant) decodeLinks(entity.productLinksJson) else emptyList(),
            referencedProducts = if (isAssistant) decodeRefs(entity.referencedProductsJson) else emptyList(),
            provenance = if (isAssistant) entity.provenanceJson?.let(::decodeProvenance) else null,
            timestampMs = entity.createdAtMs,
            responseDurationMs = if (isAssistant) entity.responseDurationMs else null,
        )
    }

    private fun encodeLinks(links: List<ChatProductLink>): String {
        val arr = JSONArray()
        links.forEach { link ->
            arr.put(
                JSONObject()
                    .put("productId", link.productId)
                    .put("startIndex", link.startIndex)
                    .put("endIndex", link.endIndex),
            )
        }
        return arr.toString()
    }

    private fun decodeLinks(json: String): List<ChatProductLink> {
        val arr = JSONArray(json)
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    ChatProductLink(
                        productId = o.getLong("productId"),
                        startIndex = o.getInt("startIndex"),
                        endIndex = o.getInt("endIndex"),
                    ),
                )
            }
        }
    }

    private fun encodeRefs(refs: List<ChatProductRef>): String {
        val arr = JSONArray()
        refs.forEach { ref ->
            arr.put(
                JSONObject()
                    .put("productId", ref.productId)
                    .put("sku", ref.sku)
                    .put("name", ref.name),
            )
        }
        return arr.toString()
    }

    private fun decodeRefs(json: String): List<ChatProductRef> {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    ChatProductRef(
                        productId = o.getLong("productId"),
                        sku = o.optString("sku"),
                        name = o.optString("name"),
                    ),
                )
            }
        }
    }

    private fun encodeProvenance(provenance: AssistantProvenance): String = JSONObject()
        .put("mode", provenance.mode.name)
        .put("detail", provenance.detail)
        .put("toolsUsed", JSONArray(provenance.toolsUsed))
        .put("safetyApproved", provenance.safetyApproved)
        .toString()

    private fun decodeProvenance(json: String): AssistantProvenance {
        val o = JSONObject(json)
        val tools = buildList {
            val arr = o.optJSONArray("toolsUsed") ?: JSONArray()
            for (i in 0 until arr.length()) add(arr.getString(i))
        }
        return AssistantProvenance(
            mode = AssistantDeliveryMode.valueOf(o.getString("mode")),
            detail = o.optString("detail"),
            toolsUsed = tools,
            safetyApproved = if (o.has("safetyApproved") && !o.isNull("safetyApproved")) {
                o.getBoolean("safetyApproved")
            } else {
                null
            },
        )
    }
}