package com.traduya.core.domain.wifinumber

import com.traduya.core.model.WifiNumberAccount
import com.traduya.core.model.WifiNumberError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegisterWifiNumberUseCaseTest {

    private class FakeRegistry(override val isConfigured: Boolean = true) : WifiNumberRegistry {
        override val account: StateFlow<WifiNumberAccount?> = MutableStateFlow(null)
        var registered: String? = null

        override suspend fun register(phoneNumber: String): WifiNumberAccount {
            registered = phoneNumber
            return WifiNumberAccount(phoneNumber, "traduya-test")
        }

        override suspend fun unregister() = Unit
    }

    private fun Result<*>.error() = (exceptionOrNull() as? WifiNumberException)?.error

    @Test
    fun `registers the normalized number`() = runTest {
        val registry = FakeRegistry()

        val result = RegisterWifiNumberUseCase(registry)("+57 300 123 4567")

        assertEquals("+573001234567", result.getOrNull()?.phoneNumber)
        assertEquals("+573001234567", registry.registered)
    }

    @Test
    fun `refuses invalid numbers without calling the directory`() = runTest {
        val registry = FakeRegistry()

        val result = RegisterWifiNumberUseCase(registry)("300 123")

        assertEquals(WifiNumberError.INVALID_NUMBER, result.error())
        assertNull(registry.registered)
    }

    @Test
    fun `reports when the directory is not configured`() = runTest {
        val result = RegisterWifiNumberUseCase(FakeRegistry(isConfigured = false))("+573001234567")

        assertEquals(WifiNumberError.NOT_CONFIGURED, result.error())
    }
}
