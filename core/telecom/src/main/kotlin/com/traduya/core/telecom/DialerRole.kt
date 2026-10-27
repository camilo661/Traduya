package com.traduya.core.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

/** Ayuda a convertir Traduya en la app de teléfono predeterminada. */
object DialerRole {

    fun isDefault(context: Context): Boolean =
        roleManager(context).isRoleHeld(RoleManager.ROLE_DIALER)

    fun isAvailable(context: Context): Boolean =
        roleManager(context).isRoleAvailable(RoleManager.ROLE_DIALER)

    /** Intent que muestra el diálogo del sistema "¿Usar Traduya como app de teléfono?". */
    fun requestIntent(context: Context): Intent =
        roleManager(context).createRequestRoleIntent(RoleManager.ROLE_DIALER)

    private fun roleManager(context: Context): RoleManager =
        context.getSystemService(RoleManager::class.java)
}
