package com.example

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.test.core.app.ApplicationProvider
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BiometricAuthHelperTest {

    @Test
    fun allowedAuthenticators_adaptsByApiLevel() {
        val api30Plus = BiometricAuthHelper.allowedAuthenticators(Build.VERSION_CODES.R)
        assertEquals(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            api30Plus
        )

        val api29 = BiometricAuthHelper.allowedAuthenticators(Build.VERSION_CODES.Q)
        assertEquals(BiometricManager.Authenticators.BIOMETRIC_WEAK, api29)
    }

    @Test
    fun buildPromptInfo_respectsNegativeButtonConstraintAcrossApiLevels() {
        val infoApi30 = BiometricAuthHelper.buildPromptInfo(
            title = "Buka Portal",
            subtitle = "Gunakan Sidik Jari",
            negativeButtonText = "Batal",
            sdkInt = Build.VERSION_CODES.R
        )
        assertEquals(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            infoApi30.allowedAuthenticators
        )
        assertEquals("", infoApi30.negativeButtonText.toString())

        val infoApi29 = BiometricAuthHelper.buildPromptInfo(
            title = "Buka Portal",
            subtitle = "Gunakan Sidik Jari",
            negativeButtonText = "Batal",
            sdkInt = Build.VERSION_CODES.Q
        )
        assertEquals(BiometricManager.Authenticators.BIOMETRIC_WEAK, infoApi29.allowedAuthenticators)
        assertEquals("Batal", infoApi29.negativeButtonText.toString())
    }

    @Test
    fun mapCanAuthenticateResult_mapsAllBiometricStates() {
        assertEquals(
            BiometricAvailability.AVAILABLE,
            BiometricAuthHelper.mapCanAuthenticateResult(BiometricManager.BIOMETRIC_SUCCESS)
        )
        assertEquals(
            BiometricAvailability.NONE_ENROLLED,
            BiometricAuthHelper.mapCanAuthenticateResult(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
        )
        assertEquals(
            BiometricAvailability.TEMPORARILY_UNAVAILABLE,
            BiometricAuthHelper.mapCanAuthenticateResult(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE)
        )
        assertEquals(
            BiometricAvailability.NO_HARDWARE,
            BiometricAuthHelper.mapCanAuthenticateResult(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)
        )
    }

    @Test
    fun shouldRelockAfterBackground_respectsTimeoutThreshold() {
        assertFalse(BiometricAuthHelper.shouldRelockAfterBackground(0L, 50_000L))
        assertFalse(BiometricAuthHelper.shouldRelockAfterBackground(10_000L, 20_000L, timeoutMs = 15_000L))
        assertTrue(BiometricAuthHelper.shouldRelockAfterBackground(10_000L, 25_000L, timeoutMs = 15_000L))
    }

    @Test
    fun formatBiometricError_returnsUserFriendlyIndonesianMessages() {
        val canceled = BiometricAuthHelper.formatBiometricError(
            BiometricPrompt.ERROR_USER_CANCELED,
            "Canceled"
        )
        assertTrue(canceled.contains("dibatalkan", ignoreCase = true))

        val lockout = BiometricAuthHelper.formatBiometricError(
            BiometricPrompt.ERROR_LOCKOUT,
            "Too many attempts"
        )
        assertTrue(lockout.contains("30 detik"))
    }

    @Test
    fun preferences_persistIndependentlyPerUser() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        BiometricAuthHelper.setPortalLockEnabled(context, "uid_a", true)
        BiometricAuthHelper.setPaymentBiometricEnabled(context, "uid_a", true)
        BiometricAuthHelper.setHomePromoDismissed(context, "uid_a", true)

        assertTrue(BiometricAuthHelper.isPortalLockEnabled(context, "uid_a"))
        assertTrue(BiometricAuthHelper.isPaymentBiometricEnabled(context, "uid_a"))
        assertTrue(BiometricAuthHelper.isHomePromoDismissed(context, "uid_a"))

        assertFalse(BiometricAuthHelper.isPortalLockEnabled(context, "uid_b"))
        assertFalse(BiometricAuthHelper.isPaymentBiometricEnabled(context, "uid_b"))
        assertFalse(BiometricAuthHelper.isHomePromoDismissed(context, "uid_b"))
    }
}
