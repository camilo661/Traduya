package com.traduya

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.traduya.core.designsystem.theme.TraduyaTheme
import com.traduya.feature.onboarding.OnboardingRoute
import com.traduya.navigation.TraduyaMainScaffold
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Número recibido de otra app (por ejemplo, al tocar un teléfono en el navegador). */
    private var dialNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        dialNumber = intent.dialNumber()

        setContent {
            TraduyaTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    when (uiState) {
                        MainUiState.Loading -> Unit
                        MainUiState.NeedsOnboarding -> OnboardingRoute(onFinished = {})
                        MainUiState.Ready -> TraduyaMainScaffold(dialNumber = dialNumber)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dialNumber()?.let { dialNumber = it }
    }

    private fun Intent.dialNumber(): String? =
        if (action == Intent.ACTION_DIAL || action == Intent.ACTION_VIEW) data?.schemeSpecificPart else null
}
