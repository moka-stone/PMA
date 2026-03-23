package com.spetsian.pmacalculator.passkey

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.spetsian.pmacalculator.R

fun AppCompatActivity.canUseStrongBiometric(): Boolean {
    val bm = BiometricManager.from(this)
    return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS
}

fun AppCompatActivity.showBiometricAuth(
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onCanceled: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val executor = ContextCompat.getMainExecutor(this)
    val prompt = BiometricPrompt(
        this,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED -> onCanceled()
                    else -> if (errorCode != BiometricPrompt.ERROR_CANCELED) {
                        onError(errString.toString())
                    }
                }
            }

            override fun onAuthenticationFailed() {
                onError(getString(R.string.biometric_not_recognized))
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText(getString(R.string.cancel))
        .build()
    prompt.authenticate(info)
}
