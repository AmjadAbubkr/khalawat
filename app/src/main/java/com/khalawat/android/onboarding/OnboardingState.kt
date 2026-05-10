package com.khalawat.android.onboarding

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
 * Pure logic — fully unit-testable, no Android dependencies.
 */
class OnboardingState {

    private val screens = OnboardingScreen.entries
    private var currentIndex: Int = 0

    var currentScreen: OnboardingScreen = screens[0]
        private set

    var companionPinEnabled: Boolean = false
        private set

    var companionPin: String? = null
        private set

    var vpnPermissionGranted: Boolean = false
        private set

    var isComplete: Boolean = false
        private set

    var parentMessage: String = ""
        private set

    var selectedLanguage: Language = Language.EN
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

    fun setCompanionPinEnabled(enabled: Boolean) {
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

    fun setParentMessage(message: String) {
        parentMessage = message
    }

    fun setSelectedLanguage(language: Language) {
        selectedLanguage = language
    }

    fun grantVpnPermission() {
        vpnPermissionGranted = true
        isComplete = true
    }
}
