package com.khalawat.android.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.khalawat.android.content.Language

enum class OnboardingScreen {
    WELCOME,
    PURPOSE,
    HOW_IT_WORKS,
    COMPANION_PIN,
    VPN_PERMISSION
}

/**
 * State machine driving the 5-screen onboarding flow.
 * Uses Compose mutableStateOf so that Composables recompose on changes.
 * compose-runtime is a pure-JVM library — this class remains unit-testable.
 */
class OnboardingState {
    private val screens = OnboardingScreen.entries
    private var currentIndex: Int = 0

    var currentScreen: OnboardingScreen by mutableStateOf(screens[0])
        private set

    var companionPinEnabled: Boolean by mutableStateOf(false)
        private set

    var companionPin: String? by mutableStateOf(null)
        private set

    var vpnPermissionGranted: Boolean by mutableStateOf(false)
        private set

    var isComplete: Boolean by mutableStateOf(false)
        private set

    var parentMessage: String by mutableStateOf("")
        private set

    var selectedLanguage: Language by mutableStateOf(Language.EN)
        private set

    fun next() {
        if (currentIndex < screens.size - 1) {
            currentIndex++
            currentScreen = screens[currentIndex]
        }
    }

    fun back() {
        if (currentIndex > 0) {
            currentIndex--
            currentScreen = screens[currentIndex]
        }
    }

    fun enableCompanionPin(enabled: Boolean) {
        companionPinEnabled = enabled
        if (!enabled) {
            companionPin = null
        }
    }

    /**
     * Sets the companion PIN. Returns true if valid (4 digits), false otherwise.
     */
    fun setCompanionPin(pin: String): Boolean {
        if (!companionPinEnabled) {
            companionPin = null
            return false
        }
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            return false
        }
        companionPin = pin
        return true
    }

    fun updateParentMessage(message: String) {
        parentMessage = message
    }

    fun changeLanguage(language: Language) {
        selectedLanguage = language
    }

    fun grantVpnPermission() {
        vpnPermissionGranted = true
        isComplete = true
    }
}
