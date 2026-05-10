package com.khalawat.android

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple preferences wrapper for Khalawat app state.
 * Stores: onboarding complete, companion pin, parent message, language.
 */
class KhalawatPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("khalawat_prefs", Context.MODE_PRIVATE)

    var isOnboardingComplete: Boolean
        get() = prefs.getBoolean("onboarding_complete", false)
        set(value) = prefs.edit().putBoolean("onboarding_complete", value).apply()

    var companionPin: String?
        get() = prefs.getString("companion_pin", null)
        set(value) = prefs.edit().putString("companion_pin", value).apply()

    var parentMessage: String
        get() = prefs.getString("parent_message", "") ?: ""
        set(value) = prefs.edit().putString("parent_message", value).apply()

    var selectedLanguage: String
        get() = prefs.getString("selected_language", "EN") ?: "EN"
        set(value) = prefs.edit().putString("selected_language", value).apply()

    var isVpnActive: Boolean
        get() = prefs.getBoolean("vpn_active", false)
        set(value) = prefs.edit().putBoolean("vpn_active", value).apply()

    var disconnectCount: Int
        get() = prefs.getInt("disconnect_count", 0)
        set(value) = prefs.edit().putInt("disconnect_count", value).apply()

    fun clear() = prefs.edit().clear().apply()
}
