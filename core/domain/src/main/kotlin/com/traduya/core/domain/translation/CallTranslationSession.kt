package com.traduya.core.domain.translation

import com.traduya.core.model.TranslationSessionState
import kotlinx.coroutines.flow.StateFlow

/** Sesión de traducción ligada a la llamada en curso. */
interface CallTranslationSession {
    val state: StateFlow<TranslationSessionState>
    fun setEnabled(enabled: Boolean)
}
