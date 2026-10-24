package com.poorrat.gotcha.data.repository

import com.poorrat.gotcha.data.local.dao.NutritionDao
import com.poorrat.gotcha.data.local.entity.DailyFoodLog
import com.poorrat.gotcha.data.local.entity.FoodItem
import com.poorrat.gotcha.data.local.entity.UserNutritionConfig
import com.poorrat.gotcha.data.local.entity.WeightLog
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class NutritionRepository(private val dao: NutritionDao) {

    val userConfig: Flow<UserNutritionConfig?> = dao.getUserConfig()
    val allWeightLogs: Flow<List<WeightLog>> = dao.getAllWeightLogs()
    val latestWeightLog: Flow<WeightLog?> = dao.getLatestWeightLog()

    fun searchFood(query: String): Flow<List<FoodItem>> = dao.searchFood(query)

    fun getAllFoods(): Flow<List<FoodItem>> = dao.getAllFoods()

    fun getLogsByDate(dateString: String): Flow<List<DailyFoodLog>> = dao.getLogsByDate(dateString)

    fun getLogsForWeek(mondayDate: String, sundayDate: String): Flow<List<DailyFoodLog>> =
        dao.getLogsBetweenDates(mondayDate, sundayDate)

    suspend fun saveUserConfig(config: UserNutritionConfig) {
        dao.insertOrUpdateConfig(config)
    }

    suspend fun insertFood(food: FoodItem): Long {
        return dao.insertFood(food)
    }

    suspend fun insertLog(log: DailyFoodLog): Long {
        return dao.insertLog(log)
    }

    suspend fun deleteLogById(id: Long) {
        dao.deleteLogById(id)
    }

    // --- Weight Operations with Auto-Sync TDEE ---
    suspend fun addWeightLog(
        weightKg: Float,
        note: String,
        targetWeightKg: Float? = null
    ): Long {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val log = WeightLog(
            weightKg = weightKg,
            note = note,
            timestampMillis = System.currentTimeMillis(),
            dateString = todayStr
        )
        val id = dao.insertWeightLog(log)

        // Read current config and sync if needed
        val curConfig = dao.getUserConfigSync() ?: UserNutritionConfig()
        val newTarget = targetWeightKg ?: curConfig.targetWeightKg

        if (curConfig.autoSyncWeight) {
            val (bmr, tdee, dailyTarget) = calculateMifflin(
                sex = curConfig.sex,
                weightKg = weightKg,
                heightCm = curConfig.heightCm,
                age = curConfig.age,
                activity = curConfig.activityLevel,
                goal = curConfig.goalType,
                adjustment = curConfig.calorieAdjustment
            )
            val updatedConfig = curConfig.copy(
                weightKg = weightKg,
                targetWeightKg = newTarget,
                bmr = bmr,
                tdee = tdee,
                dailyCalorieTarget = dailyTarget
            )
            dao.insertOrUpdateConfig(updatedConfig)
        } else if (targetWeightKg != null) {
            dao.insertOrUpdateConfig(curConfig.copy(targetWeightKg = newTarget))
        }

        return id
    }

    suspend fun deleteWeightLog(id: Long) {
        dao.deleteWeightLogById(id)

        val curConfig = dao.getUserConfigSync() ?: UserNutritionConfig()
        if (curConfig.autoSyncWeight) {
            // Re-sync with new latest weight
            val latest = dao.getLatestWeightLogSync()
            val newWeight = latest?.weightKg ?: curConfig.weightKg
            val (bmr, tdee, dailyTarget) = calculateMifflin(
                sex = curConfig.sex,
                weightKg = newWeight,
                heightCm = curConfig.heightCm,
                age = curConfig.age,
                activity = curConfig.activityLevel,
                goal = curConfig.goalType,
                adjustment = curConfig.calorieAdjustment
            )
            dao.insertOrUpdateConfig(
                curConfig.copy(
                    weightKg = newWeight,
                    bmr = bmr,
                    tdee = tdee,
                    dailyCalorieTarget = dailyTarget
                )
            )
        }
    }

    suspend fun updateAutoSyncWeight(enabled: Boolean) {
        val curConfig = dao.getUserConfigSync() ?: UserNutritionConfig()
        if (enabled) {
            val latest = dao.getLatestWeightLogSync()
            val weightToUse = latest?.weightKg ?: curConfig.weightKg
            val (bmr, tdee, dailyTarget) = calculateMifflin(
                sex = curConfig.sex,
                weightKg = weightToUse,
                heightCm = curConfig.heightCm,
                age = curConfig.age,
                activity = curConfig.activityLevel,
                goal = curConfig.goalType,
                adjustment = curConfig.calorieAdjustment
            )
            dao.insertOrUpdateConfig(
                curConfig.copy(
                    autoSyncWeight = true,
                    weightKg = weightToUse,
                    bmr = bmr,
                    tdee = tdee,
                    dailyCalorieTarget = dailyTarget
                )
            )
        } else {
            dao.insertOrUpdateConfig(curConfig.copy(autoSyncWeight = false))
        }
    }

    suspend fun prepopulateIfEmpty() {
        if (dao.getFoodCount() == 0) {
            val defaultFoods = listOf(
                FoodItem(name = "Pechuga de pollo", caloriesPer100g = 165f, proteinPer100g = 31f, carbsPer100g = 0f, fatPer100g = 3.6f),
                FoodItem(name = "Arroz blanco", caloriesPer100g = 130f, proteinPer100g = 2.7f, carbsPer100g = 28f, fatPer100g = 0.3f),
                FoodItem(name = "Huevo entero", caloriesPer100g = 155f, proteinPer100g = 13f, carbsPer100g = 1.1f, fatPer100g = 11f),
                FoodItem(name = "Avena en hojuelas", caloriesPer100g = 389f, proteinPer100g = 16.9f, carbsPer100g = 66.3f, fatPer100g = 6.9f),
                FoodItem(name = "Plátano", caloriesPer100g = 89f, proteinPer100g = 1.1f, carbsPer100g = 22.8f, fatPer100g = 0.3f),
                FoodItem(name = "Atún en agua", caloriesPer100g = 116f, proteinPer100g = 26f, carbsPer100g = 0f, fatPer100g = 1f),
                FoodItem(name = "Manzana", caloriesPer100g = 52f, proteinPer100g = 0.3f, carbsPer100g = 14f, fatPer100g = 0.2f),
                FoodItem(name = "Pan integral", caloriesPer100g = 247f, proteinPer100g = 13f, carbsPer100g = 41f, fatPer100g = 3.4f),
                FoodItem(name = "Aceite de oliva", caloriesPer100g = 884f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 100f),
                FoodItem(name = "Proteína Whey", caloriesPer100g = 370f, proteinPer100g = 75f, carbsPer100g = 8f, fatPer100g = 3.5f),
                FoodItem(name = "Salmón fresco", caloriesPer100g = 208f, proteinPer100g = 20f, carbsPer100g = 0f, fatPer100g = 13f),
                FoodItem(name = "Almendras", caloriesPer100g = 579f, proteinPer100g = 21f, carbsPer100g = 22f, fatPer100g = 50f)
            )
            dao.insertFoods(defaultFoods)
        }

        // Initialize User Nutrition Config if not present
        if (dao.getUserConfigSync() == null) {
            val initialConfig = UserNutritionConfig(
                id = 1,
                sex = "MALE",
                weightKg = 74.5f,
                targetWeightKg = 70.0f,
                autoSyncWeight = true,
                heightCm = 178,
                age = 26,
                activityLevel = "MODERATE",
                goalType = "DEFICIT",
                calorieAdjustment = 350,
                bmr = 1733,
                tdee = 2686,
                dailyCalorieTarget = 2200
            )
            dao.insertOrUpdateConfig(initialConfig)
        }

        // Seed weight logs matching template Image 7 if empty
        if (dao.getWeightLogCount() == 0) {
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()

            val seedWeights = listOf(
                WeightLog(
                    weightKg = 74.5f,
                    note = "En ayunas",
                    timestampMillis = now,
                    dateString = today.format(dateFormatter)
                ),
                WeightLog(
                    weightKg = 74.7f,
                    note = "Post cardio",
                    timestampMillis = now - dayMillis,
                    dateString = today.minusDays(1).format(dateFormatter)
                ),
                WeightLog(
                    weightKg = 74.9f,
                    note = "En ayunas",
                    timestampMillis = now - dayMillis * 3,
                    dateString = today.minusDays(3).format(dateFormatter)
                ),
                WeightLog(
                    weightKg = 75.1f,
                    note = "En ayunas",
                    timestampMillis = now - dayMillis * 5,
                    dateString = today.minusDays(5).format(dateFormatter)
                ),
                WeightLog(
                    weightKg = 75.4f,
                    note = "Inicio de semana",
                    timestampMillis = now - dayMillis * 7,
                    dateString = today.minusDays(7).format(dateFormatter)
                )
            )
            dao.insertWeightLogs(seedWeights)
        }

        // If food logs are empty, seed recent days of the current week
        if (dao.getLogCount() == 0) {
            val today = LocalDate.now()
            val monday = today.with(DayOfWeek.MONDAY)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            val sampleDays = listOf(
                monday.plusDays(0) to 2150,
                monday.plusDays(1) to 2280,
                monday.plusDays(2) to 2100,
                monday.plusDays(3) to 2320,
                monday.plusDays(4) to 1950,
                monday.plusDays(5) to 1800
            )

            val seedLogs = mutableListOf<DailyFoodLog>()
            for ((date, calories) in sampleDays) {
                if (date.isBefore(today)) {
                    seedLogs.add(
                        DailyFoodLog(
                            foodName = "Comidas del día",
                            gramsConsumed = 500f,
                            calories = calories,
                            proteinGrams = (calories * 0.28f / 4f),
                            carbsGrams = (calories * 0.45f / 4f),
                            fatGrams = (calories * 0.27f / 9f),
                            timestampMillis = System.currentTimeMillis() - (today.toEpochDay() - date.toEpochDay()) * 86400000L,
                            dateString = date.format(formatter)
                        )
                    )
                }
            }

            seedLogs.add(
                DailyFoodLog(
                    foodName = "Avena con leche y plátano",
                    gramsConsumed = 200f,
                    calories = 215,
                    proteinGrams = 14f,
                    carbsGrams = 32f,
                    fatGrams = 4f,
                    timestampMillis = System.currentTimeMillis() - 7200000L,
                    dateString = today.format(formatter)
                )
            )

            dao.insertLogs(seedLogs)
        }
    }

    companion object {
        fun calculateMifflin(
            sex: String,
            weightKg: Float,
            heightCm: Int,
            age: Int,
            activity: String,
            goal: String,
            adjustment: Int
        ): Triple<Int, Int, Int> {
            val base = (10f * weightKg) + (6.25f * heightCm) - (5f * age)
            val bmr = if (sex == "FEMALE") (base - 161f).roundToInt() else (base + 5f).roundToInt()

            val factor = when (activity) {
                "SEDENTARY" -> 1.200f
                "LIGHT" -> 1.375f
                "MODERATE" -> 1.550f
                "HIGH" -> 1.725f
                else -> 1.550f
            }

            val tdee = (bmr * factor).roundToInt()

            val target = when (goal) {
                "DEFICIT" -> maxOf(500, tdee - adjustment)
                "SURPLUS" -> tdee + adjustment
                "MAINTENANCE" -> tdee
                else -> tdee
            }

            return Triple(bmr, tdee, target)
        }
    }
}
