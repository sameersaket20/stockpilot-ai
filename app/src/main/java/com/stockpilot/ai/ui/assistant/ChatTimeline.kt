package com.stockpilot.ai.ui.assistant

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class ChatListItem {
    abstract val stableKey: String

    data class DateHeader(
        val label: String,
        val dayEpoch: Long,
    ) : ChatListItem() {
        override val stableKey: String = "date-$dayEpoch"
    }

    data class MessageItem(
        val message: ChatMessage,
    ) : ChatListItem() {
        override val stableKey: String = "msg-${message.id}"
    }
}

object ChatDateTimeFormat {
    private val zone: ZoneId
        get() = ZoneId.systemDefault()

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    private val sameYearFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

    private val otherYearFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

    fun messageTime(timestampMs: Long): String =
        Instant.ofEpochMilli(timestampMs).atZone(zone).format(timeFormatter)

    fun sectionLabel(timestampMs: Long, today: LocalDate = LocalDate.now(zone)): String {
        val date = Instant.ofEpochMilli(timestampMs).atZone(zone).toLocalDate()
        val yesterday = today.minusDays(1)
        return when (date) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> if (date.year == today.year) {
                date.format(sameYearFormatter)
            } else {
                date.format(otherYearFormatter)
            }
        }
    }

    fun dayEpoch(timestampMs: Long): Long =
        Instant.ofEpochMilli(timestampMs).atZone(zone).toLocalDate().toEpochDay()
}

fun buildChatTimeline(messages: List<ChatMessage>): List<ChatListItem> {
    if (messages.isEmpty()) return emptyList()
    val items = mutableListOf<ChatListItem>()
    var lastDay: Long? = null
    for (message in messages) {
        val day = ChatDateTimeFormat.dayEpoch(message.timestampMs)
        if (lastDay != day) {
            items += ChatListItem.DateHeader(
                label = ChatDateTimeFormat.sectionLabel(message.timestampMs),
                dayEpoch = day,
            )
            lastDay = day
        }
        items += ChatListItem.MessageItem(message)
    }
    return items
}