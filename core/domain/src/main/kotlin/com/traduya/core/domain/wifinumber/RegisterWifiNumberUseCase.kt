package com.traduya.core.domain.wifinumber

import com.traduya.core.model.WifiNumberAccount
import com.traduya.core.model.WifiNumberError
import javax.inject.Inject

/** Valida el número que escribió el usuario y lo registra en el directorio. */
class RegisterWifiNumberUseCase @Inject constructor(
    private val registry: WifiNumberRegistry,
) {
    suspend operator fun invoke(rawNumber: String): Result<WifiNumberAccount> {
        if (!registry.isConfigured) return Result.failure(WifiNumberException(WifiNumberError.NOT_CONFIGURED))
        val number = PhoneNumberRules.normalize(rawNumber)
            ?: return Result.failure(WifiNumberException(WifiNumberError.INVALID_NUMBER))
        return runCatching { registry.register(number) }
    }
}
