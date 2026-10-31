package com.traduya.core.directory

/**
 * Dirección del proyecto de Firebase que guarda el directorio de números.
 * La clave web de Firebase es pública por diseño: la seguridad está en las reglas (firebase/firestore.rules).
 */
internal data class FirebaseConfig(
    val projectId: String,
    val apiKey: String,
    val authUrl: String,
    val tokenUrl: String,
    val firestoreUrl: String,
) {
    val isConfigured: Boolean get() = projectId.isNotBlank() && apiKey.isNotBlank()

    /** Ruta de la colección de números en la API REST de Firestore. */
    val numbersUrl: String get() = "$firestoreUrl/v1/projects/$projectId/databases/(default)/documents/numbers"

    companion object {
        private const val EMULATOR_PROJECT_ID = "demo-traduya"

        fun fromBuildConfig(): FirebaseConfig {
            val emulator = BuildConfig.FIREBASE_EMULATOR_HOST.takeIf { BuildConfig.DEBUG && it.isNotBlank() }
            return if (emulator != null) {
                FirebaseConfig(
                    // Mismo proyecto de prueba que la página web y las pruebas de las reglas
                    projectId = EMULATOR_PROJECT_ID,
                    apiKey = "emulator",
                    authUrl = "http://$emulator:9099/identitytoolkit.googleapis.com",
                    tokenUrl = "http://$emulator:9099/securetoken.googleapis.com",
                    firestoreUrl = "http://$emulator:8080",
                )
            } else {
                FirebaseConfig(
                    projectId = BuildConfig.FIREBASE_PROJECT_ID,
                    apiKey = BuildConfig.FIREBASE_API_KEY,
                    authUrl = "https://identitytoolkit.googleapis.com",
                    tokenUrl = "https://securetoken.googleapis.com",
                    firestoreUrl = "https://firestore.googleapis.com",
                )
            }
        }
    }
}
