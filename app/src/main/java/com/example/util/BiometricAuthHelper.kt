package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricHardwareStatus(val label: String, val isHardwareAvailable: Boolean) {
    READY("Biometric sensor ready (Fingerprint / Face Unlock)", true),
    NONE_ENROLLED("Biometric sensor detected (No fingerprint/face enrolled yet — PIN fallback active)", true),
    NO_HARDWARE("Device has no biometric sensor (App PIN protection active)", false),
    UNAVAILABLE("Biometric hardware temporarily unavailable (App PIN fallback active)", false)
}

object BiometricAuthHelper {

    fun Context.findFragmentActivity(): FragmentActivity? {
        var ctx: Context? = this
        while (ctx is ContextWrapper) {
            if (ctx is FragmentActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun getHardwareStatus(context: Context): BiometricHardwareStatus {
        return try {
            val manager = BiometricManager.from(context)
            val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            } else {
                BIOMETRIC_WEAK
            }
            when (manager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricHardwareStatus.READY
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricHardwareStatus.NONE_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricHardwareStatus.NO_HARDWARE
                else -> BiometricHardwareStatus.UNAVAILABLE
            }
        } catch (_: Exception) {
            BiometricHardwareStatus.UNAVAILABLE
        }
    }

    fun authenticate(
        context: Context,
        title: String = "Unlock My Finance",
        subtitle: String = "Verify your identity with fingerprint, face, or device credential",
        onSuccess: () -> Unit,
        onFallbackToPin: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val activity = context.findFragmentActivity()
        if (activity == null) {
            onFallbackToPin("Use your security PIN to continue")
            return
        }

        val status = getHardwareStatus(context)
        if (status != BiometricHardwareStatus.READY) {
            onFallbackToPin(status.label)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                    errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                    errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE ||
                    errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL
                ) {
                    onFallbackToPin(errString.toString())
                } else if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric not recognized. Try again or use PIN.")
            }
        }

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                    .build()
            } else {
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(BIOMETRIC_WEAK)
                    .setNegativeButtonText("Use App PIN")
                    .build()
            }
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onFallbackToPin(e.localizedMessage ?: "Use App PIN")
        }
    }
}
