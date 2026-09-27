package com.stockpilot.ai

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stockpilot.ai.data.local.ProductEntity
import com.stockpilot.ai.data.local.StockPilotDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryDaoInstrumentedTest {
    private lateinit var database: StockPilotDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, StockPilotDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryProduct() = runBlocking {
        val dao = database.productDao()
        dao.insertAll(
            listOf(
                ProductEntity(
                    sku = "SKU-BT-001",
                    name = "Blue T-shirt",
                    category = "Apparel",
                    stockQuantity = 24,
                    reorderThreshold = 10,
                    targetStock = 40,
                    dailySalesEstimate = 4,
                ),
            ),
        )
        val total = dao.observeTotalUnits().first()
        assertEquals(24, total)
        val found = dao.searchByNameToken("blue")
        assertEquals(1, found.size)
    }
}