package com.traduya

import android.app.Application
import com.traduya.session.CallSessionCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TraduyaApp : Application() {

    @Inject lateinit var callSessionCoordinator: CallSessionCoordinator

    override fun onCreate() {
        super.onCreate()
        // Empieza a vigilar las llamadas para traducirlas y guardarlas en el historial
        callSessionCoordinator.start()
    }
}
