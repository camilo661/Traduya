package com.traduya.core.callcapture

import com.traduya.core.domain.audio.CallAudioTap
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CallCaptureModule {

    @Binds
    abstract fun bindCallAudioTap(impl: ShizukuCallAudioTap): CallAudioTap
}
