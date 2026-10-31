package com.traduya.feature.wifinumber

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.traduya.core.designsystem.theme.TraduyaTheme
import com.traduya.feature.wifinumber.ui.WifiNumberRoute
import dagger.hilt.android.AndroidEntryPoint

/** Pantalla para registrar (o borrar) el número con el que te llaman por WiFi. */
@AndroidEntryPoint
class WifiNumberActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TraduyaTheme {
                WifiNumberRoute(onBack = ::finish)
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, WifiNumberActivity::class.java)
    }
}
