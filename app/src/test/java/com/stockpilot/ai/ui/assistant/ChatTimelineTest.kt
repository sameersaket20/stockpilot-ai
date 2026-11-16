package com.stockpilot.ai.ui.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ChatTimelineTest {
    @Test
    fun sectionLabel_todayAndYesterday() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val todayMs = today.atStartOfDay(zone).toInstant().toEpochMilli() + 60_000
        val yesterdayMs = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals("Today", ChatDateTimeFormat.sectionLabel(todayMs, today))
        assertEquals("Yesterday", ChatDateTimeFormat.sectionLabel(yesterdayMs, today))
    }

    @Test
    fun buildChatTimeline_insertsDateHeaderWhenDayChanges() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        val m1 = ChatMessage(
            id = 1,
            role = ChatRole.USER,
            text = "a",
            timestampMs = yesterday.atTime(10, 0).atZone(zone).toInstant().toEpochMilli(),
        )
        val m2 = ChatMessage(
            id = 2,
            role = ChatRole.ASSISTANT,
            text = "b",
            timestampMs = today.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
        )
        val timeline = buildChatTimeline(listOf(m1, m2))
        assertEquals(4, timeline.size)
        assertTrue(timeline[0] is ChatListItem.DateHeader)
        assertTrue(timeline[2] is ChatListItem.DateHeader)
    }
}