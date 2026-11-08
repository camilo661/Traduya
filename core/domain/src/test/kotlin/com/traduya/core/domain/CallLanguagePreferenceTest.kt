package com.traduya.core.domain

import com.traduya.core.model.CallLanguagePreference
import com.traduya.core.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CallLanguagePreferenceTest {

    @Test
    fun `automatic listens for every language except the user's`() {
        assertEquals(
            listOf(Language.ENGLISH, Language.CHINESE),
            CallLanguagePreference.AUTOMATIC.foreignCandidates(Language.SPANISH),
        )
        assertEquals(
            listOf(Language.SPANISH, Language.CHINESE),
            CallLanguagePreference.AUTOMATIC.foreignCandidates(Language.ENGLISH),
        )
    }

    @Test
    fun `a fixed language is the only one heard`() {
        assertEquals(listOf(Language.SPANISH), CallLanguagePreference.SPANISH.foreignCandidates(Language.CHINESE))
    }

    @Test
    fun `the user's own language as the other person's falls back to automatic`() {
        assertEquals(
            listOf(Language.SPANISH, Language.CHINESE),
            CallLanguagePreference.ENGLISH.foreignCandidates(Language.ENGLISH),
        )
        assertFalse(CallLanguagePreference.ENGLISH in CallLanguagePreference.choicesFor(Language.ENGLISH))
    }
}
