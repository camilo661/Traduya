package com.traduya.core.audio

import com.traduya.core.domain.audio.AudioCapture
import com.traduya.core.domain.audio.CallRecordingStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AudioModule {

    @Binds
    abstract fun bindAudioCapture(impl: AndroidAudioCapture): AudioCapture

    @Binds
    abstract fun bindCallRecordingStore(impl: WavRecordingStore): CallRecordingStore
}
