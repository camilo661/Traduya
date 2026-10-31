package com.traduya.core.domain.wifinumber

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberRulesTest {

    @Test
    fun `removes spaces dashes and parentheses`() {
        assertEquals("+573001234567", PhoneNumberRules.normalize(" +57 (300) 123-4567 "))
    }

    @Test
    fun `accepts the 00 international prefix`() {
        assertEquals("+13055551234", PhoneNumberRules.normalize("001 305 555 1234"))
    }

    @Test
    fun `rejects numbers without country code`() {
        assertNull(PhoneNumberRules.normalize("3001234567"))
    }

    @Test
    fun `rejects letters and numbers that are too short or too long`() {
        assertNull(PhoneNumberRules.normalize("+57 300 ABC 4567"))
        assertNull(PhoneNumberRules.normalize("+571234"))
        assertNull(PhoneNumberRules.normalize("+1234567890123456"))
    }

    @Test
    fun `knows the calling code of the SIM country`() {
        assertEquals("+57", PhoneNumberRules.callingCodeFor("CO"))
        assertNull(PhoneNumberRules.callingCodeFor("zz"))
        assertNull(PhoneNumberRules.callingCodeFor(null))
    }
}
