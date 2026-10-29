package com.traduya.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Atajos para instalar y abrir Shizuku. */
internal object ShizukuIntents {
    private const val PACKAGE = "moe.shizuku.privileged.api"

    fun install(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE"))
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE"))
        start(context, market) || start(context, web)
    }

    fun open(context: Context) {
        context.packageManager.getLaunchIntentForPackage(PACKAGE)?.let { start(context, it) }
    }

    private fun start(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}
