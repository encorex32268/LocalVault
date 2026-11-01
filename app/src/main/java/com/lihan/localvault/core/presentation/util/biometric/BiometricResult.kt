package com.lihan.localvault.core.presentation.util.biometric

sealed interface BiometricResult {
    data object Success : BiometricResult
    data class Error(val errorCode: Int, val message: String) : BiometricResult
    data object Failed : BiometricResult
    data object Dismissed : BiometricResult
}