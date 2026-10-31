package com.traduya.feature.internetcall.peer

import android.content.Context
import com.traduya.core.domain.internetcall.CallPage
import com.traduya.core.domain.wifinumber.WifiNumberRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cómo encuentran a este teléfono para llamarlo por internet.
 *
 * Si el usuario registró su número, lo llaman escribiendo ese número; si no,
 * con un código fijo de este teléfono. El enlace y el QR funcionan en ambos casos.
 */
@Singleton
class InternetCallIdentity @Inject constructor(
    @ApplicationContext context: Context,
    private val wifiNumbers: WifiNumberRegistry,
) {
    private val prefs = context.getSharedPreferences("internet_call", Context.MODE_PRIVATE)

    private val code: String = prefs.getString(KEY_CODE, null) ?: generateCode().also {
        prefs.edit().putString(KEY_CODE, it).apply()
    }

    private val phoneNumber: String? get() = wifiNumbers.account.value?.phoneNumber

    val peerId: String get() = wifiNumbers.account.value?.peerId ?: "$PEER_PREFIX$code"

    /** Lo que se muestra en pantalla: "Tu número: +57…" o "Tu código: abc123". */
    val label: String get() = phoneNumber?.let { "Tu número: $it" } ?: "Tu código: $code"

    /** Página web que abre la otra persona (iPhone, Android o computadora) para llamar. */
    val link: String
        get() = phoneNumber?.let { "${CallPage.URL}?n=${URLEncoder.encode(it, "UTF-8")}" } ?: "${CallPage.URL}?to=$code"

    private fun generateCode(): String = (1..CODE_LENGTH).map { ALPHABET.random() }.joinToString("")

    private companion object {
        const val KEY_CODE = "code"
        const val CODE_LENGTH = 6
        // Sin letras que se confunden al dictarlas (0/o, 1/l/i)
        const val ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"
        const val PEER_PREFIX = "traduya-"
    }
}
