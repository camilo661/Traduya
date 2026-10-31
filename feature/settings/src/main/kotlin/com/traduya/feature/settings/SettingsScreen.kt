package com.traduya.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.traduya.core.designsystem.component.SectionCard
import com.traduya.core.designsystem.component.TraduyaLogo
import com.traduya.core.model.CallLanguagePreference
import com.traduya.core.model.Language
import com.traduya.core.model.LanguagePackStatus
import com.traduya.core.telecom.DialerRole

@Composable
fun SettingsRoute(onOpenWifiNumber: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isDefaultDialer by remember { mutableStateOf(true) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isDefaultDialer = DialerRole.isDefault(context)
        viewModel.refreshCallAudioTap()
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultDialer = DialerRole.isDefault(context)
    }

    SettingsScreen(
        uiState = uiState,
        isDefaultDialer = isDefaultDialer,
        onLanguagePreference = viewModel::setLanguagePreference,
        onAutoTranslate = viewModel::setAutoTranslate,
        onAutoSpeaker = viewModel::setAutoSpeaker,
        onRecordCalls = viewModel::setRecordCalls,
        onDownloadPack = viewModel::downloadPack,
        onMakeDefault = { roleLauncher.launch(DialerRole.requestIntent(context)) },
        onInstallShizuku = { ShizukuIntents.install(context) },
        onOpenShizuku = { ShizukuIntents.open(context) },
        onAllowShizuku = viewModel::allowCallAudioTap,
        onOpenWifiNumber = onOpenWifiNumber,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    isDefaultDialer: Boolean,
    onLanguagePreference: (CallLanguagePreference) -> Unit,
    onAutoTranslate: (Boolean) -> Unit,
    onAutoSpeaker: (Boolean) -> Unit,
    onRecordCalls: (Boolean) -> Unit,
    onDownloadPack: (Language) -> Unit,
    onMakeDefault: () -> Unit,
    onInstallShizuku: () -> Unit,
    onOpenShizuku: () -> Unit,
    onAllowShizuku: () -> Unit,
    onOpenWifiNumber: () -> Unit,
) {
    val settings = uiState.settings
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 8.dp, top = 20.dp),
        )

        CallAudioTapCard(
            status = uiState.callAudioTap,
            onInstall = onInstallShizuku,
            onOpenShizuku = onOpenShizuku,
            onAllow = onAllowShizuku,
        )

        WifiNumberCard(phoneNumber = uiState.wifiNumber, onOpen = onOpenWifiNumber)

        SectionCard(title = "Idioma de la otra persona") {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    CallLanguagePreference.entries.forEachIndexed { index, preference ->
                        SegmentedButton(
                            selected = settings.languagePreference == preference,
                            onClick = { onLanguagePreference(preference) },
                            shape = SegmentedButtonDefaults.itemShape(index, CallLanguagePreference.entries.size),
                        ) { Text(preference.label) }
                    }
                }
                Text(
                    text = settings.languagePreference.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        SectionCard(title = "Durante la llamada") {
            SwitchRow(
                title = "Traducir automáticamente",
                subtitle = "Empieza a traducir en cuanto contestan",
                checked = settings.autoTranslate,
                onCheckedChange = onAutoTranslate,
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SwitchRow(
                title = "Altavoz automático",
                subtitle = "Con Shizuku, Traduya solo escucha a la otra persona aunque uses el altavoz",
                checked = settings.autoSpeaker,
                onCheckedChange = onAutoSpeaker,
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SwitchRow(
                title = "Grabar llamadas",
                subtitle = "Guarda el audio en el historial. Avisa a la otra persona",
                checked = settings.recordCalls,
                onCheckedChange = onRecordCalls,
            )
        }

        SectionCard(title = "Idiomas (voz y traducción)") {
            Language.entries.forEachIndexed { index, language ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                PackRow(
                    language = language,
                    status = uiState.packs[language] ?: LanguagePackStatus.Missing,
                    onDownload = { onDownloadPack(language) },
                )
            }
        }

        SectionCard(title = "Teléfono") {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("App de teléfono predeterminada", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (isDefaultDialer) "Traduya gestiona tus llamadas" else "Traduya no está activa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isDefaultDialer) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Activa", tint = MaterialTheme.colorScheme.primary)
                } else {
                    TextButton(onClick = onMakeDefault) { Text("Activar") }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TraduyaLogo(size = 40.dp)
            Spacer(Modifier.height(8.dp))
            Text("Traduya 1.6.0", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "Traductor de llamadas en tiempo real con IA",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PackRow(language: Language, status: LanguagePackStatus, onDownload: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(language.flag, style = MaterialTheme.typography.titleLarge)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(language.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = when (status) {
                    LanguagePackStatus.Ready -> if (language == Language.SPANISH) {
                        "Listo · reconoce tu voz para no mostrarla"
                    } else {
                        "Listo · funciona sin internet"
                    }
                    is LanguagePackStatus.Downloading -> "Descargando… ${(status.progress * 100).toInt()} %"
                    LanguagePackStatus.Failed -> "Error al descargar"
                    LanguagePackStatus.Missing -> "Necesario para traducir (≈ 70 MB)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (status) {
            LanguagePackStatus.Ready -> Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = "Descargado",
                tint = MaterialTheme.colorScheme.primary,
            )

            is LanguagePackStatus.Downloading -> CircularProgressIndicator(
                progress = { status.progress },
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
            )

            LanguagePackStatus.Failed -> FilledTonalButton(onClick = onDownload) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Reintentar", modifier = Modifier.padding(start = 6.dp))
            }

            LanguagePackStatus.Missing -> FilledTonalButton(onClick = onDownload) { Text("Descargar") }
        }
    }
}
