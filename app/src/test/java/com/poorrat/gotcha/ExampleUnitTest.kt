package com.poorrat.gotcha

import com.poorrat.gotcha.ui.nutrition.NutritionViewModel
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun estimatedFoodResult_instantiationAndProperties() {
        val result = com.poorrat.gotcha.data.ai.EstimatedFoodResult(
            foodName = "Pechuga de pollo con arroz",
            grams = 250f,
            calories = 380,
            protein = 42f,
            carbs = 35f,
            fat = 6f,
            summaryDescription = "Datos obtenidos mediante Google Search Grounding"
        )
        assertEquals("Pechuga de pollo con arroz", result.foodName)
        assertEquals(250f, result.grams, 0.01f)
        assertEquals(380, result.calories)
        assertEquals(42f, result.protein, 0.01f)
        assertEquals(35f, result.carbs, 0.01f)
        assertEquals(6f, result.fat, 0.01f)
        assertTrue(result.summaryDescription.contains("Google Search"))
    }

    @Test
    fun mifflinCalculation_returnsExpectedValues() {
        val (bmr, tdee, target) = NutritionViewModel.computeMifflinValues(
            sex = "MALE",
            weightStr = "80",
            heightStr = "180",
            ageStr = "25",
            activity = "MODERATE",
            goal = "MAINTENANCE",
            adjustmentStr = "300"
        )
        assertTrue(bmr > 1000)
        assertTrue(tdee > bmr)
        assertEquals(tdee, target)
    }
}