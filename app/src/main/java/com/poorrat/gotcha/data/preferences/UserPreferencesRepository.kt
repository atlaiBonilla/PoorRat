package com.poorrat.gotcha.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.roundToInt

enum class WeightUnit(val code: String, val label: String, val symbol: String) {
    KG("kg", "Kilogramos (kg / g)", "kg"),
    LB("lbs", "Libras (lb / oz)", "lb")
}

enum class HeightUnit(val code: String, val label: String, val symbol: String) {
    CM("cm", "Métrico (cm)", "cm"),
    FT_IN("ft", "Imperial (pies / in)", "ft")
}

data class UserPreferences(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val heightUnit: HeightUnit = HeightUnit.CM,
    val isDarkMode: Boolean = true,
    val customGeminiApiKey: String = ""
)

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("poorrat_user_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(
        UserPreferences(
            weightUnit = if (prefs.getString("weight_unit", "kg") == "lbs") WeightUnit.LB else WeightUnit.KG,
            heightUnit = if (prefs.getString("height_unit", "cm") == "ft") HeightUnit.FT_IN else HeightUnit.CM,
            isDarkMode = prefs.getBoolean("dark_mode", true),
            customGeminiApiKey = prefs.getString("gemini_api_key", "").orEmpty()
        )
    )
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    fun setWeightUnit(unit: WeightUnit) {
        prefs.edit().putString("weight_unit", unit.code).apply()
        _preferences.value = _preferences.value.copy(weightUnit = unit)
    }

    fun setHeightUnit(unit: HeightUnit) {
        prefs.edit().putString("height_unit", unit.code).apply()
        _preferences.value = _preferences.value.copy(heightUnit = unit)
    }

    fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean("dark_mode", isDark).apply()
        _preferences.value = _preferences.value.copy(isDarkMode = isDark)
    }

    fun setCustomGeminiApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        prefs.edit().putString("gemini_api_key", trimmed).apply()
        _preferences.value = _preferences.value.copy(customGeminiApiKey = trimmed)
    }

    companion object {
        private const val KG_TO_LB = 2.20462262f
        private const val GRAM_TO_OZ = 0.03527396f

        fun formatWeight(weightKg: Float, unit: WeightUnit): String {
            return when (unit) {
                WeightUnit.KG -> String.format(Locale.US, "%.1f kg", weightKg)
                WeightUnit.LB -> String.format(Locale.US, "%.1f lb", weightKg * KG_TO_LB)
            }
        }

        fun formatWeightValueOnly(weightKg: Float, unit: WeightUnit): String {
            return when (unit) {
                WeightUnit.KG -> String.format(Locale.US, "%.1f", weightKg)
                WeightUnit.LB -> String.format(Locale.US, "%.1f", weightKg * KG_TO_LB)
            }
        }

        fun formatGrams(grams: Float, unit: WeightUnit): String {
            return when (unit) {
                WeightUnit.KG -> "${grams.roundToInt()}g"
                WeightUnit.LB -> String.format(Locale.US, "%.1foz", grams * GRAM_TO_OZ)
            }
        }

        fun formatHeight(heightCm: Float, unit: HeightUnit): String {
            return when (unit) {
                HeightUnit.CM -> "${heightCm.roundToInt()} cm"
                HeightUnit.FT_IN -> {
                    val totalInches = (heightCm / 2.54f).roundToInt()
                    val feet = totalInches / 12
                    val inches = totalInches % 12
                    "$feet' $inches\""
                }
            }
        }

        fun convertInputWeightToKg(inputVal: Float, unit: WeightUnit): Float {
            return when (unit) {
                WeightUnit.KG -> inputVal
                WeightUnit.LB -> inputVal / KG_TO_LB
            }
        }

        fun convertKgToDisplayValue(weightKg: Float, unit: WeightUnit): Float {
            return when (unit) {
                WeightUnit.KG -> weightKg
                WeightUnit.LB -> weightKg * KG_TO_LB
            }
        }
    }
}
