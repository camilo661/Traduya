package com.traduya.feature.internetcall.peer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Código fijo de este teléfono para recibir llamadas, y el enlace para compartirlo. */
@Singleton
class InternetCallIdentity @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("internet_call", Context.MODE_PRIVATE)

    val code: String = prefs.getString(KEY_CODE, null) ?: generateCode().also {
        prefs.edit().putString(KEY_CODE, it).apply()
    }

    val peerId: String get() = "$PEER_PREFIX$code"

    /** Página web que abre la otra persona (iPhone, Android o computadora) para llamar. */
    val link: String get() = "$CALL_PAGE?to=$code"

    private fun generateCode(): String = (1..CODE_LENGTH).map { ALPHABET.random() }.joinToString("")

    private companion object {
        const val KEY_CODE = "code"
        const val CODE_LENGTH = 6
        // Sin letras que se confunden al dictarlas (0/o, 1/l/i)
        const val ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"
        const val PEER_PREFIX = "traduya-"
        const val CALL_PAGE = "https://camilo661.github.io/Traduya/"
    }
}
