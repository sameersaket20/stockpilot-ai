package com.stockpilot.ai.domain.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class AgentPlanParserTest {
    @Test
    fun parsesToolCallsArray() {
        val plan = AgentPlanParser.parse(
            """{"tool_calls":[{"tool":"inventory_metrics","arguments":{}},""" +
                """{"tool":"get_product","args":{"sku_or_name":"Blue T-shirt"}}]}""",
        )
        assertEquals(2, plan.toolCalls.size)
        assertEquals("inventory_metrics", plan.toolCalls[0].tool)
        assertEquals("get_product", plan.toolCalls[1].tool)
    }
}