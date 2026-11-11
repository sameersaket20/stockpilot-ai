package com.stockpilot.ai.domain.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseGuardrailsTest {
    @Test
    fun blocksDeleteInstructions() {
        val verdict = ResponseGuardrails.validate("Please DELETE FROM products", "{}")
        assertFalse(verdict.allowed)
    }

    @Test
    fun allowsNormalStockReply() {
        val verdict = ResponseGuardrails.validate("Blue T-shirt has 18 units on hand.", """{"stock_quantity":18}""")
        assertTrue(verdict.allowed)
    }
}