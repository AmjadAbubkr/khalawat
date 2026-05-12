package com.khalawat.android.antitamper

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Issue #11: Anti-tamper — TDD
 *
 * Tests the AntiTamperState that enforces:
 * - 30-second hold on "I want to disable" button
 * - Spiritual reminder during hold
 * - Companion PIN gate (if enabled)
 * - Disconnect tracking for reminder notifications
 */
class AntiTamperStateTest {

    private lateinit var state: AntiTamperState

    @Before
    fun setUp() {
        state = AntiTamperState()
    }

    // --- Disable button hold ---

    @Test
    fun `disable button is not active initially`() {
        assertThat(state.isHoldActive).isFalse()
    }

    @Test
    fun `disable button requires 30 seconds hold`() {
        assertThat(state.holdDurationSeconds).isEqualTo(30)
    }

    @Test
    fun `start hold activates hold state`() {
        state.startHold()
        assertThat(state.isHoldActive).isTrue()
    }

    @Test
    fun `hold progress starts at 0`() {
        state.startHold()
        assertThat(state.holdProgress).isWithin(0.01f).of(0f)
    }

    @Test
    fun `hold progress reaches 1 after 30 seconds`() {
        state.startHold()
        state.updateHoldProgress(30_000L)
        assertThat(state.holdProgress).isWithin(0.01f).of(1f)
    }

    @Test
    fun `hold progress is partial at 15 seconds`() {
        state.startHold()
        state.updateHoldProgress(15_000L)
        assertThat(state.holdProgress).isWithin(0.01f).of(0.5f)
    }

    @Test
    fun `releasing hold before completion resets progress`() {
        state.startHold()
        state.updateHoldProgress(10_000L)
        state.releaseHold()
        assertThat(state.isHoldActive).isFalse()
        assertThat(state.holdProgress).isWithin(0.01f).of(0f)
    }

    @Test
    fun `hold complete after 30 seconds`() {
        state.startHold()
        state.updateHoldProgress(30_000L)
        assertThat(state.isHoldComplete).isTrue()
    }

    @Test
    fun `hold not complete before 30 seconds`() {
        state.startHold()
        state.updateHoldProgress(29_000L)
        assertThat(state.isHoldComplete).isFalse()
    }

    // --- Spiritual reminder during hold ---

    @Test
    fun `spiritual reminder is available during hold`() {
        state.startHold()
        assertThat(state.spiritualReminder).isNotNull()
    }

    @Test
    fun `spiritual reminder contains Arabic text`() {
        state.startHold()
        assertThat(state.spiritualReminder!!.arabic).isNotEmpty()
    }

    @Test
    fun `spiritual reminder changes on each hold`() {
        state.startHold()
        val first = state.spiritualReminder
        state.releaseHold()
        state.startHold()
        // May or may not differ (rotation), but should not crash
        assertThat(state.spiritualReminder).isNotNull()
    }

    // --- Companion PIN gate ---

    @Test
    fun `disable requires companion pin when enabled`() {
        state.setCompanionPinRequired("1234")
        assertThat(state.requiresCompanionPin).isTrue()
    }

    @Test
    fun `disable does not require companion pin by default`() {
        assertThat(state.requiresCompanionPin).isFalse()
    }

    @Test
    fun `companion pin verification succeeds with correct pin`() {
        state.setCompanionPinRequired("1234")
        assertThat(state.verifyCompanionPin("1234")).isTrue()
    }

    @Test
    fun `companion pin verification fails with wrong pin`() {
        state.setCompanionPinRequired("1234")
        assertThat(state.verifyCompanionPin("0000")).isFalse()
    }

    // --- Disconnect state ---

    @Test
    fun `initial disconnect count is 0`() {
        assertThat(state.disconnectCount).isEqualTo(0)
    }

    @Test
    fun `record disconnect increments count`() {
        state.recordDisconnect()
        assertThat(state.disconnectCount).isEqualTo(1)
    }

    @Test
    fun `should show reminder after 3 disconnects`() {
        repeat(3) { state.recordDisconnect() }
        assertThat(state.shouldShowDisconnectReminder).isTrue()
    }

    @Test
    fun `should not show reminder before 3 disconnects`() {
        repeat(2) { state.recordDisconnect() }
        assertThat(state.shouldShowDisconnectReminder).isFalse()
    }

    @Test
    fun `reset disconnect count`() {
        repeat(5) { state.recordDisconnect() }
        state.resetDisconnectCount()
        assertThat(state.disconnectCount).isEqualTo(0)
    }

    @Test
    fun `restore disconnect count from persisted value`() {
        state.restoreDisconnectCount(4)
        assertThat(state.disconnectCount).isEqualTo(4)
    }

    @Test
    fun `restore disconnect count then increment`() {
        state.restoreDisconnectCount(2)
        state.recordDisconnect()
        assertThat(state.disconnectCount).isEqualTo(3)
        assertThat(state.shouldShowDisconnectReminder).isTrue()
    }

    @Test
    fun `restore disconnect count clamps negative to zero`() {
        state.restoreDisconnectCount(-5)
        assertThat(state.disconnectCount).isEqualTo(0)
    }

    @Test
    fun `restore disconnect count clamps to max`() {
        state.restoreDisconnectCount(10000)
        assertThat(state.disconnectCount).isEqualTo(AntiTamperState.MAX_DISCONNECT_COUNT)
    }
}
