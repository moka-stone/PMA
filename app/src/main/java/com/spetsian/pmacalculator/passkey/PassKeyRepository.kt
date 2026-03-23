package com.spetsian.pmacalculator.passkey

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Хранит только хеш PIN (SHA-256 + соль), не сам PIN.
 * SharedPreferences зашифрованы через EncryptedSharedPreferences (AES).
 */
class PassKeyRepository(context: Context) {

    private val appContext = context.applicationContext

    private val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs = EncryptedSharedPreferences.create(
        PREFS_FILE,
        masterKeyAlias,
        appContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isPinConfigured(): Boolean = prefs.getBoolean(KEY_CONFIGURED, false)

    fun isBiometricUnlockEnabled(): Boolean =
        prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricUnlockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun savePin(pin: String) {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putBoolean(KEY_CONFIGURED, true)
            .apply()
    }

    /**
     * Смена PIN: пересчитать хеш, флаг биометрии сохраняем как был (вызывающий может переустановить).
     */
    fun updatePin(newPin: String) {
        val keepBio = isBiometricUnlockEnabled()
        savePin(newPin)
        setBiometricUnlockEnabled(keepBio)
    }

    fun verifyPin(pin: String): Boolean {
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)
        val actual = hashPin(pin, salt)
        return actual.contentEquals(expected)
    }

    /** Сброс (забыли PIN после биометрии). */
    fun clearPin() {
        prefs.edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .putBoolean(KEY_CONFIGURED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(StandardCharsets.UTF_8))
        return digest.digest()
    }

    companion object {
        private const val PREFS_FILE = "pass_key_encrypted_prefs"
        private const val KEY_CONFIGURED = "configured"
        private const val KEY_SALT = "salt_b64"
        private const val KEY_HASH = "hash_b64"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val SALT_LEN = 16

        const val PIN_MIN_LEN = 4
        const val PIN_MAX_LEN = 6

        fun isValidPinFormat(pin: String): Boolean =
            pin.length in PIN_MIN_LEN..PIN_MAX_LEN && pin.all { it.isDigit() }
    }
}
