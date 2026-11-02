package com.traduya.core.directory.internet

import com.traduya.core.directory.appusers.AppUsersApi
import com.traduya.core.directory.auth.AnonymousSession
import com.traduya.core.directory.numbers.NumbersApi
import com.traduya.core.domain.internetcall.InternetDirectory
import com.traduya.core.domain.wifinumber.WifiNumberRegistry
import com.traduya.core.model.DirectoryEntry
import java.io.IOException
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Directorio de números en Firebase, visto desde la app (con la sesión anónima de este teléfono). */
@Singleton
internal class FirebaseInternetDirectory @Inject constructor(
    private val session: AnonymousSession,
    private val numbers: NumbersApi,
    private val appUsers: AppUsersApi,
    private val registry: WifiNumberRegistry,
) : InternetDirectory {

    /** Los números registrados antes de esta versión no tenían su ficha de app: se crea una vez. */
    @Volatile
    private var appUserSavedFor: String? = null

    override suspend fun lookup(phoneNumber: String): DirectoryEntry? {
        ensureAppUser()
        val entry = numbers.lookup(phoneNumber, session.idToken()) ?: return null
        val active = entry.lastSeen?.let { Duration.between(it, Instant.now()) < ACTIVE_WINDOW } ?: false
        return DirectoryEntry(peerId = entry.peerId, isActive = active)
    }

    override suspend fun sessionToken(): String {
        ensureAppUser()
        return session.idToken()
    }

    override suspend fun markActive() {
        val phone = registry.account.value?.phoneNumber ?: return
        ensureAppUser()
        numbers.touch(phone, session.idToken())
    }

    private suspend fun ensureAppUser() {
        val phone = registry.account.value?.phoneNumber
            ?: throw IOException("Este teléfono no tiene su número registrado")
        if (appUserSavedFor == phone) return
        val uid = session.uid ?: throw IOException("Sesión sin usuario")
        appUsers.save(uid, phone, session.idToken())
        appUserSavedFor = phone
    }

    private companion object {
        /** Igual que en la web: se renueva cada 40 s y se considera activo durante 100 s. */
        val ACTIVE_WINDOW: Duration = Duration.ofSeconds(100)
    }
}
