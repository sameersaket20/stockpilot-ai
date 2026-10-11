package com.stockpilot.ai.domain.inventory

import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderCalculatorTest {
    @Test
    fun reorder_exampleFromSpec() {
        assertEquals(16, ReorderCalculator.calculate(24, 10, 30))
    }

    @Test
    fun reorder_neverNegative() {
        assertEquals(0, ReorderCalculator.calculate(100, 0, 50))
    }

    @Test
    fun reorder_exactTarget() {
        assertEquals(5, ReorderCalculator.calculate(10, 5, 10))
    }
}