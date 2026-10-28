package com.traduya.core.domain.translation

import com.traduya.core.model.Language
import kotlinx.coroutines.flow.Flow

/**
 * Paquetes de idioma que se descargan al teléfono para funcionar sin internet:
 * el modelo de reconocimiento de voz y el de traducción.
 */
interface LanguagePacks {
    suspend fun installed(): Set<Language>

    /** Emite el progreso de 0 a 1 y termina cuando el paquete está listo. */
    fun install(language: Language): Flow<Float>
}
