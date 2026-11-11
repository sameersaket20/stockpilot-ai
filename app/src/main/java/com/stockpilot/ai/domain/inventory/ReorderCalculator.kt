package com.stockpilot.ai.domain.inventory

object ReorderCalculator {
    /**
     * Target stock is the desired on-hand quantity after expected sales are accounted for.
     * reorder = max(0, targetStock - (currentStock - expectedSales))
     */
    fun calculate(currentStock: Int, expectedSales: Int, targetStock: Int): Int {
        require(currentStock >= 0 && expectedSales >= 0 && targetStock >= 0)
        val projected = currentStock - expectedSales
        val raw = targetStock - projected
        return maxOf(0, raw)
    }
}