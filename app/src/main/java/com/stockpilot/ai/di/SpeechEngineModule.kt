package com.stockpilot.ai.di

import com.stockpilot.ai.speech.SpeechEngine
import com.stockpilot.ai.speech.RoutingSpeechEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SpeechEngineModule {

    @Binds
    @Singleton
    abstract fun bindSpeechEngine(impl: RoutingSpeechEngine): SpeechEngine
}