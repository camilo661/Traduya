package com.traduya.core.telecom

import android.content.Context
import android.content.Intent

/**
 * Abre la pantalla de llamada. La implementa el módulo app,
 * así telecom no necesita conocer las pantallas.
 */
interface CallUiLauncher {
    fun inCallIntent(context: Context): Intent
}
