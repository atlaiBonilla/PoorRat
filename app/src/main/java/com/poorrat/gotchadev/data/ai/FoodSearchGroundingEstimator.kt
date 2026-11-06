package com.poorrat.gotchadev.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "FoodSearchEstimator"

data class EstimatedFoodResult(
    val foodName: String,
    val grams: Float,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val summaryDescription: String = ""
)

sealed interface GroundingResult {
    data class Success(val result: EstimatedFoodResult) : GroundingResult
    data object NotEdible : GroundingResult
    data class Failure(val message: String) : GroundingResult
}

object FoodSearchGroundingEstimator {

    // Ultra-lightweight, token-efficient primary model: gemini-3.5-flash-lite
    // Configured for fast raw JSON output with minimal token consumption
    private val liteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.1f
            }
        )
    }

    // Secondary fallback model: gemini-3.8-flash
    private val flashModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.1f
            }
        )
    }

    /**
     * Estimates food macros using the ultra-lightweight gemini-3.5-flash-lite model,
     * with gemini-3.8-flash as fallback.
     */
    suspend fun estimateFood(userPrompt: String): GroundingResult {
        val trimmed = userPrompt.trim()
        if (trimmed.isBlank()) {
            return GroundingResult.Failure("El texto ingresado está vacío.")
        }

        val prompt = buildNutritionPrompt(trimmed)

        // 1. Primary: Lightweight, fast execution with gemini-3.5-flash-lite
        try {
            Log.d(TAG, "Running nutrition estimation on gemini-3.5-flash-lite")
            val response = withTimeout(20_000L) {
                liteModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                val parsed = parseEstimatedFood(text)
                if (parsed is GroundingResult.Success || parsed is GroundingResult.NotEdible) {
                    return parsed
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "gemini-3.5-flash-lite attempt failed: ${e.message}, trying flashModel fallback...", e)
        }

        // 2. Fallback: gemini-3.8-flash
        return try {
            val response = withTimeout(25_000L) {
                flashModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                parseEstimatedFood(text)
            } else {
                GroundingResult.Failure("El modelo devolvió una respuesta vacía.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Fallback AI estimation failed: ${e.message}", e)
            val msg = e.message.orEmpty()
            if (msg.contains("quota", ignoreCase = true) || msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true)) {
                GroundingResult.Failure("Límite temporal alcanzado. Puedes usar la opción de Catálogo o Ingreso Manual.")
            } else {
                GroundingResult.Failure("Error al estimar con IA: ${e.localizedMessage ?: "Intenta nuevamente"}")
            }
        }
    }

    /**
     * Ultra-compact prompt to minimize input and output token consumption.
     */
    private fun buildNutritionPrompt(foodQuery: String): String {
        return """
            Estima calorías y macronutrientes para el siguiente alimento, plato, combo o bebida: "$foodQuery".
            Si NO es un alimento comestible ni bebida, responde exactamente: {"isEdibleFood":false}
            Si es comestible (incluyendo combos de comida rápida, comidas compuestas o recetas), calcula porción total en gramos o ml, calorías, proteínas, carbohidratos y grasas.
            Responde ÚNICAMENTE en JSON con este formato:
            {"isEdibleFood":true,"foodName":"Nombre descriptivo","grams":250.0,"calories":350,"protein":25.0,"carbs":30.0,"fat":10.0,"summaryDescription":"Estimado con IA"}
        """.trimIndent()
    }

    suspend fun estimateWithGoogleSearch(userPrompt: String): EstimatedFoodResult? {
        return when (val res = estimateFood(userPrompt)) {
            is GroundingResult.Success -> res.result
            else -> null
        }
    }

    internal fun parseEstimatedFood(rawText: String): GroundingResult {
        val clean = extractJson(rawText)

        try {
            val json = JSONObject(clean)
            val isEdible = json.optBoolean("isEdibleFood", true)
            if (!isEdible) {
                return GroundingResult.NotEdible
            }

            val foodName = json.optString("foodName", "").trim()
            if (foodName.isNotBlank() && !foodName.equals("null", ignoreCase = true)) {
                val grams = json.optDouble("grams", 100.0).toFloat().coerceAtLeast(1f)
                val calories = if (json.has("calories")) {
                    json.optDouble("calories", 0.0).toInt().coerceAtLeast(0)
                } else 0
                val protein = json.optDouble("protein", 0.0).toFloat().coerceAtLeast(0f)
                val carbs = json.optDouble("carbs", 0.0).toFloat().coerceAtLeast(0f)
                val fat = json.optDouble("fat", 0.0).toFloat().coerceAtLeast(0f)
                val summary = json.optString("summaryDescription", "Estimado con IA")

                return GroundingResult.Success(
                    EstimatedFoodResult(
                        foodName = foodName,
                        grams = grams,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        summaryDescription = summary
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "JSONObject parse failed, trying regex: ${e.message}")
        }

        return tryExtractWithRegex(rawText)
    }

    private fun tryExtractWithRegex(raw: String): GroundingResult {
        val isEdibleRegex = Regex(""""isEdibleFood"\s*:\s*(true|false)""", RegexOption.IGNORE_CASE)
        val isEdibleMatch = isEdibleRegex.find(raw)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        if (isEdibleMatch == false) {
            return GroundingResult.NotEdible
        }

        val nameRegex = Regex(""""foodName"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""")
        val foodName = nameRegex.find(raw)?.groupValues?.get(1)?.replace("\\\"", "\"")?.trim()

        if (foodName.isNullOrBlank()) {
            return GroundingResult.Failure("No se pudo extraer el nombre del alimento.")
        }

        val grams = Regex(""""grams"\s*:\s*([\d.]+)""").find(raw)?.groupValues?.get(1)?.toFloatOrNull() ?: 100f
        val calories = Regex(""""calories"\s*:\s*([\d.]+)""").find(raw)?.groupValues?.get(1)?.toFloatOrNull()?.toInt() ?: 0
        val protein = Regex(""""protein"\s*:\s*([\d.]+)""").find(raw)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val carbs = Regex(""""carbs"\s*:\s*([\d.]+)""").find(raw)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val fat = Regex(""""fat"\s*:\s*([\d.]+)""").find(raw)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val summary = Regex(""""summaryDescription"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""")
            .find(raw)?.groupValues?.get(1)?.replace("\\\"", "\"") ?: "Estimado con IA"

        return GroundingResult.Success(
            EstimatedFoodResult(
                foodName = foodName,
                grams = grams.coerceAtLeast(1f),
                calories = calories.coerceAtLeast(0),
                protein = protein.coerceAtLeast(0f),
                carbs = carbs.coerceAtLeast(0f),
                fat = fat.coerceAtLeast(0f),
                summaryDescription = summary
            )
        )
    }

    private fun extractJson(raw: String): String {
        var str = raw.trim()
        val firstBrace = str.indexOf('{')
        val lastBrace = str.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            str = str.substring(firstBrace, lastBrace + 1)
        }
        str = str.replace(Regex(",\\s*([}\\]])"), "$1")
        return str
    }
}
