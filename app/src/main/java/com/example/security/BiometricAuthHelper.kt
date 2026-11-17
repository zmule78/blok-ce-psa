package com.example.security

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    AVAILABLE,
    NONE_ENROLLED,
    NO_HARDWARE,
    TEMPORARILY_UNAVAILABLE
}

data class BiometricSecurityState(
    val availability: BiometricAvailability = BiometricAvailability.NO_HARDWARE,
    val portalLockEnabled: Boolean = false,
    val requireOnPayment: Boolean = false,
    val isPortalLocked: Boolean = false,
    val homePromoDismissed: Boolean = false,
    val lastErrorMessage: String? = null
)

object BiometricAuthHelper {
    private const val PREFS_NAME = "blokce_biometric_prefs"
    private const val KEY_PORTAL_LOCK_PREFIX = "biometric_portal_lock_"
    private const val KEY_PAYMENT_LOCK_PREFIX = "biometric_payment_lock_"
    private const val KEY_PROMO_DISMISSED_PREFIX = "biometric_promo_dismissed_"
    const val DEFAULT_RELOCK_TIMEOUT_MS = 15_000L

    fun allowedAuthenticators(sdkInt: Int = Build.VERSION.SDK_INT): Int {
        return if (sdkInt >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
    }

    fun mapCanAuthenticateResult(result: Int): BiometricAvailability {
        return when (result) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.TEMPORARILY_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.NO_HARDWARE
            else -> BiometricAvailability.NO_HARDWARE
        }
    }

    fun checkAvailability(context: Context): BiometricAvailability {
        return try {
            val manager = BiometricManager.from(context.applicationContext)
            val result = manager.canAuthenticate(allowedAuthenticators())
            mapCanAuthenticateResult(result)
        } catch (_: Throwable) {
            BiometricAvailability.NO_HARDWARE
        }
    }

    fun shouldRelockAfterBackground(
        lastStoppedElapsedMs: Long,
        nowElapsedMs: Long,
        timeoutMs: Long = DEFAULT_RELOCK_TIMEOUT_MS
    ): Boolean {
        if (lastStoppedElapsedMs <= 0L) return false
        return (nowElapsedMs - lastStoppedElapsedMs) >= timeoutMs
    }

    fun formatBiometricError(errorCode: Int, rawError: CharSequence): String {
        return when (errorCode) {
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_CANCELED -> "Autentikasi dibatalkan. Ketuk tombol di bawah untuk mencoba lagi."
            BiometricPrompt.ERROR_LOCKOUT -> "Terlalu banyak percobaan gagal. Tunggu 30 detik lalu coba lagi."
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Sensor biometrik terkunci sementara. Gunakan kunci layar perangkat atau buka setelan keamanan."
            BiometricPrompt.ERROR_NO_BIOMETRICS -> "Belum ada sidik jari atau wajah yang terdaftar di perangkat ini."
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE -> "Sensor biometrik sedang tidak tersedia di perangkat ini."
            else -> rawError.toString().ifBlank { "Verifikasi biometrik gagal. Silakan coba lagi." }
        }
    }

    fun isPortalLockEnabled(context: Context, uid: String): Boolean {
        if (uid.isBlank()) return false
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("$KEY_PORTAL_LOCK_PREFIX$uid", false)
    }

    fun setPortalLockEnabled(context: Context, uid: String, enabled: Boolean) {
        if (uid.isBlank()) return
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("$KEY_PORTAL_LOCK_PREFIX$uid", enabled)
            .apply()
    }

    fun isPaymentBiometricEnabled(context: Context, uid: String): Boolean {
        if (uid.isBlank()) return false
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("$KEY_PAYMENT_LOCK_PREFIX$uid", false)
    }

    fun setPaymentBiometricEnabled(context: Context, uid: String, enabled: Boolean) {
        if (uid.isBlank()) return
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("$KEY_PAYMENT_LOCK_PREFIX$uid", enabled)
            .apply()
    }

    fun isHomePromoDismissed(context: Context, uid: String): Boolean {
        if (uid.isBlank()) return false
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("$KEY_PROMO_DISMISSED_PREFIX$uid", false)
    }

    fun setHomePromoDismissed(context: Context, uid: String, dismissed: Boolean) {
        if (uid.isBlank()) return
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("$KEY_PROMO_DISMISSED_PREFIX$uid", dismissed)
            .apply()
    }

    fun buildPromptInfo(
        title: String,
        subtitle: String,
        description: String = "",
        negativeButtonText: String = "Batal",
        sdkInt: Int = Build.VERSION.SDK_INT
    ): BiometricPrompt.PromptInfo {
        val allowed = allowedAuthenticators(sdkInt)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setConfirmationRequired(false)
            .setAllowedAuthenticators(allowed)

        if (description.isNotBlank()) {
            builder.setDescription(description)
        }
        if ((allowed and BiometricManager.Authenticators.DEVICE_CREDENTIAL) == 0) {
            builder.setNegativeButtonText(negativeButtonText)
        }
        return builder.build()
    }

    fun authenticate(
        context: Context,
        title: String,
        subtitle: String,
        description: String = "",
        negativeButtonText: String = "Batal",
        onPromptShown: () -> Unit = {},
        onPromptFinished: () -> Unit = {},
        onSuccess: () -> Unit,
        onError: (errorCode: Int, message: String) -> Unit,
        onFailedAttempt: () -> Unit = {}
    ) {
        val activity = context.findFragmentActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onError(
                BiometricPrompt.ERROR_CANCELED,
                "Tidak dapat membuka dialog biometrik saat ini."
            )
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onPromptFinished()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onPromptFinished()
                onError(errorCode, formatBiometricError(errorCode, errString))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailedAttempt()
            }
        }

        try {
            val prompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = buildPromptInfo(
                title = title,
                subtitle = subtitle,
                description = description,
                negativeButtonText = negativeButtonText
            )
            onPromptShown()
            prompt.authenticate(promptInfo)
        } catch (t: Throwable) {
            onPromptFinished()
            onError(
                BiometricPrompt.ERROR_HW_UNAVAILABLE,
                t.localizedMessage ?: "Sensor biometrik gagal diinisialisasi."
            )
        }
    }

    fun openBiometricEnrollSettings(context: Context) {
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(
                    Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            allowedAuthenticators()
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            add(
                Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            add(
                Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
        for (intent in candidates) {
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Try next settings fallback
            }
        }
    }
}

tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
