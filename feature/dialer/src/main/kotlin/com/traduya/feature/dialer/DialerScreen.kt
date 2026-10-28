package com.traduya.feature.dialer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.traduya.core.designsystem.component.DialPad
import com.traduya.core.designsystem.component.TraduyaWordmark
import com.traduya.core.designsystem.theme.CallGreen
import com.traduya.core.model.CallLanguagePreference
import com.traduya.core.telecom.DialerRole

@Composable
fun DialerRoute(
    initialNumber: String?,
    onOpenSettings: () -> Unit,
    onOpenInternetCall: () -> Unit,
    viewModel: DialerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isDefaultDialer by remember { mutableStateOf(true) }

    LaunchedEffect(initialNumber) {
        if (!initialNumber.isNullOrBlank()) viewModel.setNumber(initialNumber)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isDefaultDialer = DialerRole.isDefault(context)
        viewModel.refreshLanguagePacks()
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultDialer = DialerRole.isDefault(context)
    }

    DialerScreen(
        uiState = uiState,
        isDefaultDialer = isDefaultDialer,
        onKey = viewModel::onKey,
        onDelete = viewModel::deleteLast,
        onClear = viewModel::clear,
        onCall = viewModel::call,
        onMakeDefault = { roleLauncher.launch(DialerRole.requestIntent(context)) },
        onDismissError = viewModel::dismissError,
        onOpenSettings = onOpenSettings,
        onReturnToCall = viewModel::returnToCall,
        onOpenInternetCall = onOpenInternetCall,
    )
}

@Composable
internal fun DialerScreen(
    uiState: DialerUiState,
    isDefaultDialer: Boolean,
    onKey: (Char) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    onCall: () -> Unit,
    onMakeDefault: () -> Unit,
    onDismissError: () -> Unit,
    onOpenSettings: () -> Unit,
    onReturnToCall: () -> Unit,
    onOpenInternetCall: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TraduyaWordmark(modifier = Modifier.weight(1f))
            TranslationChip(uiState.languagePreference)
        }

        InternetCallCard(onClick = onOpenInternetCall)

        AnimatedVisibility(visible = uiState.ongoingCallLabel != null) {
            ReturnToCallBanner(label = uiState.ongoingCallLabel.orEmpty(), onClick = onReturnToCall)
        }
        AnimatedVisibility(visible = !isDefaultDialer) {
            InfoBanner(
                text = "Traduya aún no es tu app de teléfono. Actívala para traducir tus llamadas.",
                actionLabel = "Activar",
                onAction = onMakeDefault,
            )
        }
        AnimatedVisibility(visible = isDefaultDialer && uiState.languagePacksMissing) {
            InfoBanner(
                text = "Faltan los idiomas para traducir. Descárgalos una vez y funcionan sin internet.",
                actionLabel = "Descargar",
                onAction = onOpenSettings,
            )
        }
        AnimatedVisibility(visible = uiState.errorMessage != null) {
            InfoBanner(
                text = uiState.errorMessage.orEmpty(),
                actionLabel = "Entendido",
                onAction = onDismissError,
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = uiState.number.ifEmpty { "Marca un número" },
            style = MaterialTheme.typography.displayMedium,
            fontSize = if (uiState.number.length > 12) 30.sp else 38.sp,
            color = if (uiState.number.isEmpty()) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        DialPad(onKey = onKey, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = onCall,
                shape = CircleShape,
                color = CallGreen,
                shadowElevation = 6.dp,
                modifier = Modifier.size(76.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Llamar",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (uiState.number.isNotEmpty()) BackspaceButton(onDelete = onDelete, onClear = onClear)
            }
        }
    }
}

@Composable
private fun TranslationChip(preference: CallLanguagePreference) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Translate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = preference.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Acceso principal a la traducción: las llamadas por internet sí se pueden escuchar y traducir. */
@Composable
private fun InternetCallCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .background(com.traduya.core.designsystem.theme.TraduyaGradients.brand)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Translate,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(32.dp),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = "Llamada traducida",
                    style = MaterialTheme.typography.titleMedium,
                    color = androidx.compose.ui.graphics.Color.White,
                )
                Text(
                    text = "Comparte tu enlace y lee en español lo que te dicen, en vivo",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
            )
        }
    }
}

@Composable
private fun ReturnToCallBanner(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = CallGreen,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Call, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
            Text(
                text = "Llamada en curso con $label · Toca para volver",
                style = MaterialTheme.typography.titleSmall,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun InfoBanner(text: String, actionLabel: String, onAction: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BackspaceButton(onDelete: () -> Unit, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .combinedClickable(onClick = onDelete, onLongClick = onClear),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Backspace,
            contentDescription = "Borrar",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
