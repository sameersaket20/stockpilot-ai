package com.stockpilot.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM products
        WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(sku) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(category) LIKE '%' || LOWER(:query) || '%'
        ORDER BY name ASC
        """,
    )
    fun observeSearch(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE LOWER(sku) = LOWER(:sku) LIMIT 1")
    suspend fun getBySku(sku: String): ProductEntity?

    @Query("SELECT * FROM products")
    suspend fun getAllOnce(): List<ProductEntity>

    @Query(
        """
        SELECT * FROM products
        WHERE LOWER(name) LIKE '%' || LOWER(:token) || '%'
        ORDER BY name ASC
        """,
    )
    suspend fun searchByNameToken(token: String): List<ProductEntity>

    @Query(
        """
        SELECT COALESCE(SUM(stockQuantity), 0) FROM products
        """,
    )
    fun observeTotalUnits(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products")
    fun observeProductCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM products
        WHERE stockQuantity > 0 AND stockQuantity <= reorderThreshold
        """,
    )
    fun observeLowStockCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    @Transaction
    suspend fun replaceAll(products: List<ProductEntity>) {
        deleteAll()
        insertAll(products)
    }

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}