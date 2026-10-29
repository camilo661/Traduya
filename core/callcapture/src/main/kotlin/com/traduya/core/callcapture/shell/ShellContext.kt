package com.traduya.core.callcapture.shell

import android.content.AttributionSource
import android.content.Context
import android.content.ContextWrapper
import android.os.Process
import androidx.annotation.RequiresApi

/**
 * Contexto que se presenta como el paquete "com.android.shell".
 *
 * El proceso de Shizuku corre con el uid de shell, pero el contexto que recibe es el de
 * Traduya: sin este disfraz, el sistema rechaza la grabación porque el paquete
 * no corresponde al uid.
 */
internal class ShellContext(base: Context) : ContextWrapper(base) {

    override fun getPackageName(): String = SHELL_PACKAGE

    override fun getOpPackageName(): String = SHELL_PACKAGE

    @RequiresApi(31)
    override fun getAttributionSource(): AttributionSource =
        AttributionSource.Builder(Process.myUid()).setPackageName(SHELL_PACKAGE).build()

    override fun getApplicationContext(): Context = this

    private companion object {
        const val SHELL_PACKAGE = "com.android.shell"
    }
}
