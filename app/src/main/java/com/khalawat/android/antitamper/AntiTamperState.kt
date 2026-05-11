package com.khalawat.android.antitamper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * State machine for the anti-tamper "disable" flow.
 * Pure logic — fully unit-testable.
 * Uses Compose mutableStateOf so that Composables (DisableScreen) recompose on changes.
 * compose-runtime is a pure-JVM library — this class remains unit-testable.
 *
 * Enforces:
 * - 30-second hold on "I want to disable" button
 * - Spiritual reminder during hold
 * - Companion PIN gate (if enabled)
 * - Disconnect tracking for reminder notifications
 */
class AntiTamperState(
    val holdDurationSeconds: Int = 30
) {
    private var holdStartTime: Long? = null
    private var reminderIndex: Int = 0
    private var _companionPin: String? = null

    var isHoldActive: Boolean by mutableStateOf(false)
        private set

    var holdProgress: Float by mutableFloatStateOf(0f)
        private set

    var isHoldComplete: Boolean by mutableStateOf(false)
        private set

    var requiresCompanionPin: Boolean by mutableStateOf(false)
        private set

    var disconnectCount: Int by mutableIntStateOf(0)
        private set

    val shouldShowDisconnectReminder: Boolean get() = disconnectCount >= 3

    /** Spiritual reminders shown during the hold period */
    private val reminders = listOf(
        SpiritualReminder(
            arabic = "\u0648\u064E\u0645\u064E\u0646 \u064A\u064E\u062A\u0651\u064E\u0642\u0650 \u0627\u0644\u0644\u0651\u064E\u0647\u064E \u064A\u064E\u062C\u0652\u0639\u064E\u0644 \u0644\u0651\u064E\u0647\u064F \u0645\u064E\u062E\u0652\u0631\u064E\u062C\u064B\u0627",
            translation = "Whoever fears Allah, He will make for him a way out",
            source = "Quran 65:2"
        ),
        SpiritualReminder(
            arabic = "\u0625\u0650\u0646\u0651\u064E \u0645\u064E\u0639\u064E \u0627\u0644\u0652\u0639\u064F\u0633\u0652\u0631\u0650 \u064A\u064F\u0633\u0652\u0631\u064B\u0627",
            translation = "Indeed, with hardship comes ease",
            source = "Quran 94:6"
        ),
        SpiritualReminder(
            arabic = "\u0648\u064E\u0644\u064E\u0627 \u062A\u064E\u0647\u0650\u0646\u064F\u0648\u0627 \u0648\u064E\u0644\u064E\u0627 \u062A\u064E\u062D\u0652\u0632\u064E\u0646\u064F\u0648\u0627",
            translation = "Do not weaken and do not grieve",
            source = "Quran 3:139"
        )
    )

    val spiritualReminder: SpiritualReminder?
        get() = if (isHoldActive) reminders[reminderIndex % reminders.size] else null

    fun startHold() {
        isHoldActive = true
        holdProgress = 0f
        isHoldComplete = false
        holdStartTime = System.currentTimeMillis()
    }

    fun updateHoldProgress(elapsedMillis: Long) {
        if (!isHoldActive) return
        val total = holdDurationSeconds * 1000L
        holdProgress = (elapsedMillis.toFloat() / total).coerceIn(0f, 1f)
        isHoldComplete = elapsedMillis >= total
    }

    fun releaseHold() {
        isHoldActive = false
        holdProgress = 0f
        isHoldComplete = false
        holdStartTime = null
        reminderIndex++
    }

    fun setCompanionPinRequired(pin: String) {
        _companionPin = pin
        requiresCompanionPin = pin.isNotEmpty()
    }

    fun verifyCompanionPin(pin: String): Boolean = pin == _companionPin

    fun recordDisconnect() {
        disconnectCount++
    }

    fun resetDisconnectCount() {
        disconnectCount = 0
    }
}

data class SpiritualReminder(
    val arabic: String,
    val translation: String,
    val source: String
)
