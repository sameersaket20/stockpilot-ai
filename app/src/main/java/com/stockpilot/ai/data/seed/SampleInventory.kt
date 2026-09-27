package com.stockpilot.ai.data.seed

import com.stockpilot.ai.data.local.ProductEntity

object SampleInventory {
    fun products(now: Long = System.currentTimeMillis()): List<ProductEntity> = listOf(
        product("SKU-BT-001", "Blue T-shirt", "Apparel", 18, 10, 40, 4, now),
        product("SKU-BT-002", "Black T-shirt", "Apparel", 32, 12, 45, 5, now),
        product("SKU-BT-003", "White T-shirt", "Apparel", 0, 10, 40, 6, now),
        product("SKU-JN-101", "Slim Jeans — Dark Wash", "Apparel", 14, 8, 30, 3, now),
        product("SKU-JN-102", "Relaxed Jeans — Light Wash", "Apparel", 9, 10, 35, 2, now),
        product("SKU-HD-201", "Cotton Hoodie — Gray", "Apparel", 22, 10, 35, 3, now),
        product("SKU-SK-301", "Running Sneakers — 42", "Footwear", 6, 8, 25, 2, now),
        product("SKU-SK-302", "Canvas Sneakers — 41", "Footwear", 11, 10, 30, 2, now),
        product("SKU-BG-401", "Leather Messenger Bag", "Accessories", 4, 5, 15, 1, now),
        product("SKU-BG-402", "Nylon Backpack — 20L", "Accessories", 17, 8, 25, 2, now),
        product("SKU-HT-501", "Wool Beanie", "Accessories", 28, 10, 35, 4, now),
        product("SKU-GL-601", "Polarized Sunglasses", "Accessories", 3, 5, 12, 1, now),
        product("SKU-WT-701", "Stainless Water Bottle — 750ml", "Home", 25, 10, 40, 4, now),
        product("SKU-MG-801", "Ceramic Mug — 350ml", "Home", 0, 8, 30, 3, now),
        product("SKU-TW-901", "Cotton Bath Towel — Navy", "Home", 19, 10, 35, 3, now),
        product("SKU-BK-1001", "Hardcover Notebook — A5", "Stationery", 45, 15, 60, 5, now),
        product("SKU-PN-1002", "Gel Pen — Blue (12pk)", "Stationery", 7, 10, 40, 3, now),
        product("SKU-PN-1003", "Mechanical Pencil — 0.7mm", "Stationery", 12, 10, 35, 2, now),
        product("SKU-PH-1101", "USB-C Charging Cable — 1m", "Electronics", 31, 12, 50, 6, now),
        product("SKU-PH-1102", "Wireless Earbuds — Basic", "Electronics", 5, 6, 20, 2, now),
        product("SKU-PH-1103", "Portable Power Bank — 10k", "Electronics", 8, 8, 25, 2, now),
        product("SKU-FD-1201", "Whole Bean Coffee — 500g", "Grocery", 16, 10, 35, 4, now),
        product("SKU-FD-1202", "Green Tea — 50 bags", "Grocery", 23, 12, 40, 3, now),
        product("SKU-FD-1203", "Granola — 400g", "Grocery", 2, 8, 25, 2, now),
        product("SKU-CL-1301", "Liquid Dish Soap — 500ml", "Household", 27, 10, 40, 4, now),
        product("SKU-CL-1302", "Laundry Pods — 24ct", "Household", 6, 8, 30, 3, now),
        product("SKU-SP-1401", "Yoga Mat — 5mm", "Sports", 13, 8, 25, 2, now),
        product("SKU-SP-1402", "Resistance Bands — Set", "Sports", 9, 6, 20, 2, now),
        product("SKU-PF-1501", "Dog Treats — 200g", "Pets", 11, 8, 30, 3, now),
        product("SKU-PF-1502", "Cat Litter — 5kg", "Pets", 4, 6, 20, 2, now),
    )

    private fun product(
        sku: String,
        name: String,
        category: String,
        stock: Int,
        reorderThreshold: Int,
        targetStock: Int,
        dailySales: Int,
        updatedAt: Long,
    ) = ProductEntity(
        sku = sku,
        name = name,
        category = category,
        stockQuantity = stock,
        reorderThreshold = reorderThreshold,
        targetStock = targetStock,
        dailySalesEstimate = dailySales,
        updatedAt = updatedAt,
    )
}