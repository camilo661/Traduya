package com.traduya.core.telecom

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.traduya.core.model.ActiveCall
import com.traduya.core.model.CallStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Notificaciones de llamada entrante (pantalla completa) y de llamada en curso. */
@Singleton
class CallNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val launcher: CallUiLauncher,
) {
    private val manager = NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    fun update(call: ActiveCall?) {
        if (call == null || call.status == CallStatus.DISCONNECTED) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.notify(NOTIFICATION_ID, build(call))
    }

    private fun build(call: ActiveCall) = if (call.status == CallStatus.RINGING) {
        baseBuilder(CHANNEL_INCOMING, call)
            .setContentText("Llamada entrante · Traduya traducirá en tiempo real")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(openCallIntent(), true)
            .addAction(0, "Rechazar", actionIntent(CallActionReceiver.ACTION_HANG_UP))
            .addAction(0, "Contestar", actionIntent(CallActionReceiver.ACTION_ANSWER))
            .build()
    } else {
        baseBuilder(CHANNEL_ONGOING, call)
            .setContentText(if (call.status == CallStatus.ACTIVE) "Llamada en curso" else "Llamando…")
            .setUsesChronometer(call.connectedAtMillis != null)
            .setWhen(call.connectedAtMillis ?: System.currentTimeMillis())
            .addAction(0, "Colgar", actionIntent(CallActionReceiver.ACTION_HANG_UP))
            .build()
    }

    private fun baseBuilder(channel: String, call: ActiveCall) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_traduya)
            .setContentTitle(call.label)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setColor(BRAND_COLOR)
            .setContentIntent(openCallIntent())

    private fun openCallIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN,
        launcher.inCallIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun actionIntent(action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, CallActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_INCOMING, "Llamadas entrantes", NotificationManager.IMPORTANCE_HIGH).apply {
                // El sistema ya hace sonar el tono de llamada
                setSound(null, null)
            },
        )
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING, "Llamada en curso", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private companion object {
        const val CHANNEL_INCOMING = "calls_incoming"
        const val CHANNEL_ONGOING = "calls_ongoing"
        const val NOTIFICATION_ID = 1001
        const val REQUEST_OPEN = 1
        const val BRAND_COLOR = 0xFF29B6F6.toInt()
    }
}
