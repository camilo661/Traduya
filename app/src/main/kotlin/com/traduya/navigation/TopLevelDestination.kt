package com.traduya.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    DIALER(Routes.DIALER, "Teclado", Icons.Rounded.Dialpad, Icons.Outlined.Dialpad),
    HISTORY(Routes.HISTORY, "Historial", Icons.Rounded.History, Icons.Outlined.History),
    SETTINGS(Routes.SETTINGS, "Ajustes", Icons.Rounded.Settings, Icons.Outlined.Settings),
}

object Routes {
    const val DIALER = "dialer"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val CALL_DETAIL = "history/{callId}"

    fun callDetail(callId: Long) = "history/$callId"
}
