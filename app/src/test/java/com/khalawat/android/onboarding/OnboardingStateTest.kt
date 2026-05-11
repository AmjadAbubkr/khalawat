package com.khalawat.android.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Issue #9 + #10: OnboardingFlow + CompanionPin — TDD
 *
 * Tests the OnboardingState (state machine) that drives the 5-screen
 * onboarding flow. Compose UI consumes this state; the state machine
 * uses mutableStateOf so Composables recompose on changes.
 */
class OnboardingStateTest {

    private lateinit var state: OnboardingState

    @Before
    fun setUp() {
        state = OnboardingState()
    }

    // --- Screen progression ---

    @Test
    fun `initial screen is welcome`() {
        assertThat(state.currentScreen).isEqualTo(OnboardingScreen.WELCOME)
    }

    @Test
    fun `next advances from welcome to purpose`() {
        state.next()
        assertThat(state.currentScreen).isEqualTo(OnboardingScreen.PURPOSE)
    }

    @Test
    fun `next advances from purpose to how_it_works`() {
        state.next() // welcome → purpose
        state.next() // purpose → how_it_works
        assertThat(state.currentScreen).isEqualTo(OnboardingScreen.HOW_IT_WORKS)
    }

    @Test
    fun `next advances from how_it_works to companion_pin`() {
        state.next(); state.next(); state.next()
        assertThat(state.currentScreen).isEqualTo(OnboardingScreen.COMPANION_PIN)
    }

    @Test
    fun `next advances from companion_pin to vpn_permission`() {
        state.next(); state.next(); state.next(); state.next()
        assertThat(state.currentScreen).isEqualTo(OnboardingScreen.VPN_PERMISSION)
    }

    @Test
    fun `next on vpn_permission does not advance further`() {
        repeat(5) { state.next() }
        assertThat(state.currentScreen).isEqualTo(OnboardingScreen.VPN_PERMISSION)
    }

    // --- Back navigation ---

    @Test
    fun `back on welcome stays on welcome`() {
        state.back()
        assertThat(state.currentScreen).isEqualTo(OnboardingScreen.WELCOME)
    }

    @Test
    fun `back returns to previous screen`() {
        state.next() // welcome → purpose
        state.back()
        assertThat(state.currentScreen).isEqualTo(OnboardingScreen.WELCOME)
    }

    // --- Companion PIN ---

    @Test
    fun `companion pin is disabled by default`() {
        assertThat(state.companionPinEnabled).isFalse()
    }

    @Test
    fun `enable companion pin`() {
        state.enableCompanionPin(true)
        assertThat(state.companionPinEnabled).isTrue()
    }

    @Test
    fun `set companion pin code`() {
        state.enableCompanionPin(true)
        state.setCompanionPin("1234")
        assertThat(state.companionPin).isEqualTo("1234")
    }

    @Test
    fun `companion pin code is null when disabled`() {
        state.setCompanionPin("1234")
        assertThat(state.companionPin).isNull()
    }

    @Test
    fun `companion pin must be 4 digits`() {
        state.enableCompanionPin(true)
        assertThat(state.setCompanionPin("12")).isFalse()
        assertThat(state.companionPin).isNull()
    }

    @Test
    fun `companion pin accepts valid 4-digit code`() {
        state.enableCompanionPin(true)
        assertThat(state.setCompanionPin("5678")).isTrue()
        assertThat(state.companionPin).isEqualTo("5678")
    }

    @Test
    fun `companion pin rejects non-numeric`() {
        state.enableCompanionPin(true)
        assertThat(state.setCompanionPin("abcd")).isFalse()
        assertThat(state.companionPin).isNull()
    }

    // --- Completion ---

    @Test
    fun `onboarding is not complete by default`() {
        assertThat(state.isComplete).isFalse()
    }

    @Test
    fun `onboarding completes when vpn permission granted`() {
        state.grantVpnPermission()
        assertThat(state.isComplete).isTrue()
    }

    @Test
    fun `vpn permission not granted by default`() {
        assertThat(state.vpnPermissionGranted).isFalse()
    }

    // --- Parent message ---

    @Test
    fun `parent message is empty by default`() {
        assertThat(state.parentMessage).isEmpty()
    }

    @Test
    fun `set parent message`() {
        state.enableCompanionPin(true)
        state.updateParentMessage("Remember Allah is watching")
        assertThat(state.parentMessage).isEqualTo("Remember Allah is watching")
    }

    // --- Language preference ---

    @Test
    fun `default language is EN`() {
        assertThat(state.selectedLanguage).isEqualTo(com.khalawat.android.content.Language.EN)
    }

    @Test
    fun `set language preference`() {
        state.changeLanguage(com.khalawat.android.content.Language.AR)
        assertThat(state.selectedLanguage).isEqualTo(com.khalawat.android.content.Language.AR)
    }
}
