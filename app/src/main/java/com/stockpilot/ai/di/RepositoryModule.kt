package com.stockpilot.ai.di

import com.stockpilot.ai.data.repository.InventoryOperations
import com.stockpilot.ai.data.repository.InventoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindInventoryOperations(impl: InventoryRepository): InventoryOperations
}