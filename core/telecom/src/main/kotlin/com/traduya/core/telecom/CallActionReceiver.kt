package com.traduya.core.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Recibe los botones "Contestar" y "Colgar" de la notificación. */
class CallActionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CallManagerEntryPoint {
        fun callManager(): CallManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        val callManager = EntryPointAccessors
            .fromApplication(context, CallManagerEntryPoint::class.java)
            .callManager()

        when (intent.action) {
            ACTION_ANSWER -> {
                callManager.answer()
                runCatching {
                    val entryPoint = EntryPointAccessors.fromApplication(context, LauncherEntryPoint::class.java)
                    context.startActivity(
                        entryPoint.launcher().inCallIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }

            ACTION_HANG_UP -> callManager.hangUp()
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LauncherEntryPoint {
        fun launcher(): CallUiLauncher
    }

    companion object {
        const val ACTION_ANSWER = "com.traduya.action.ANSWER"
        const val ACTION_HANG_UP = "com.traduya.action.HANG_UP"
    }
}
