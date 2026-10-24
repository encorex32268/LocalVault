package com.lihan.localvault.core.biometric

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity


enum class BiometricAvailability {
    READY,
    NOT_ENROLLED,
    UNAVAILABLE
}

sealed interface BiometricResult {
    data object Success : BiometricResult
    data class Error(val errorCode: Int, val message: String) : BiometricResult
    data object Failed : BiometricResult
    data object Dismissed : BiometricResult
}

class BiometricPromptManager(
    private val context: Context
) {
    private val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

    fun checkBiometricAvailability(): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    fun promptBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        description: String? = null,
        onResult: (BiometricResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                subtitle?.let { setSubtitle(it) }
                description?.let { setDescription(it) }
            }
            .setAllowedAuthenticators(authenticators)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(BiometricResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onResult(BiometricResult.Dismissed)
                    } else {
                        onResult(BiometricResult.Error(errorCode, errString.toString()))
                    }
                }

                override fun onAuthenticationFailed() {
                    onResult(BiometricResult.Failed)
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}


