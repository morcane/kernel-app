package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kernel_secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_api_key"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_CURRENT_MODEL = "current_model"
        private const val KEY_ACTIVE_PERSONA = "active_persona"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_TOP_P = "top_p"
        private const val KEY_TOP_K = "top_k"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_SAFETY_BLOCK_NONE = "safety_block_none"
        private const val KEY_MONET_THEME = "monet_theme"
        private const val KEY_THINKING_LEVEL = "thinking_level"
    }

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_KEY, value.trim()).apply()

    fun getEffectiveApiKey(): String {
        val custom = customApiKey
        if (custom.isNotBlank()) return custom
        return try {
            val injected = BuildConfig.GEMINI_API_KEY
            if (injected != "MY_GEMINI_API_KEY") injected else ""
        } catch (_: Exception) {
            ""
        }
    }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var currentModelId: String
        get() = prefs.getString(KEY_CURRENT_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash"
        set(value) = prefs.edit().putString(KEY_CURRENT_MODEL, value).apply()

    var activePersonaId: String
        get() = prefs.getString(KEY_ACTIVE_PERSONA, "default_core") ?: "default_core"
        set(value) = prefs.edit().putString(KEY_ACTIVE_PERSONA, value).apply()

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()

    var topP: Float
        get() = prefs.getFloat(KEY_TOP_P, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_TOP_P, value).apply()

    var topK: Int
        get() = prefs.getInt(KEY_TOP_K, 40)
        set(value) = prefs.edit().putInt(KEY_TOP_K, value).apply()

    var maxOutputTokens: Int
        get() = prefs.getInt(KEY_MAX_TOKENS, 4096)
        set(value) = prefs.edit().putInt(KEY_MAX_TOKENS, value).apply()

    var isSafetyBlockNone: Boolean
        get() = prefs.getBoolean(KEY_SAFETY_BLOCK_NONE, true)
        set(value) = prefs.edit().putBoolean(KEY_SAFETY_BLOCK_NONE, value).apply()

    var isMonetThemeEnabled: Boolean
        get() = prefs.getBoolean(KEY_MONET_THEME, false)
        set(value) = prefs.edit().putBoolean(KEY_MONET_THEME, value).apply()

    var thinkingLevel: String
        get() = prefs.getString(KEY_THINKING_LEVEL, "low") ?: "low"
        set(value) = prefs.edit().putString(KEY_THINKING_LEVEL, value).apply()

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
    }
}
