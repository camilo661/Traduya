package com.traduya.core.translation.mlkit

import com.google.mlkit.nl.translate.TranslateLanguage
import com.traduya.core.model.Language

internal val Language.mlKitCode: String
    get() = when (this) {
        Language.SPANISH -> TranslateLanguage.SPANISH
        Language.ENGLISH -> TranslateLanguage.ENGLISH
        Language.CHINESE -> TranslateLanguage.CHINESE
    }
