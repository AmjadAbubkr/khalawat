package com.khalawat.android.antitamper

/**
 * State machine for the anti-tamper "disable" flow.
 * Pure logic — fully unit-testable.
 *
 * Enforces:
 *   - 30-second hold on "I want to disable" button
 *   - Spiritual reminder during hold
 *   - Companion PIN gate (if enabled)
 *   - Disconnect tracking for reminder notifications
 */
class AntiTamperState(
    val holdDurationSeconds: Int = 30
) {
    private var holdStartTime: Long? = null
    private var _isHoldActive: Boolean = false
    private var _holdProgress: Float = 0f
    private var _isHoldComplete: Boolean = false
    private var _companionPin: String? = null
    private var _disconnectCount: Int = 0
    private var reminderIndex: Int = 0

    val isHoldActive: Boolean get() = _isHoldActive
    val holdProgress: Float get() = _holdProgress
    val isHoldComplete: Boolean get() = _isHoldComplete
    val requiresCompanionPin: Boolean get() = _companionPin != null
    val disconnectCount: Int get() = _disconnectCount
    val shouldShowDisconnectReminder: Boolean get() = _disconnectCount >= 3

    /** Spiritual reminders shown during the hold period */
    private val reminders = listOf(
        SpiritualReminder(
            arabic = "وَمَن يَتَّقِ اللَّهَ يَجْعَل لَّهُ مَخْرَجًا",
            translation = "Whoever fears Allah, He will make for him a way out",
            source = "Quran 65:2"
        ),
        SpiritualReminder(
            arabic = "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            translation = "Indeed, with hardship comes ease",
            source = "Quran 94:6"
        ),
        SpiritualReminder(
            arabic = "وَلَا تَهِنُوا وَلَا تَحْزَنُوا",
            translation = "Do not weaken and do not grieve",
            source = "Quran 3:139"
        )
    )

    val spiritualReminder: SpiritualReminder?
        get() = if (_isHoldActive) reminders[reminderIndex % reminders.size] else null

    fun startHold() {
        _isHoldActive = true
        _holdProgress = 0f
        _isHoldComplete = false
        holdStartTime = System.currentTimeMillis()
    }

    fun updateHoldProgress(elapsedMillis: Long) {
        if (!_isHoldActive) return
        val total = holdDurationSeconds * 1000L
        _holdProgress = (elapsedMillis.toFloat() / total).coerceIn(0f, 1f)
        _isHoldComplete = elapsedMillis >= total
    }

    fun releaseHold() {
        _isHoldActive = false
        _holdProgress = 0f
        _isHoldComplete = false
        holdStartTime = null
        reminderIndex++
    }

    fun setCompanionPinRequired(pin: String) {
        _companionPin = pin
    }

    fun verifyCompanionPin(pin: String): Boolean = pin == _companionPin

    fun recordDisconnect() {
        _disconnectCount++
    }

    fun resetDisconnectCount() {
        _disconnectCount = 0
    }
}

data class SpiritualReminder(
    val arabic: String,
    val translation: String,
    val source: String
)
