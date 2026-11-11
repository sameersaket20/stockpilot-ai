package com.stockpilot.ai.di

import android.content.Context
import com.stockpilot.ai.data.local.AssistantMessageDao
import com.stockpilot.ai.data.local.ProductDao
import com.stockpilot.ai.data.local.StockPilotDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StockPilotDatabase =
        StockPilotDatabase.build(context)

    @Provides
    fun provideProductDao(database: StockPilotDatabase): ProductDao =
        database.productDao()

    @Provides
    fun provideAssistantMessageDao(database: StockPilotDatabase): AssistantMessageDao =
        database.assistantMessageDao()
}