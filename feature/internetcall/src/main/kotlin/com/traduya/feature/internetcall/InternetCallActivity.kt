package com.traduya.feature.internetcall

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebViewAssetLoader
import com.traduya.core.callui.CallScreen
import com.traduya.core.designsystem.theme.TraduyaTheme
import com.traduya.feature.internetcall.peer.PeerState
import com.traduya.feature.internetcall.ui.WaitingScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Llamadas traducidas por internet.
 * Un WebView invisible mantiene la conexión WebRTC; la interfaz es Compose.
 */
@AndroidEntryPoint
class InternetCallActivity : ComponentActivity() {

    private val viewModel: InternetCallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        setContent {
            TraduyaTheme {
                val peerState by viewModel.peerState.collectAsStateWithLifecycle()
                val callState by viewModel.callState.collectAsStateWithLifecycle()

                Box(Modifier.fillMaxSize()) {
                    // La conexión WebRTC vive aquí; es invisible pero debe estar en pantalla para no pausarse
                    AndroidView(
                        factory = ::createPeerWebView,
                        modifier = Modifier
                            .size(1.dp)
                            .alpha(0f),
                    )
                    when (peerState) {
                        is PeerState.Incoming, PeerState.InCall, PeerState.Ended -> CallScreen(
                            uiState = callState,
                            onAnswer = viewModel::answer,
                            onHangUp = viewModel::hangUp,
                            onToggleMute = viewModel::toggleMute,
                            onToggleSpeaker = viewModel::toggleSpeaker,
                            onToggleTranslation = viewModel::toggleTranslation,
                            onDtmf = {},
                            showKeypad = false,
                        )

                        else -> WaitingScreen(
                            state = peerState,
                            code = viewModel.identity.code,
                            link = viewModel.identity.link,
                            onShare = ::shareLink,
                            onCopy = ::copyLink,
                            onBack = ::finish,
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createPeerWebView(context: android.content.Context): WebView {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
        return WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            addJavascriptInterface(viewModel.bridge, "Traduya")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
                    assetLoader.shouldInterceptRequest(request.url)
            }
            webChromeClient = object : WebChromeClient() {
                // El micrófono ya está autorizado para la app; solo lo pide nuestra página local
                override fun onPermissionRequest(request: PermissionRequest) {
                    if (request.origin.host == ASSET_HOST) {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                    } else {
                        request.deny()
                    }
                }
            }
            viewModel.bridge.attach(this)
            loadUrl("https://$ASSET_HOST/assets/peer/receiver.html?id=${viewModel.identity.peerId}")
        }
    }

    private fun shareLink() {
        val text = "Llámame con traducción en vivo / Call me with live translation: ${viewModel.identity.link}"
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
                "Compartir enlace de llamada",
            ),
        )
    }

    private fun copyLink() {
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("Enlace Traduya", viewModel.identity.link))
        Toast.makeText(this, "Enlace copiado", Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val ASSET_HOST = "appassets.androidplatform.net"
    }
}
