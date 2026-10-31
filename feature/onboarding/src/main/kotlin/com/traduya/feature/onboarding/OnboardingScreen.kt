package com.traduya.feature.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.traduya.core.designsystem.component.TraduyaLogo
import com.traduya.core.model.LanguagePackStatus
import com.traduya.core.telecom.CallPermissions
import com.traduya.core.telecom.DialerRole

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    fun refresh() = viewModel.refreshSystemState(
        permissionsGranted = CallPermissions.allGranted(context),
        isDefaultDialer = DialerRole.isDefault(context),
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refresh() }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refresh() }

    OnboardingScreen(
        uiState = uiState,
        onRequestPermissions = { permissionLauncher.launch(CallPermissions.required.toTypedArray()) },
        onRequestDefaultDialer = { roleLauncher.launch(DialerRole.requestIntent(context)) },
        onDownloadPacks = viewModel::downloadPacks,
        onContinue = {
            viewModel.finish()
            onFinished()
        },
    )
}

@Composable
internal fun OnboardingScreen(
    uiState: OnboardingUiState,
    onRequestPermissions: () -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onDownloadPacks: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        TraduyaLogo(size = 96.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Bienvenido a Traduya",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Recibe llamadas y lee en español, en tiempo real, lo que te dicen en inglés o chino.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SetupStep(
                number = 1,
                icon = Icons.Rounded.Mic,
                title = "Permisos",
                description = "Teléfono, micrófono y notificaciones para llamar y traducir.",
                done = uiState.permissionsGranted,
                actionLabel = "Conceder",
                onAction = onRequestPermissions,
            )
            SetupStep(
                number = 2,
                icon = Icons.Rounded.PhoneInTalk,
                title = "App de teléfono (opcional)",
                description = "Necesario para traducir llamadas normales. Para las llamadas por internet no hace falta.",
                done = uiState.isDefaultDialer,
                actionLabel = "Activar",
                onAction = onRequestDefaultDialer,
            )
            SetupStep(
                number = 3,
                icon = Icons.Rounded.CloudDownload,
                title = "Idiomas",
                description = when (val status = uiState.packsStatus) {
                    LanguagePackStatus.Failed -> "No se pudo descargar. Revisa tu conexión e inténtalo de nuevo."
                    is LanguagePackStatus.Downloading -> "Descargando… ${(status.progress * 100).toInt()} %"
                    else -> "Voz y traducción de español, inglés y chino (≈ 200 MB). Usa Wi-Fi."
                },
                done = uiState.packsStatus == LanguagePackStatus.Ready,
                loading = uiState.packsStatus is LanguagePackStatus.Downloading,
                progress = (uiState.packsStatus as? LanguagePackStatus.Downloading)?.progress,
                actionLabel = if (uiState.packsStatus == LanguagePackStatus.Failed) "Reintentar" else "Descargar",
                onAction = onDownloadPacks,
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            enabled = uiState.canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Comenzar", style = MaterialTheme.typography.titleMedium)
        }
        if (!uiState.canContinue) {
            Text(
                text = "Completa los pasos 1 y 3 para continuar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SetupStep(
    number: Int,
    icon: ImageVector,
    title: String,
    description: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    loading: Boolean = false,
    progress: Float? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = "$number. $title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                done -> Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Listo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )

                loading && progress != null -> CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                )

                loading -> CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)

                else -> FilledTonalButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
