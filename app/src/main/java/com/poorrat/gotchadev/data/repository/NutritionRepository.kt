package com.poorrat.gotchadev.data.repository

import android.util.Log
import com.poorrat.gotchadev.data.firebase.FirebaseSyncRepository
import com.poorrat.gotchadev.data.local.dao.NutritionDao
import com.poorrat.gotchadev.data.local.entity.DailyFoodLog
import com.poorrat.gotchadev.data.local.entity.FoodItem
import com.poorrat.gotchadev.data.local.entity.UserNutritionConfig
import com.poorrat.gotchadev.data.local.entity.WeightLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val TAG = "NutritionRepo"

class NutritionRepository(
    private val dao: NutritionDao,
    private val firebaseSyncRepository: FirebaseSyncRepository? = null
) {

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
        try {
            firebaseSyncRepository?.saveNutritionConfig(config)
        } catch (_: Exception) {
            // Keep local changes if offline
        }
    }

    suspend fun insertFood(food: FoodItem): Long {
        return dao.insertFood(food)
    }

    suspend fun insertLog(log: DailyFoodLog): Long {
        val id = dao.insertLog(log)
        try {
            firebaseSyncRepository?.saveFoodLog(log.copy(id = id))
        } catch (_: Exception) {
            // Keep local
        }
        return id
    }

    suspend fun deleteLogById(id: Long) {
        dao.deleteLogById(id)
        try {
            firebaseSyncRepository?.deleteFoodLog(id)
        } catch (_: Exception) {
            // Keep local
        }
    }

    suspend fun removeSupplementLog(dateString: String, supplementId: Long, foodName: String, rawName: String) {
        dao.deleteSupplementLog(dateString, supplementId, foodName, rawName)
        // Also sync deletion with cloud
        try {
            val cloudLogs = firebaseSyncRepository?.fetchFoodLogs() ?: emptyList()
            for (cl in cloudLogs) {
                if (cl.dateString == dateString && (cl.supplementId == supplementId || cl.foodName == foodName || cl.foodName == rawName)) {
                    val logId = cl.id.toLongOrNull()
                    if (logId != null) {
                        firebaseSyncRepository?.deleteFoodLog(logId)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun deleteLogsBySupplementId(supplementId: Long) {
        dao.deleteLogsBySupplementId(supplementId)
        try {
            val cloudLogs = firebaseSyncRepository?.fetchFoodLogs() ?: emptyList()
            for (cl in cloudLogs) {
                if (cl.supplementId == supplementId) {
                    val logId = cl.id.toLongOrNull()
                    if (logId != null) {
                        firebaseSyncRepository?.deleteFoodLog(logId)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    // --- Weight Operations with Auto-Sync TDEE and Cloud Backup ---
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

        val updatedConfig = if (curConfig.autoSyncWeight) {
            val (bmr, tdee, dailyTarget) = calculateMifflin(
                sex = curConfig.sex,
                weightKg = weightKg,
                heightCm = curConfig.heightCm,
                age = curConfig.age,
                activity = curConfig.activityLevel,
                goal = curConfig.goalType,
                adjustment = curConfig.calorieAdjustment
            )
            curConfig.copy(
                weightKg = weightKg,
                targetWeightKg = newTarget,
                bmr = bmr,
                tdee = tdee,
                dailyCalorieTarget = dailyTarget
            )
        } else {
            curConfig.copy(weightKg = weightKg, targetWeightKg = newTarget)
        }
        dao.insertOrUpdateConfig(updatedConfig)

        // Cloud persistence
        try {
            firebaseSyncRepository?.saveWeightLog(log.copy(id = id))
            firebaseSyncRepository?.saveNutritionConfig(updatedConfig)
        } catch (_: Exception) {
        }

        return id
    }

    suspend fun updateTargetWeight(targetWeightKg: Float) {
        val curConfig = dao.getUserConfigSync() ?: UserNutritionConfig()
        val updatedConfig = curConfig.copy(targetWeightKg = targetWeightKg)
        dao.insertOrUpdateConfig(updatedConfig)
        try {
            firebaseSyncRepository?.saveNutritionConfig(updatedConfig)
        } catch (_: Exception) {
        }
    }

    suspend fun deleteWeightLog(id: Long) {
        dao.deleteWeightLogById(id)
        try {
            firebaseSyncRepository?.deleteWeightLog(id)
        } catch (_: Exception) {
        }

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
            val updated = curConfig.copy(
                weightKg = newWeight,
                bmr = bmr,
                tdee = tdee,
                dailyCalorieTarget = dailyTarget
            )
            dao.insertOrUpdateConfig(updated)
            try {
                firebaseSyncRepository?.saveNutritionConfig(updated)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun updateAutoSyncWeight(enabled: Boolean) {
        val curConfig = dao.getUserConfigSync() ?: UserNutritionConfig()
        val updated = if (enabled) {
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
            curConfig.copy(
                autoSyncWeight = true,
                weightKg = weightToUse,
                bmr = bmr,
                tdee = tdee,
                dailyCalorieTarget = dailyTarget
            )
        } else {
            curConfig.copy(autoSyncWeight = false)
        }
        dao.insertOrUpdateConfig(updated)
        try {
            firebaseSyncRepository?.saveNutritionConfig(updated)
        } catch (_: Exception) {
        }
    }

    /**
     * Bidirectional Cloud Synchronization:
     * Restores user's chosen calorie target, weight goal, calculation inputs,
     * daily and weekly food logs, and all historical weight logs.
     */
    suspend fun syncWithCloud() {
        val fbSync = firebaseSyncRepository ?: return
        try {
            // 1. Sync User Nutrition Goals & Profile (dailyCalorieTarget, targetWeightKg, bmr, etc.)
            val cloudProfile = fbSync.getUserProfile()
            if (cloudProfile != null && (cloudProfile.dailyCalorieTarget > 0 || cloudProfile.targetWeightKg > 0)) {
                val restoredConfig = UserNutritionConfig(
                    id = 1,
                    sex = cloudProfile.sex.ifBlank { "MALE" },
                    weightKg = cloudProfile.weightKg.toFloat(),
                    targetWeightKg = cloudProfile.targetWeightKg.toFloat(),
                    autoSyncWeight = cloudProfile.autoSyncWeight,
                    heightCm = cloudProfile.heightCm,
                    age = cloudProfile.age,
                    activityLevel = cloudProfile.activityLevel.ifBlank { "MODERATE" },
                    goalType = cloudProfile.goalType.ifBlank { "MAINTENANCE" },
                    calorieAdjustment = cloudProfile.calorieAdjustment,
                    bmr = cloudProfile.bmr,
                    tdee = cloudProfile.tdee,
                    dailyCalorieTarget = cloudProfile.dailyCalorieTarget
                )
                dao.insertOrUpdateConfig(restoredConfig)
                Log.d(TAG, "Restored UserNutritionConfig from Firestore: targetWeight=${restoredConfig.targetWeightKg}, targetCals=${restoredConfig.dailyCalorieTarget}")
            } else {
                // If cloud has empty nutrition config, push current local config
                val localConfig = dao.getUserConfigSync()
                if (localConfig != null && (localConfig.dailyCalorieTarget > 0 || localConfig.targetWeightKg > 0)) {
                    fbSync.saveNutritionConfig(localConfig)
                }
            }

            // 2. Sync Food Logs (Daily & Weekly meal consumption)
            val cloudFoodLogs = fbSync.fetchFoodLogs()
            if (cloudFoodLogs.isNotEmpty()) {
                val localLogs = dao.getAllFoodLogsSync()
                val localSet = localLogs.map { "${it.dateString}_${it.foodName}_${it.calories}" }.toSet()
                val toInsert = cloudFoodLogs.map { cfl ->
                    DailyFoodLog(
                        id = 0L,
                        foodName = cfl.foodName,
                        gramsConsumed = cfl.gramsConsumed.toFloat(),
                        calories = cfl.calories,
                        proteinGrams = cfl.proteinGrams.toFloat(),
                        carbsGrams = cfl.carbsGrams.toFloat(),
                        fatGrams = cfl.fatGrams.toFloat(),
                        timestampMillis = if (cfl.timestampMillis > 0) cfl.timestampMillis else System.currentTimeMillis(),
                        dateString = cfl.dateString,
                        supplementId = cfl.supplementId
                    )
                }.filter { !localSet.contains("${it.dateString}_${it.foodName}_${it.calories}") }

                if (toInsert.isNotEmpty()) {
                    dao.insertLogs(toInsert)
                    Log.d(TAG, "Restored ${toInsert.size} food logs from Firestore")
                }
            } else {
                val localLogs = dao.getAllFoodLogsSync()
                if (localLogs.isNotEmpty()) {
                    fbSync.syncAllFoodLogs(localLogs)
                }
            }

            // 3. Sync Weight Logs (History of weigh-ins with exact dates and notes)
            val cloudWeightLogs = fbSync.fetchWeightLogs()
            if (cloudWeightLogs.isNotEmpty()) {
                val localWeightLogs = dao.getAllWeightLogsSync()
                val localWeightSet = localWeightLogs.map { "${it.dateString}_${it.weightKg}" }.toSet()
                val toInsertWeights = cloudWeightLogs.map { cwl ->
                    WeightLog(
                        id = 0L,
                        weightKg = cwl.weightKg.toFloat(),
                        note = cwl.note,
                        timestampMillis = if (cwl.timestampMillis > 0) cwl.timestampMillis else System.currentTimeMillis(),
                        dateString = cwl.dateString
                    )
                }.filter { !localWeightSet.contains("${it.dateString}_${it.weightKg}") }

                if (toInsertWeights.isNotEmpty()) {
                    dao.insertWeightLogs(toInsertWeights)
                    Log.d(TAG, "Restored ${toInsertWeights.size} weight logs from Firestore")
                }
            } else {
                val localWeights = dao.getAllWeightLogsSync()
                if (localWeights.isNotEmpty()) {
                    fbSync.syncWeightLogs(localWeights)
                }
            }

            // 4. Sync Cloud Food Catalog (items in Firestore /food_catalog)
            val cloudCatalog = fbSync.fetchCloudFoodCatalog()
            val localCatalog = dao.getAllFoodsSync()
            if (cloudCatalog.isNotEmpty()) {
                val localByName = localCatalog.associateBy { it.name.trim().lowercase() }
                for (cloudItem in cloudCatalog) {
                    val existing = localByName[cloudItem.name.trim().lowercase()]
                    if (existing == null) {
                        dao.insertFood(cloudItem)
                    } else if (
                        existing.caloriesPer100g != cloudItem.caloriesPer100g ||
                        existing.proteinPer100g != cloudItem.proteinPer100g ||
                        existing.carbsPer100g != cloudItem.carbsPer100g ||
                        existing.fatPer100g != cloudItem.fatPer100g
                    ) {
                        dao.insertFood(cloudItem.copy(id = existing.id))
                    }
                }
                val cloudNames = cloudCatalog.map { it.name.trim().lowercase() }.toHashSet()
                val missingInCloud = localCatalog.filter { it.name.trim().lowercase() !in cloudNames }
                if (missingInCloud.isNotEmpty()) {
                    fbSync.seedCloudFoodCatalog(missingInCloud)
                }
                Log.d(TAG, "Synchronized ${cloudCatalog.size} catalog items from Firestore /food_catalog")
            } else if (localCatalog.isNotEmpty()) {
                fbSync.seedCloudFoodCatalog(localCatalog)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nutrition syncWithCloud encountered non-fatal error: ${e.message}")
        }
    }

    suspend fun prepopulateIfEmpty() {
        val defaultFoods = listOf(
                FoodItem(name = "Pechuga de pollo", caloriesPer100g = 165f, proteinPer100g = 31f, carbsPer100g = 0f, fatPer100g = 3.6f),
                FoodItem(name = "Pechuga de pollo a la plancha", caloriesPer100g = 165f, proteinPer100g = 31f, carbsPer100g = 0f, fatPer100g = 3.6f),
                FoodItem(name = "Pechuga de pollo hervida", caloriesPer100g = 151f, proteinPer100g = 29f, carbsPer100g = 0f, fatPer100g = 3.1f),
                FoodItem(name = "Pechuga de pollo empanizada", caloriesPer100g = 246f, proteinPer100g = 21.5f, carbsPer100g = 15.2f, fatPer100g = 11f),
                FoodItem(name = "Pechuga de pollo guisada salvadoreña", caloriesPer100g = 142f, proteinPer100g = 22.4f, carbsPer100g = 3.8f, fatPer100g = 4.1f),
                FoodItem(name = "Pechuga de pollo desmenuzada", caloriesPer100g = 160f, proteinPer100g = 30f, carbsPer100g = 0f, fatPer100g = 3.5f),
                FoodItem(name = "Pechuga de pollo asada al carbón", caloriesPer100g = 172f, proteinPer100g = 31.5f, carbsPer100g = 0f, fatPer100g = 4.2f),
                FoodItem(name = "Muslo de pollo sin piel cocido", caloriesPer100g = 209f, proteinPer100g = 26f, carbsPer100g = 0f, fatPer100g = 10.9f),
                FoodItem(name = "Muslo de pollo con piel asado", caloriesPer100g = 232f, proteinPer100g = 24f, carbsPer100g = 0f, fatPer100g = 14.5f),
                FoodItem(name = "Muslo de pollo frito salvadoreño", caloriesPer100g = 268f, proteinPer100g = 22.8f, carbsPer100g = 6.5f, fatPer100g = 16.8f),
                FoodItem(name = "Pierna de pollo sin piel cocida", caloriesPer100g = 172f, proteinPer100g = 27.3f, carbsPer100g = 0f, fatPer100g = 5.7f),
                FoodItem(name = "Pierna de pollo con piel horneada", caloriesPer100g = 216f, proteinPer100g = 26.1f, carbsPer100g = 0f, fatPer100g = 11.5f),
                FoodItem(name = "Pierna de pollo frita", caloriesPer100g = 245f, proteinPer100g = 24.5f, carbsPer100g = 5.4f, fatPer100g = 13.8f),
                FoodItem(name = "Pierna de pollo guisada con papas", caloriesPer100g = 138f, proteinPer100g = 14.5f, carbsPer100g = 8.2f, fatPer100g = 5.1f),
                FoodItem(name = "Alitas de pollo fritas", caloriesPer100g = 290f, proteinPer100g = 23.8f, carbsPer100g = 2.5f, fatPer100g = 20.4f),
                FoodItem(name = "Alitas de pollo asadas", caloriesPer100g = 254f, proteinPer100g = 26.5f, carbsPer100g = 0f, fatPer100g = 16f),
                FoodItem(name = "Alitas de pollo en barbacoa", caloriesPer100g = 242f, proteinPer100g = 20.5f, carbsPer100g = 11.2f, fatPer100g = 12.8f),
                FoodItem(name = "Hígado de pollo cocido", caloriesPer100g = 167f, proteinPer100g = 24.5f, carbsPer100g = 0.9f, fatPer100g = 6.5f),
                FoodItem(name = "Hígado de pollo encebollado", caloriesPer100g = 182f, proteinPer100g = 21f, carbsPer100g = 4.5f, fatPer100g = 8.8f),
                FoodItem(name = "Mollejas de pollo cocidas", caloriesPer100g = 154f, proteinPer100g = 30.4f, carbsPer100g = 0f, fatPer100g = 2.7f),
                FoodItem(name = "Mollejas de pollo guisadas", caloriesPer100g = 168f, proteinPer100g = 26.5f, carbsPer100g = 3.2f, fatPer100g = 5.2f),
                FoodItem(name = "Corazón de pollo cocido", caloriesPer100g = 185f, proteinPer100g = 26.4f, carbsPer100g = 0.1f, fatPer100g = 7.9f),
                FoodItem(name = "Patas de pollo cocidas", caloriesPer100g = 215f, proteinPer100g = 19.4f, carbsPer100g = 0.2f, fatPer100g = 14.6f),
                FoodItem(name = "Gallina india cocida", caloriesPer100g = 219f, proteinPer100g = 24.7f, carbsPer100g = 0f, fatPer100g = 12.6f),
                FoodItem(name = "Gallina india asada al carbón", caloriesPer100g = 235f, proteinPer100g = 26.2f, carbsPer100g = 0f, fatPer100g = 13.8f),
                FoodItem(name = "Gallina india guisada", caloriesPer100g = 195f, proteinPer100g = 21.5f, carbsPer100g = 3.4f, fatPer100g = 10.5f),
                FoodItem(name = "Pavo horneado estilo salvadoreño", caloriesPer100g = 189f, proteinPer100g = 28.6f, carbsPer100g = 0.5f, fatPer100g = 7.4f),
                FoodItem(name = "Pechuga de pavo sin piel", caloriesPer100g = 135f, proteinPer100g = 30.1f, carbsPer100g = 0f, fatPer100g = 1f),
                FoodItem(name = "Pavo guisado con recaudo salvadoreño", caloriesPer100g = 156f, proteinPer100g = 20.4f, carbsPer100g = 6.2f, fatPer100g = 5.5f),
                FoodItem(name = "Codorniz asada", caloriesPer100g = 227f, proteinPer100g = 25.1f, carbsPer100g = 0f, fatPer100g = 14.1f),
                FoodItem(name = "Pato asado", caloriesPer100g = 337f, proteinPer100g = 19f, carbsPer100g = 0f, fatPer100g = 28.4f),
                FoodItem(name = "Pupusa de queso (maíz)", caloriesPer100g = 238f, proteinPer100g = 9.8f, carbsPer100g = 30.5f, fatPer100g = 8.6f),
                FoodItem(name = "Pupusa de queso con loroco (maíz)", caloriesPer100g = 232f, proteinPer100g = 9.6f, carbsPer100g = 30.2f, fatPer100g = 8.2f),
                FoodItem(name = "Pupusa revuelta de chicharrón, frijol y queso (maíz)", caloriesPer100g = 256f, proteinPer100g = 11.2f, carbsPer100g = 28.4f, fatPer100g = 10.8f),
                FoodItem(name = "Pupusa de frijol con queso (maíz)", caloriesPer100g = 224f, proteinPer100g = 9.2f, carbsPer100g = 32.6f, fatPer100g = 6.4f),
                FoodItem(name = "Pupusa de frijoles (maíz)", caloriesPer100g = 202f, proteinPer100g = 7.4f, carbsPer100g = 35f, fatPer100g = 3.8f),
                FoodItem(name = "Pupusa de chicharrón (maíz)", caloriesPer100g = 272f, proteinPer100g = 12.5f, carbsPer100g = 26.8f, fatPer100g = 12.8f),
                FoodItem(name = "Pupusa de chicharrón con queso (maíz)", caloriesPer100g = 265f, proteinPer100g = 11.8f, carbsPer100g = 27.5f, fatPer100g = 11.9f),
                FoodItem(name = "Pupusa de ayote con queso (maíz)", caloriesPer100g = 215f, proteinPer100g = 8.5f, carbsPer100g = 29.8f, fatPer100g = 6.9f),
                FoodItem(name = "Pupusa de chipilín con queso (maíz)", caloriesPer100g = 230f, proteinPer100g = 9.7f, carbsPer100g = 30f, fatPer100g = 8f),
                FoodItem(name = "Pupusa de mora con queso (maíz)", caloriesPer100g = 228f, proteinPer100g = 9.6f, carbsPer100g = 29.9f, fatPer100g = 7.9f),
                FoodItem(name = "Pupusa de ajo con queso (maíz)", caloriesPer100g = 240f, proteinPer100g = 9.8f, carbsPer100g = 30.8f, fatPer100g = 8.7f),
                FoodItem(name = "Pupusa de jalapeño con queso (maíz)", caloriesPer100g = 234f, proteinPer100g = 9.5f, carbsPer100g = 30.4f, fatPer100g = 8.3f),
                FoodItem(name = "Pupusa de pollo con queso (maíz)", caloriesPer100g = 242f, proteinPer100g = 13.2f, carbsPer100g = 28f, fatPer100g = 8.5f),
                FoodItem(name = "Pupusa de camarón con queso (maíz)", caloriesPer100g = 236f, proteinPer100g = 12.8f, carbsPer100g = 28.5f, fatPer100g = 7.9f),
                FoodItem(name = "Pupusa de pescado con queso (maíz)", caloriesPer100g = 231f, proteinPer100g = 12.4f, carbsPer100g = 28.2f, fatPer100g = 7.6f),
                FoodItem(name = "Pupusa de espinaca con queso (maíz)", caloriesPer100g = 226f, proteinPer100g = 9.4f, carbsPer100g = 29.6f, fatPer100g = 7.8f),
                FoodItem(name = "Pupusa de zanahoria con queso (maíz)", caloriesPer100g = 222f, proteinPer100g = 8.8f, carbsPer100g = 30.8f, fatPer100g = 7.1f),
                FoodItem(name = "Pupusa de papelillo con queso (maíz)", caloriesPer100g = 229f, proteinPer100g = 9.5f, carbsPer100g = 30.1f, fatPer100g = 7.9f),
                FoodItem(name = "Pupusa de cochinito con queso (maíz)", caloriesPer100g = 268f, proteinPer100g = 12f, carbsPer100g = 27.2f, fatPer100g = 12.4f),
                FoodItem(name = "Pupusa de chorizo con queso (maíz)", caloriesPer100g = 274f, proteinPer100g = 11.6f, carbsPer100g = 27f, fatPer100g = 13.2f),
                FoodItem(name = "Pupusa de jamón con queso (maíz)", caloriesPer100g = 245f, proteinPer100g = 11.4f, carbsPer100g = 29f, fatPer100g = 9.2f),
                FoodItem(name = "Pupusa de tocino con queso (maíz)", caloriesPer100g = 282f, proteinPer100g = 11.8f, carbsPer100g = 26.8f, fatPer100g = 14.2f),
                FoodItem(name = "Pupusa de hongos con queso (maíz)", caloriesPer100g = 225f, proteinPer100g = 9.3f, carbsPer100g = 29.7f, fatPer100g = 7.7f),
                FoodItem(name = "Pupusa de flor de izote con queso (maíz)", caloriesPer100g = 227f, proteinPer100g = 9.4f, carbsPer100g = 29.8f, fatPer100g = 7.8f),
                FoodItem(name = "Pupusa de pacaya con queso (maíz)", caloriesPer100g = 229f, proteinPer100g = 9.5f, carbsPer100g = 29.9f, fatPer100g = 7.9f),
                FoodItem(name = "Pupusa loca (maíz)", caloriesPer100g = 262f, proteinPer100g = 12.4f, carbsPer100g = 27.5f, fatPer100g = 11.4f),
                FoodItem(name = "Pupusa de queso (arroz)", caloriesPer100g = 252f, proteinPer100g = 9.4f, carbsPer100g = 33.8f, fatPer100g = 8.9f),
                FoodItem(name = "Pupusa de queso con loroco (arroz)", caloriesPer100g = 246f, proteinPer100g = 9.2f, carbsPer100g = 33.4f, fatPer100g = 8.5f),
                FoodItem(name = "Pupusa revuelta (arroz)", caloriesPer100g = 268f, proteinPer100g = 10.9f, carbsPer100g = 31.2f, fatPer100g = 11.1f),
                FoodItem(name = "Pupusa de frijol con queso (arroz)", caloriesPer100g = 238f, proteinPer100g = 8.9f, carbsPer100g = 35.4f, fatPer100g = 6.8f),
                FoodItem(name = "Pupusa de chicharrón con queso (arroz)", caloriesPer100g = 278f, proteinPer100g = 11.5f, carbsPer100g = 30.2f, fatPer100g = 12.4f),
                FoodItem(name = "Pupusa de ayote con queso (arroz)", caloriesPer100g = 228f, proteinPer100g = 8.2f, carbsPer100g = 32.8f, fatPer100g = 7.2f),
                FoodItem(name = "Pupusa de chipilín con queso (arroz)", caloriesPer100g = 244f, proteinPer100g = 9.3f, carbsPer100g = 33.2f, fatPer100g = 8.3f),
                FoodItem(name = "Pupusa de mora con queso (arroz)", caloriesPer100g = 242f, proteinPer100g = 9.2f, carbsPer100g = 33.1f, fatPer100g = 8.2f),
                FoodItem(name = "Pupusa de ajo con queso (arroz)", caloriesPer100g = 254f, proteinPer100g = 9.4f, carbsPer100g = 34f, fatPer100g = 9f),
                FoodItem(name = "Pupusa de jalapeño con queso (arroz)", caloriesPer100g = 248f, proteinPer100g = 9.1f, carbsPer100g = 33.6f, fatPer100g = 8.6f),
                FoodItem(name = "Pupusa de pollo con queso (arroz)", caloriesPer100g = 255f, proteinPer100g = 12.8f, carbsPer100g = 31f, fatPer100g = 8.8f),
                FoodItem(name = "Pupusa de camarón con queso (arroz)", caloriesPer100g = 249f, proteinPer100g = 12.4f, carbsPer100g = 31.5f, fatPer100g = 8.2f),
                FoodItem(name = "Pupusa loca (arroz)", caloriesPer100g = 275f, proteinPer100g = 12.1f, carbsPer100g = 30.5f, fatPer100g = 11.8f),
                FoodItem(name = "Curtido de repollo tradicional (en vinagre)", caloriesPer100g = 28f, proteinPer100g = 1.1f, carbsPer100g = 5.8f, fatPer100g = 0.2f),
                FoodItem(name = "Curtido de repollo con mayonesa", caloriesPer100g = 118f, proteinPer100g = 1.4f, carbsPer100g = 6.5f, fatPer100g = 9.8f),
                FoodItem(name = "Curtido de cebolla morada con chile", caloriesPer100g = 34f, proteinPer100g = 0.9f, carbsPer100g = 7.6f, fatPer100g = 0.1f),
                FoodItem(name = "Salsa de tomate salvadoreña para pupusas", caloriesPer100g = 36f, proteinPer100g = 1.2f, carbsPer100g = 6.4f, fatPer100g = 0.7f),
                FoodItem(name = "Chile picante criollo en escabeche", caloriesPer100g = 32f, proteinPer100g = 1f, carbsPer100g = 6.2f, fatPer100g = 0.4f),
                FoodItem(name = "Tamal de pollo salvadoreño", caloriesPer100g = 198f, proteinPer100g = 7.8f, carbsPer100g = 22.5f, fatPer100g = 8.6f),
                FoodItem(name = "Tamal de gallina salvadoreño", caloriesPer100g = 206f, proteinPer100g = 8.2f, carbsPer100g = 22f, fatPer100g = 9.4f),
                FoodItem(name = "Tamal de cerdo salvadoreño", caloriesPer100g = 218f, proteinPer100g = 7.6f, carbsPer100g = 21.8f, fatPer100g = 11.2f),
                FoodItem(name = "Tamal de pato salvadoreño", caloriesPer100g = 224f, proteinPer100g = 7.9f, carbsPer100g = 21.5f, fatPer100g = 11.8f),
                FoodItem(name = "Tamal pisque (con frijoles)", caloriesPer100g = 186f, proteinPer100g = 5.8f, carbsPer100g = 28.4f, fatPer100g = 5.4f),
                FoodItem(name = "Tamal pisque con queso", caloriesPer100g = 198f, proteinPer100g = 6.9f, carbsPer100g = 27.6f, fatPer100g = 6.8f),
                FoodItem(name = "Tamal de elote cocido", caloriesPer100g = 174f, proteinPer100g = 4.2f, carbsPer100g = 30.5f, fatPer100g = 4.1f),
                FoodItem(name = "Tamal de elote frito", caloriesPer100g = 232f, proteinPer100g = 4.5f, carbsPer100g = 31.2f, fatPer100g = 10.2f),
                FoodItem(name = "Tamal de chipilín con queso", caloriesPer100g = 192f, proteinPer100g = 6.4f, carbsPer100g = 26.8f, fatPer100g = 6.6f),
                FoodItem(name = "Tamal de azúcar (dulce con pasas)", caloriesPer100g = 215f, proteinPer100g = 3.6f, carbsPer100g = 36.5f, fatPer100g = 6.2f),
                FoodItem(name = "Montuca de elote con pollo", caloriesPer100g = 204f, proteinPer100g = 7.5f, carbsPer100g = 27.8f, fatPer100g = 7.1f),
                FoodItem(name = "Ticuco viajero con frijoles y chipilín", caloriesPer100g = 195f, proteinPer100g = 6.5f, carbsPer100g = 31f, fatPer100g = 5.1f),
                FoodItem(name = "Yuca sancochada", caloriesPer100g = 159f, proteinPer100g = 1.4f, carbsPer100g = 38f, fatPer100g = 0.3f),
                FoodItem(name = "Yuca frita", caloriesPer100g = 265f, proteinPer100g = 1.6f, carbsPer100g = 40.5f, fatPer100g = 10.8f),
                FoodItem(name = "Yuca frita con chicharrón y curtido", caloriesPer100g = 248f, proteinPer100g = 8.5f, carbsPer100g = 28.4f, fatPer100g = 11.2f),
                FoodItem(name = "Yuca sancochada con pepesca y curtido", caloriesPer100g = 168f, proteinPer100g = 7.2f, carbsPer100g = 27.5f, fatPer100g = 3.2f),
                FoodItem(name = "Yuca con merienda de cerdo", caloriesPer100g = 236f, proteinPer100g = 9.4f, carbsPer100g = 26.8f, fatPer100g = 10.2f),
                FoodItem(name = "Pastelitos de maíz rellenos de carne y papa", caloriesPer100g = 275f, proteinPer100g = 7.8f, carbsPer100g = 31.5f, fatPer100g = 13.2f),
                FoodItem(name = "Pastelitos de maíz rellenos de pollo", caloriesPer100g = 262f, proteinPer100g = 8.4f, carbsPer100g = 31.8f, fatPer100g = 11.4f),
                FoodItem(name = "Pastelitos de maíz rellenos de verduras", caloriesPer100g = 245f, proteinPer100g = 4.5f, carbsPer100g = 33.6f, fatPer100g = 10.4f),
                FoodItem(name = "Enchilada salvadoreña completa (con frijol, huevo y queso)", caloriesPer100g = 268f, proteinPer100g = 9.6f, carbsPer100g = 29.4f, fatPer100g = 12.5f),
                FoodItem(name = "Tostada de maíz para enchilada salvadoreña", caloriesPer100g = 465f, proteinPer100g = 7.2f, carbsPer100g = 62.5f, fatPer100g = 20.8f),
                FoodItem(name = "Nuégados de yuca fritos", caloriesPer100g = 295f, proteinPer100g = 4.8f, carbsPer100g = 41.2f, fatPer100g = 12.4f),
                FoodItem(name = "Nuégados de yuca con miel de panela", caloriesPer100g = 318f, proteinPer100g = 4.1f, carbsPer100g = 52.6f, fatPer100g = 10.2f),
                FoodItem(name = "Nuégados de masa de maíz", caloriesPer100g = 310f, proteinPer100g = 5.2f, carbsPer100g = 45f, fatPer100g = 12.2f),
                FoodItem(name = "Nuégados de huevo", caloriesPer100g = 342f, proteinPer100g = 7.8f, carbsPer100g = 42.5f, fatPer100g = 15.6f),
                FoodItem(name = "Buñuelos salvadoreños con miel", caloriesPer100g = 308f, proteinPer100g = 4.6f, carbsPer100g = 49.8f, fatPer100g = 10.1f),
                FoodItem(name = "Empanada de plátano rellena de leche poleada", caloriesPer100g = 228f, proteinPer100g = 3.4f, carbsPer100g = 38.5f, fatPer100g = 6.8f),
                FoodItem(name = "Empanada de plátano rellena de frijol", caloriesPer100g = 218f, proteinPer100g = 4.6f, carbsPer100g = 36.8f, fatPer100g = 6.1f),
                FoodItem(name = "Riguas de elote tierno", caloriesPer100g = 182f, proteinPer100g = 4.8f, carbsPer100g = 31.4f, fatPer100g = 4.2f),
                FoodItem(name = "Riguas de elote con queso y crema", caloriesPer100g = 216f, proteinPer100g = 6.8f, carbsPer100g = 28.5f, fatPer100g = 8.4f),
                FoodItem(name = "Elote loco salvadoreño (con mayonesa, salsa, mostaza y queso)", caloriesPer100g = 192f, proteinPer100g = 5.4f, carbsPer100g = 23.6f, fatPer100g = 8.6f),
                FoodItem(name = "Elote sancochado con limón y sal", caloriesPer100g = 96f, proteinPer100g = 3.4f, carbsPer100g = 21f, fatPer100g = 1.5f),
                FoodItem(name = "Elote asado al carbón", caloriesPer100g = 112f, proteinPer100g = 3.8f, carbsPer100g = 24.2f, fatPer100g = 1.6f),
                FoodItem(name = "Esquite / Coctel de elote salvadoreño", caloriesPer100g = 168f, proteinPer100g = 4.6f, carbsPer100g = 21.5f, fatPer100g = 7.2f),
                FoodItem(name = "Plátano frito en tajadas", caloriesPer100g = 252f, proteinPer100g = 1.5f, carbsPer100g = 42f, fatPer100g = 9.2f),
                FoodItem(name = "Plátano sancochado", caloriesPer100g = 116f, proteinPer100g = 1f, carbsPer100g = 31.2f, fatPer100g = 0.2f),
                FoodItem(name = "Plátano horneado maduro", caloriesPer100g = 138f, proteinPer100g = 1.2f, carbsPer100g = 35.8f, fatPer100g = 0.3f),
                FoodItem(name = "Canoas de plátano con leche poleada y pasas", caloriesPer100g = 214f, proteinPer100g = 3.2f, carbsPer100g = 39.4f, fatPer100g = 5.1f),
                FoodItem(name = "Chilaquiles salvadoreños (tortilla rellena de queso en salsa)", caloriesPer100g = 188f, proteinPer100g = 8.4f, carbsPer100g = 20.6f, fatPer100g = 8.1f),
                FoodItem(name = "Rellenos de güisquil con queso", caloriesPer100g = 154f, proteinPer100g = 6.8f, carbsPer100g = 14.2f, fatPer100g = 7.8f),
                FoodItem(name = "Rellenos de pacaya con queso", caloriesPer100g = 162f, proteinPer100g = 7.4f, carbsPer100g = 13.5f, fatPer100g = 8.8f),
                FoodItem(name = "Rellenos de coliflor", caloriesPer100g = 148f, proteinPer100g = 6.5f, carbsPer100g = 12.8f, fatPer100g = 7.9f),
                FoodItem(name = "Rellenos de ejotes con queso", caloriesPer100g = 156f, proteinPer100g = 7.1f, carbsPer100g = 13.4f, fatPer100g = 8.2f),
                FoodItem(name = "Rellenos de papa con queso", caloriesPer100g = 178f, proteinPer100g = 6.9f, carbsPer100g = 20.4f, fatPer100g = 7.6f),
                FoodItem(name = "Rellenos de chile verde con carne y verduras", caloriesPer100g = 165f, proteinPer100g = 9.2f, carbsPer100g = 12.6f, fatPer100g = 8.6f),
                FoodItem(name = "Rellenos de chile verde con queso", caloriesPer100g = 172f, proteinPer100g = 8.8f, carbsPer100g = 11.4f, fatPer100g = 10.1f),
                FoodItem(name = "Rellenos de pescado seco (tortas de pescado de Semana Santa)", caloriesPer100g = 212f, proteinPer100g = 15.8f, carbsPer100g = 16.4f, fatPer100g = 9.2f),
                FoodItem(name = "Pacayas envueltas en huevo con salsa", caloriesPer100g = 136f, proteinPer100g = 6.2f, carbsPer100g = 10.8f, fatPer100g = 7.6f),
                FoodItem(name = "Flor de izote envuelta en huevo", caloriesPer100g = 128f, proteinPer100g = 5.8f, carbsPer100g = 10.2f, fatPer100g = 7.2f),
                FoodItem(name = "Tortitas de elote fritas", caloriesPer100g = 224f, proteinPer100g = 4.9f, carbsPer100g = 34.2f, fatPer100g = 7.6f),
                FoodItem(name = "Torrejas salvadoreñas en miel de panela", caloriesPer100g = 276f, proteinPer100g = 6.2f, carbsPer100g = 44.8f, fatPer100g = 8.2f),
                FoodItem(name = "Jocotes en miel de dulce de atado", caloriesPer100g = 194f, proteinPer100g = 0.8f, carbsPer100g = 48.5f, fatPer100g = 0.3f),
                FoodItem(name = "Mangos en miel de panela", caloriesPer100g = 186f, proteinPer100g = 0.6f, carbsPer100g = 46.8f, fatPer100g = 0.2f),
                FoodItem(name = "Ayote en miel de panela", caloriesPer100g = 168f, proteinPer100g = 0.9f, carbsPer100g = 41.5f, fatPer100g = 0.2f),
                FoodItem(name = "Camote en miel de panela", caloriesPer100g = 198f, proteinPer100g = 1.2f, carbsPer100g = 48.2f, fatPer100g = 0.3f),
                FoodItem(name = "Plátanos en gloria (en miel de panela y canela)", caloriesPer100g = 204f, proteinPer100g = 1.1f, carbsPer100g = 47.6f, fatPer100g = 1.4f),
                FoodItem(name = "Conserva de coco con dulce de panela", caloriesPer100g = 385f, proteinPer100g = 3.2f, carbsPer100g = 62.4f, fatPer100g = 14.6f),
                FoodItem(name = "Dulce de leche salvadoreño", caloriesPer100g = 315f, proteinPer100g = 6.8f, carbsPer100g = 55.4f, fatPer100g = 7.4f),
                FoodItem(name = "Tartaleta de leche", caloriesPer100g = 358f, proteinPer100g = 6.4f, carbsPer100g = 54.2f, fatPer100g = 13.2f),
                FoodItem(name = "Alboroto de maicillo con miel", caloriesPer100g = 372f, proteinPer100g = 6.8f, carbsPer100g = 78.4f, fatPer100g = 3.8f),
                FoodItem(name = "Peperecha salvadoreña", caloriesPer100g = 365f, proteinPer100g = 5.4f, carbsPer100g = 66.8f, fatPer100g = 8.4f),
                FoodItem(name = "Sopa de pata salvadoreña", caloriesPer100g = 118f, proteinPer100g = 8.6f, carbsPer100g = 10.4f, fatPer100g = 4.6f),
                FoodItem(name = "Sopa de gallina india", caloriesPer100g = 96f, proteinPer100g = 8.8f, carbsPer100g = 6.5f, fatPer100g = 3.8f),
                FoodItem(name = "Sopa de res salvadoreña con verdura", caloriesPer100g = 88f, proteinPer100g = 7.4f, carbsPer100g = 7.8f, fatPer100g = 3.1f),
                FoodItem(name = "Sopa de frijoles rojos con hueso de cerdo", caloriesPer100g = 112f, proteinPer100g = 6.8f, carbsPer100g = 13.6f, fatPer100g = 3.4f),
                FoodItem(name = "Sopa de frijoles blancos con costilla de cerdo", caloriesPer100g = 118f, proteinPer100g = 7.2f, carbsPer100g = 14.1f, fatPer100g = 3.7f),
                FoodItem(name = "Sopa de frijoles nuevos con crema", caloriesPer100g = 104f, proteinPer100g = 5.6f, carbsPer100g = 14.2f, fatPer100g = 2.8f),
                FoodItem(name = "Sopa de chipilín con huevos y masa", caloriesPer100g = 84f, proteinPer100g = 5.2f, carbsPer100g = 9.8f, fatPer100g = 2.7f),
                FoodItem(name = "Sopa de mora con huevos", caloriesPer100g = 76f, proteinPer100g = 4.9f, carbsPer100g = 8.4f, fatPer100g = 2.6f),
                FoodItem(name = "Sopa de tortilla salvadoreña", caloriesPer100g = 114f, proteinPer100g = 4.8f, carbsPer100g = 13.5f, fatPer100g = 4.6f),
                FoodItem(name = "Mariscada salvadoreña con crema", caloriesPer100g = 126f, proteinPer100g = 11.4f, carbsPer100g = 5.8f, fatPer100g = 6.4f),
                FoodItem(name = "Mariscada salvadoreña en caldo rojo", caloriesPer100g = 94f, proteinPer100g = 11.2f, carbsPer100g = 4.6f, fatPer100g = 3.4f),
                FoodItem(name = "Sopa de pescado salvadoreña", caloriesPer100g = 86f, proteinPer100g = 10.4f, carbsPer100g = 4.8f, fatPer100g = 2.8f),
                FoodItem(name = "Sopa de camarones con leche de coco", caloriesPer100g = 118f, proteinPer100g = 10.2f, carbsPer100g = 5.4f, fatPer100g = 6.2f),
                FoodItem(name = "Sopa de jaiba", caloriesPer100g = 82f, proteinPer100g = 9.6f, carbsPer100g = 4.5f, fatPer100g = 2.8f),
                FoodItem(name = "Sopa de punche", caloriesPer100g = 78f, proteinPer100g = 9.2f, carbsPer100g = 4.2f, fatPer100g = 2.6f),
                FoodItem(name = "Gallo en chicha salvadoreño", caloriesPer100g = 168f, proteinPer100g = 18.4f, carbsPer100g = 9.6f, fatPer100g = 6.2f),
                FoodItem(name = "Carne guisada salvadoreña con papas y zanahoria", caloriesPer100g = 146f, proteinPer100g = 15.8f, carbsPer100g = 8.4f, fatPer100g = 5.4f),
                FoodItem(name = "Hilachas salvadoreñas de res", caloriesPer100g = 138f, proteinPer100g = 16.2f, carbsPer100g = 7.6f, fatPer100g = 4.8f),
                FoodItem(name = "Salpicón de res salvadoreño (con hierbabuena y rábano)", caloriesPer100g = 142f, proteinPer100g = 21.8f, carbsPer100g = 3.2f, fatPer100g = 4.6f),
                FoodItem(name = "Chompipe navideño en salsa criolla", caloriesPer100g = 162f, proteinPer100g = 21.4f, carbsPer100g = 6.8f, fatPer100g = 5.4f),
                FoodItem(name = "Pan con pollo salvadoreño completo", caloriesPer100g = 218f, proteinPer100g = 13.6f, carbsPer100g = 22.8f, fatPer100g = 8f),
                FoodItem(name = "Pan con pavo salvadoreño completo", caloriesPer100g = 212f, proteinPer100g = 14.2f, carbsPer100g = 22.5f, fatPer100g = 7.2f),
                FoodItem(name = "Pan con pierna de cerdo horneada", caloriesPer100g = 234f, proteinPer100g = 13.8f, carbsPer100g = 22.4f, fatPer100g = 9.8f),
                FoodItem(name = "Pan mata niños (pan con salchicha, escabeche y salsa)", caloriesPer100g = 246f, proteinPer100g = 8.9f, carbsPer100g = 28.4f, fatPer100g = 10.8f),
                FoodItem(name = "Carne asada salvadoreña (lomo de aguja)", caloriesPer100g = 214f, proteinPer100g = 26.8f, carbsPer100g = 0f, fatPer100g = 11.4f),
                FoodItem(name = "Costilla de cerdo en salsa barbacoa", caloriesPer100g = 268f, proteinPer100g = 21.5f, carbsPer100g = 9.4f, fatPer100g = 16.2f),
                FoodItem(name = "Lomo relleno salvadoreño", caloriesPer100g = 228f, proteinPer100g = 22.4f, carbsPer100g = 5.8f, fatPer100g = 12.8f),
                FoodItem(name = "Bistec encebollado salvadoreño", caloriesPer100g = 184f, proteinPer100g = 22.6f, carbsPer100g = 4.8f, fatPer100g = 8.2f),
                FoodItem(name = "Hígado de res encebollado", caloriesPer100g = 176f, proteinPer100g = 22.8f, carbsPer100g = 5.4f, fatPer100g = 6.8f),
                FoodItem(name = "Lengua de res en salsa", caloriesPer100g = 196f, proteinPer100g = 17.8f, carbsPer100g = 4.6f, fatPer100g = 11.8f),
                FoodItem(name = "Panza de res en salsa (mondongo)", caloriesPer100g = 134f, proteinPer100g = 14.6f, carbsPer100g = 5.2f, fatPer100g = 6.1f),
                FoodItem(name = "Chicharrones de cerdo salvadoreños", caloriesPer100g = 512f, proteinPer100g = 32.4f, carbsPer100g = 0.5f, fatPer100g = 42f),
                FoodItem(name = "Fritada salvadoreña de cerdo", caloriesPer100g = 242f, proteinPer100g = 19.6f, carbsPer100g = 4.8f, fatPer100g = 16.2f),
                FoodItem(name = "Chorizo salvadoreño de cerdo asado", caloriesPer100g = 328f, proteinPer100g = 18.4f, carbsPer100g = 2.4f, fatPer100g = 27.2f),
                FoodItem(name = "Chorizo de Cojutepeque", caloriesPer100g = 342f, proteinPer100g = 19.2f, carbsPer100g = 2.8f, fatPer100g = 28.4f),
                FoodItem(name = "Longaniza salvadoreña", caloriesPer100g = 316f, proteinPer100g = 17.8f, carbsPer100g = 2.6f, fatPer100g = 26f),
                FoodItem(name = "Butifarra de Cojutepeque", caloriesPer100g = 286f, proteinPer100g = 18.6f, carbsPer100g = 3.2f, fatPer100g = 22.1f),
                FoodItem(name = "Moronga / Morcilla salvadoreña", caloriesPer100g = 258f, proteinPer100g = 14.8f, carbsPer100g = 6.4f, fatPer100g = 19.2f),
                FoodItem(name = "Chorizo con huevo revuelto", caloriesPer100g = 248f, proteinPer100g = 15.2f, carbsPer100g = 2.1f, fatPer100g = 19.8f),
                FoodItem(name = "Casamiento salvadoreño (arroz y frijoles rojos)", caloriesPer100g = 142f, proteinPer100g = 5.4f, carbsPer100g = 25.8f, fatPer100g = 2.1f),
                FoodItem(name = "Casamiento frito con aceite y cebolla", caloriesPer100g = 168f, proteinPer100g = 5.4f, carbsPer100g = 25.6f, fatPer100g = 4.9f),
                FoodItem(name = "Frijoles rojos de seda sancochados (en bala)", caloriesPer100g = 127f, proteinPer100g = 8.7f, carbsPer100g = 22.8f, fatPer100g = 0.5f),
                FoodItem(name = "Frijoles rojos volteados (molidos y fritos)", caloriesPer100g = 162f, proteinPer100g = 7.8f, carbsPer100g = 21.4f, fatPer100g = 5.2f),
                FoodItem(name = "Frijoles rojos licuados", caloriesPer100g = 136f, proteinPer100g = 7.4f, carbsPer100g = 20.6f, fatPer100g = 2.8f),
                FoodItem(name = "Frijoles blancos cocidos", caloriesPer100g = 139f, proteinPer100g = 9.7f, carbsPer100g = 25.1f, fatPer100g = 0.5f),
                FoodItem(name = "Frijoles negros cocidos", caloriesPer100g = 132f, proteinPer100g = 8.9f, carbsPer100g = 23.7f, fatPer100g = 0.5f),
                FoodItem(name = "Frijoles negros refritos", caloriesPer100g = 158f, proteinPer100g = 8.1f, carbsPer100g = 21.8f, fatPer100g = 4.6f),
                FoodItem(name = "Sopa de frijoles rojos (solo caldo)", caloriesPer100g = 42f, proteinPer100g = 2.6f, carbsPer100g = 7.2f, fatPer100g = 0.4f),
                FoodItem(name = "Arroz blanco cocido estilo salvadoreño (con zanahoria y chile)", caloriesPer100g = 138f, proteinPer100g = 2.7f, carbsPer100g = 28.4f, fatPer100g = 1.6f),
                FoodItem(name = "Arroz blanco hervido sin grasa", caloriesPer100g = 130f, proteinPer100g = 2.7f, carbsPer100g = 28.2f, fatPer100g = 0.3f),
                FoodItem(name = "Arroz aguado con pollo", caloriesPer100g = 124f, proteinPer100g = 7.6f, carbsPer100g = 17.8f, fatPer100g = 2.6f),
                FoodItem(name = "Arroz aguado con cerdo", caloriesPer100g = 136f, proteinPer100g = 7.4f, carbsPer100g = 17.5f, fatPer100g = 4.1f),
                FoodItem(name = "Arroz con verduras y elote", caloriesPer100g = 134f, proteinPer100g = 2.9f, carbsPer100g = 26.8f, fatPer100g = 1.8f),
                FoodItem(name = "Arroz negrito (cocido en caldo de frijol)", caloriesPer100g = 144f, proteinPer100g = 3.6f, carbsPer100g = 28.6f, fatPer100g = 1.8f),
                FoodItem(name = "Arroz cantonés estilo salvadoreño", caloriesPer100g = 178f, proteinPer100g = 7.8f, carbsPer100g = 25.4f, fatPer100g = 5.1f),
                FoodItem(name = "Arroz en leche salvadoreño con canela", caloriesPer100g = 154f, proteinPer100g = 4.1f, carbsPer100g = 27.6f, fatPer100g = 3.1f),
                FoodItem(name = "Tortilla de maíz salvadoreña (gruesa a mano)", caloriesPer100g = 218f, proteinPer100g = 5.7f, carbsPer100g = 44.6f, fatPer100g = 2.5f),
                FoodItem(name = "Tortilla de maíz tostada al comal", caloriesPer100g = 242f, proteinPer100g = 6.2f, carbsPer100g = 49.4f, fatPer100g = 2.8f),
                FoodItem(name = "Tortilla de maíz frita", caloriesPer100g = 336f, proteinPer100g = 5.8f, carbsPer100g = 46.2f, fatPer100g = 14.2f),
                FoodItem(name = "Tortilla de maicillo (sorgo)", caloriesPer100g = 212f, proteinPer100g = 5.9f, carbsPer100g = 44f, fatPer100g = 2.1f),
                FoodItem(name = "Masa de maíz nixtamalizada cruda", caloriesPer100g = 192f, proteinPer100g = 4.8f, carbsPer100g = 40.2f, fatPer100g = 2.1f),
                FoodItem(name = "Harina de maíz nixtamalizada (seca)", caloriesPer100g = 365f, proteinPer100g = 9.3f, carbsPer100g = 76.3f, fatPer100g = 4.2f),
                FoodItem(name = "Harina de arroz", caloriesPer100g = 366f, proteinPer100g = 6f, carbsPer100g = 80.1f, fatPer100g = 1.4f),
                FoodItem(name = "Huevo de gallina entero crudo", caloriesPer100g = 143f, proteinPer100g = 12.6f, carbsPer100g = 0.7f, fatPer100g = 9.5f),
                FoodItem(name = "Huevo duro / sancochado", caloriesPer100g = 155f, proteinPer100g = 12.6f, carbsPer100g = 1.1f, fatPer100g = 10.6f),
                FoodItem(name = "Huevo picado con tomate y cebolla", caloriesPer100g = 158f, proteinPer100g = 10.8f, carbsPer100g = 3.2f, fatPer100g = 11.4f),
                FoodItem(name = "Huevo picado con loroco", caloriesPer100g = 162f, proteinPer100g = 11.2f, carbsPer100g = 3.1f, fatPer100g = 11.8f),
                FoodItem(name = "Huevo picado con chipilín", caloriesPer100g = 160f, proteinPer100g = 11.4f, carbsPer100g = 3.2f, fatPer100g = 11.5f),
                FoodItem(name = "Huevo picado con ejotes", caloriesPer100g = 142f, proteinPer100g = 10.2f, carbsPer100g = 4.1f, fatPer100g = 9.6f),
                FoodItem(name = "Huevo picado con frijoles", caloriesPer100g = 154f, proteinPer100g = 10.4f, carbsPer100g = 10.2f, fatPer100g = 8.1f),
                FoodItem(name = "Huevo estrellado frito", caloriesPer100g = 196f, proteinPer100g = 13.6f, carbsPer100g = 0.8f, fatPer100g = 14.8f),
                FoodItem(name = "Huevo ranchero con salsa de tomate", caloriesPer100g = 148f, proteinPer100g = 10.4f, carbsPer100g = 4.8f, fatPer100g = 9.8f),
                FoodItem(name = "Omelette de jamón y queso", caloriesPer100g = 212f, proteinPer100g = 14.6f, carbsPer100g = 1.8f, fatPer100g = 16.2f),
                FoodItem(name = "Clara de huevo cocida", caloriesPer100g = 52f, proteinPer100g = 10.9f, carbsPer100g = 0.7f, fatPer100g = 0.2f),
                FoodItem(name = "Yema de huevo cocida", caloriesPer100g = 322f, proteinPer100g = 15.9f, carbsPer100g = 3.6f, fatPer100g = 26.5f),
                FoodItem(name = "Huevo de pato entero", caloriesPer100g = 185f, proteinPer100g = 12.8f, carbsPer100g = 1.5f, fatPer100g = 13.8f),
                FoodItem(name = "Huevo de codorniz cocido", caloriesPer100g = 158f, proteinPer100g = 13.1f, carbsPer100g = 0.4f, fatPer100g = 11.1f),
                FoodItem(name = "Huevo de tortuga (referencia tradicional)", caloriesPer100g = 148f, proteinPer100g = 12.2f, carbsPer100g = 0.8f, fatPer100g = 10.2f),
                FoodItem(name = "Queso fresco salvadoreño", caloriesPer100g = 264f, proteinPer100g = 17.5f, carbsPer100g = 2.8f, fatPer100g = 20.4f),
                FoodItem(name = "Queso duro blando salvadoreño", caloriesPer100g = 338f, proteinPer100g = 22.8f, carbsPer100g = 2.1f, fatPer100g = 26.6f),
                FoodItem(name = "Queso duro viejo salvadoreño", caloriesPer100g = 376f, proteinPer100g = 25.4f, carbsPer100g = 1.8f, fatPer100g = 29.8f),
                FoodItem(name = "Queso morolique", caloriesPer100g = 324f, proteinPer100g = 21.6f, carbsPer100g = 2.2f, fatPer100g = 25.4f),
                FoodItem(name = "Queso capita", caloriesPer100g = 312f, proteinPer100g = 20.4f, carbsPer100g = 2.6f, fatPer100g = 24.4f),
                FoodItem(name = "Quesillo salvadoreño para pupusas", caloriesPer100g = 318f, proteinPer100g = 20.8f, carbsPer100g = 2.9f, fatPer100g = 24.8f),
                FoodItem(name = "Queso con loroco", caloriesPer100g = 272f, proteinPer100g = 17.8f, carbsPer100g = 3.1f, fatPer100g = 21f),
                FoodItem(name = "Queso con chile jalapeño", caloriesPer100g = 268f, proteinPer100g = 17.4f, carbsPer100g = 3.2f, fatPer100g = 20.6f),
                FoodItem(name = "Cuajada fresca salvadoreña", caloriesPer100g = 232f, proteinPer100g = 15.6f, carbsPer100g = 3.4f, fatPer100g = 17.4f),
                FoodItem(name = "Requesón salvadoreño", caloriesPer100g = 142f, proteinPer100g = 12.4f, carbsPer100g = 3.6f, fatPer100g = 8.8f),
                FoodItem(name = "Crema pura salvadoreña", caloriesPer100g = 315f, proteinPer100g = 2.6f, carbsPer100g = 3.2f, fatPer100g = 32.5f),
                FoodItem(name = "Crema ácida / cultivada", caloriesPer100g = 198f, proteinPer100g = 2.4f, carbsPer100g = 4.6f, fatPer100g = 19.4f),
                FoodItem(name = "Mantequilla amarilla con sal", caloriesPer100g = 717f, proteinPer100g = 0.9f, carbsPer100g = 0.1f, fatPer100g = 81.1f),
                FoodItem(name = "Margarina de mesa", caloriesPer100g = 714f, proteinPer100g = 0.2f, carbsPer100g = 0.7f, fatPer100g = 80.5f),
                FoodItem(name = "Leche entera de vaca fluida", caloriesPer100g = 61f, proteinPer100g = 3.2f, carbsPer100g = 4.8f, fatPer100g = 3.3f),
                FoodItem(name = "Leche semidescremada (2%)", caloriesPer100g = 50f, proteinPer100g = 3.3f, carbsPer100g = 4.8f, fatPer100g = 2f),
                FoodItem(name = "Leche descremada (0%)", caloriesPer100g = 34f, proteinPer100g = 3.4f, carbsPer100g = 5f, fatPer100g = 0.1f),
                FoodItem(name = "Leche deslactosada entera", caloriesPer100g = 61f, proteinPer100g = 3.2f, carbsPer100g = 4.8f, fatPer100g = 3.3f),
                FoodItem(name = "Leche deslactosada semidescremada", caloriesPer100g = 50f, proteinPer100g = 3.3f, carbsPer100g = 4.8f, fatPer100g = 2f),
                FoodItem(name = "Leche en polvo entera", caloriesPer100g = 496f, proteinPer100g = 26.3f, carbsPer100g = 38.4f, fatPer100g = 26.7f),
                FoodItem(name = "Leche en polvo descremada", caloriesPer100g = 362f, proteinPer100g = 36.2f, carbsPer100g = 52f, fatPer100g = 0.8f),
                FoodItem(name = "Leche condensada azucarada", caloriesPer100g = 321f, proteinPer100g = 7.9f, carbsPer100g = 54.4f, fatPer100g = 8.7f),
                FoodItem(name = "Leche evaporada", caloriesPer100g = 134f, proteinPer100g = 6.8f, carbsPer100g = 10f, fatPer100g = 7.6f),
                FoodItem(name = "Leche poleada (manjar blanco salvadoreño)", caloriesPer100g = 168f, proteinPer100g = 3.8f, carbsPer100g = 28.4f, fatPer100g = 4.2f),
                FoodItem(name = "Yogurt natural entero", caloriesPer100g = 61f, proteinPer100g = 3.5f, carbsPer100g = 4.7f, fatPer100g = 3.3f),
                FoodItem(name = "Yogurt griego natural sin azúcar", caloriesPer100g = 59f, proteinPer100g = 10.2f, carbsPer100g = 3.6f, fatPer100g = 0.4f),
                FoodItem(name = "Yogurt de fresa azucarado", caloriesPer100g = 99f, proteinPer100g = 3.2f, carbsPer100g = 17.4f, fatPer100g = 1.8f),
                FoodItem(name = "Quesadilla salvadoreña (pan de queso y crema)", caloriesPer100g = 384f, proteinPer100g = 7.8f, carbsPer100g = 46.2f, fatPer100g = 18.6f),
                FoodItem(name = "Semita pacha de piña", caloriesPer100g = 362f, proteinPer100g = 5.8f, carbsPer100g = 62.4f, fatPer100g = 10.2f),
                FoodItem(name = "Semita alta de dulce de atado", caloriesPer100g = 374f, proteinPer100g = 6.2f, carbsPer100g = 64.8f, fatPer100g = 10.4f),
                FoodItem(name = "Marquesote salvadoreño", caloriesPer100g = 328f, proteinPer100g = 7.6f, carbsPer100g = 61.4f, fatPer100g = 5.6f),
                FoodItem(name = "Pichardín salvadoreño (relleno de piña)", caloriesPer100g = 368f, proteinPer100g = 5.4f, carbsPer100g = 61.2f, fatPer100g = 11.4f),
                FoodItem(name = "Salpor de arroz", caloriesPer100g = 412f, proteinPer100g = 5.6f, carbsPer100g = 68.4f, fatPer100g = 13f),
                FoodItem(name = "Salpor de almidón", caloriesPer100g = 406f, proteinPer100g = 4.2f, carbsPer100g = 71.2f, fatPer100g = 11.8f),
                FoodItem(name = "Novia salvadoreña (pan dulce con azúcar)", caloriesPer100g = 388f, proteinPer100g = 6.4f, carbsPer100g = 63.8f, fatPer100g = 12f),
                FoodItem(name = "Concha / Pastelito de azúcar salvadoreño", caloriesPer100g = 376f, proteinPer100g = 6.8f, carbsPer100g = 61.5f, fatPer100g = 11.6f),
                FoodItem(name = "Margarita salvadoreña (pan dulce con jalea)", caloriesPer100g = 382f, proteinPer100g = 6.2f, carbsPer100g = 62.4f, fatPer100g = 12.1f),
                FoodItem(name = "Peperecha con dulce de atado y azúcar roja", caloriesPer100g = 365f, proteinPer100g = 5.4f, carbsPer100g = 66.8f, fatPer100g = 8.4f),
                FoodItem(name = "Hojaldra salvadoreña", caloriesPer100g = 424f, proteinPer100g = 6.4f, carbsPer100g = 54.8f, fatPer100g = 20.2f),
                FoodItem(name = "Torta de yema salvadoreña", caloriesPer100g = 356f, proteinPer100g = 7.8f, carbsPer100g = 56.4f, fatPer100g = 11.2f),
                FoodItem(name = "Pan menudo de manteca", caloriesPer100g = 402f, proteinPer100g = 6.6f, carbsPer100g = 60.4f, fatPer100g = 15f),
                FoodItem(name = "Rosquilla de maíz salvadoreña", caloriesPer100g = 418f, proteinPer100g = 8.2f, carbsPer100g = 66.4f, fatPer100g = 13.4f),
                FoodItem(name = "Empanada de pan dulce con leche", caloriesPer100g = 348f, proteinPer100g = 6.2f, carbsPer100g = 56.8f, fatPer100g = 10.6f),
                FoodItem(name = "Milhojas salvadoreña con turrón", caloriesPer100g = 392f, proteinPer100g = 4.8f, carbsPer100g = 61.2f, fatPer100g = 14.2f),
                FoodItem(name = "Relámpago relleno de crema pastelera", caloriesPer100g = 316f, proteinPer100g = 5.8f, carbsPer100g = 42.4f, fatPer100g = 13.8f),
                FoodItem(name = "Budín de pan salvadoreño con guineo y pasas", caloriesPer100g = 254f, proteinPer100g = 5.2f, carbsPer100g = 44.6f, fatPer100g = 6.4f),
                FoodItem(name = "Pan francés salvadoreño (bollo)", caloriesPer100g = 278f, proteinPer100g = 8.8f, carbsPer100g = 55.4f, fatPer100g = 2.2f),
                FoodItem(name = "Pan de caja blanco", caloriesPer100g = 265f, proteinPer100g = 9f, carbsPer100g = 49f, fatPer100g = 3.2f),
                FoodItem(name = "Pan de caja integral", caloriesPer100g = 247f, proteinPer100g = 13f, carbsPer100g = 41f, fatPer100g = 3.4f),
                FoodItem(name = "Pan con ajo tostado", caloriesPer100g = 348f, proteinPer100g = 7.8f, carbsPer100g = 43.2f, fatPer100g = 16f),
                FoodItem(name = "Atol de elote salvadoreño", caloriesPer100g = 98f, proteinPer100g = 2.1f, carbsPer100g = 18.8f, fatPer100g = 1.6f),
                FoodItem(name = "Atol shuco (maíz negro fermentado con frijol y alguashte)", caloriesPer100g = 86f, proteinPer100g = 2.8f, carbsPer100g = 16.4f, fatPer100g = 1.1f),
                FoodItem(name = "Atol de piñuela", caloriesPer100g = 112f, proteinPer100g = 1.8f, carbsPer100g = 23.6f, fatPer100g = 1.2f),
                FoodItem(name = "Atol de maíz tostado", caloriesPer100g = 92f, proteinPer100g = 2.2f, carbsPer100g = 17.6f, fatPer100g = 1.4f),
                FoodItem(name = "Atol de semilla de marañón", caloriesPer100g = 118f, proteinPer100g = 2.9f, carbsPer100g = 19.4f, fatPer100g = 3.2f),
                FoodItem(name = "Atol chuco simple", caloriesPer100g = 74f, proteinPer100g = 2.1f, carbsPer100g = 15.2f, fatPer100g = 0.6f),
                FoodItem(name = "Chilate salvadoreño (atol de maíz simple para dulces)", caloriesPer100g = 52f, proteinPer100g = 1.2f, carbsPer100g = 11f, fatPer100g = 0.4f),
                FoodItem(name = "Poleada / Maicena con leche", caloriesPer100g = 88f, proteinPer100g = 2.8f, carbsPer100g = 15.4f, fatPer100g = 1.8f),
                FoodItem(name = "Horchata de morro salvadoreña (en agua con azúcar)", caloriesPer100g = 78f, proteinPer100g = 1.8f, carbsPer100g = 14.2f, fatPer100g = 1.6f),
                FoodItem(name = "Horchata de morro con leche", caloriesPer100g = 108f, proteinPer100g = 3.6f, carbsPer100g = 16.8f, fatPer100g = 3f),
                FoodItem(name = "Horchata en polvo (seca)", caloriesPer100g = 412f, proteinPer100g = 11.4f, carbsPer100g = 68.5f, fatPer100g = 10.2f),
                FoodItem(name = "Fresco de cebada salvadoreño", caloriesPer100g = 64f, proteinPer100g = 0.8f, carbsPer100g = 14.8f, fatPer100g = 0.2f),
                FoodItem(name = "Fresco de ensalada salvadoreño (con fruta picada)", caloriesPer100g = 56f, proteinPer100g = 0.4f, carbsPer100g = 13.8f, fatPer100g = 0.1f),
                FoodItem(name = "Fresco de tamarindo con azúcar", caloriesPer100g = 58f, proteinPer100g = 0.3f, carbsPer100g = 14.4f, fatPer100g = 0.1f),
                FoodItem(name = "Fresco de jamaica (rosa de jamaica)", caloriesPer100g = 46f, proteinPer100g = 0.2f, carbsPer100g = 11.4f, fatPer100g = 0f),
                FoodItem(name = "Fresco de chan (semilla de chía / chan)", caloriesPer100g = 54f, proteinPer100g = 0.8f, carbsPer100g = 11.2f, fatPer100g = 0.7f),
                FoodItem(name = "Fresco de carao con leche", caloriesPer100g = 96f, proteinPer100g = 3.1f, carbsPer100g = 16.8f, fatPer100g = 1.9f),
                FoodItem(name = "Fresco de carao en agua", caloriesPer100g = 68f, proteinPer100g = 0.6f, carbsPer100g = 16.4f, fatPer100g = 0.1f),
                FoodItem(name = "Fresco de arrayán", caloriesPer100g = 52f, proteinPer100g = 0.3f, carbsPer100g = 12.8f, fatPer100g = 0.1f),
                FoodItem(name = "Fresco de marañón", caloriesPer100g = 54f, proteinPer100g = 0.4f, carbsPer100g = 13.2f, fatPer100g = 0.1f),
                FoodItem(name = "Fresco de melón", caloriesPer100g = 48f, proteinPer100g = 0.4f, carbsPer100g = 11.8f, fatPer100g = 0.1f),
                FoodItem(name = "Fresco de sandía", caloriesPer100g = 42f, proteinPer100g = 0.4f, carbsPer100g = 10.2f, fatPer100g = 0.1f),
                FoodItem(name = "Fresco de maracuyá", caloriesPer100g = 62f, proteinPer100g = 0.5f, carbsPer100g = 15f, fatPer100g = 0.1f),
                FoodItem(name = "Fresco de nance", caloriesPer100g = 64f, proteinPer100g = 0.4f, carbsPer100g = 15.2f, fatPer100g = 0.3f),
                FoodItem(name = "Fresco de tiste salvadoreño", caloriesPer100g = 84f, proteinPer100g = 1.6f, carbsPer100g = 16.8f, fatPer100g = 1.2f),
                FoodItem(name = "Chocolate de tablilla salvadoreño en agua", caloriesPer100g = 76f, proteinPer100g = 1.4f, carbsPer100g = 14.2f, fatPer100g = 1.6f),
                FoodItem(name = "Chocolate de tablilla salvadoreño con leche", caloriesPer100g = 112f, proteinPer100g = 3.8f, carbsPer100g = 16.8f, fatPer100g = 3.4f),
                FoodItem(name = "Tablilla de chocolate artesanal salvadoreño (seca)", caloriesPer100g = 485f, proteinPer100g = 6.8f, carbsPer100g = 68.4f, fatPer100g = 21f),
                FoodItem(name = "Café negro salvadoreño sin azúcar", caloriesPer100g = 2f, proteinPer100g = 0.3f, carbsPer100g = 0f, fatPer100g = 0f),
                FoodItem(name = "Café con leche y azúcar", caloriesPer100g = 54f, proteinPer100g = 1.8f, carbsPer100g = 8.6f, fatPer100g = 1.4f),
                FoodItem(name = "Kolashampan (gaseosa tradicional salvadoreña)", caloriesPer100g = 48f, proteinPer100g = 0f, carbsPer100g = 12f, fatPer100g = 0f),
                FoodItem(name = "Agua de coco natural", caloriesPer100g = 19f, proteinPer100g = 0.7f, carbsPer100g = 3.7f, fatPer100g = 0.2f),
                FoodItem(name = "Alguashte (semilla de ayote molida)", caloriesPer100g = 541f, proteinPer100g = 29.8f, carbsPer100g = 14.7f, fatPer100g = 45.8f),
                FoodItem(name = "Semilla de ayote tostada (pepitoria)", caloriesPer100g = 559f, proteinPer100g = 30.2f, carbsPer100g = 10.7f, fatPer100g = 49f),
                FoodItem(name = "Semilla de marañón tostada", caloriesPer100g = 553f, proteinPer100g = 18.2f, carbsPer100g = 30.2f, fatPer100g = 43.9f),
                FoodItem(name = "Semilla de paterna sancochada", caloriesPer100g = 148f, proteinPer100g = 6.8f, carbsPer100g = 28.4f, fatPer100g = 0.8f),
                FoodItem(name = "Maní / Cacahuate tostado con sal", caloriesPer100g = 585f, proteinPer100g = 23.7f, carbsPer100g = 21.5f, fatPer100g = 49.7f),
                FoodItem(name = "Ajonjolí tostado", caloriesPer100g = 573f, proteinPer100g = 17.7f, carbsPer100g = 23.5f, fatPer100g = 49.7f),
                FoodItem(name = "Relajo salvadoreño (mezcla de especias para recaudo)", caloriesPer100g = 368f, proteinPer100g = 12.4f, carbsPer100g = 52.8f, fatPer100g = 13.6f),
                FoodItem(name = "Achiote en pasta", caloriesPer100g = 248f, proteinPer100g = 6.2f, carbsPer100g = 42.5f, fatPer100g = 6.4f),
                FoodItem(name = "Dulce de atado / Panela salvadoreña", caloriesPer100g = 383f, proteinPer100g = 0.4f, carbsPer100g = 97f, fatPer100g = 0.1f),
                FoodItem(name = "Miel de abeja pura", caloriesPer100g = 304f, proteinPer100g = 0.3f, carbsPer100g = 82.4f, fatPer100g = 0f),
                FoodItem(name = "Loroco fresco crudo", caloriesPer100g = 32f, proteinPer100g = 2.6f, carbsPer100g = 5.8f, fatPer100g = 0.3f),
                FoodItem(name = "Chipilín fresco (hojas crudas)", caloriesPer100g = 56f, proteinPer100g = 7f, carbsPer100g = 8.2f, fatPer100g = 0.8f),
                FoodItem(name = "Hojas de mora frescas crudas", caloriesPer100g = 38f, proteinPer100g = 4.3f, carbsPer100g = 6.4f, fatPer100g = 0.6f),
                FoodItem(name = "Flor de izote cruda", caloriesPer100g = 41f, proteinPer100g = 2.7f, carbsPer100g = 8.1f, fatPer100g = 0.4f),
                FoodItem(name = "Flor de ayote cruda", caloriesPer100g = 15f, proteinPer100g = 1f, carbsPer100g = 3.3f, fatPer100g = 0.1f),
                FoodItem(name = "Pacaya fresca cocida", caloriesPer100g = 39f, proteinPer100g = 3.2f, carbsPer100g = 6.8f, fatPer100g = 0.5f),
                FoodItem(name = "Pito (flor de pito / pitos en alguashte)", caloriesPer100g = 84f, proteinPer100g = 4.6f, carbsPer100g = 11.2f, fatPer100g = 2.6f),
                FoodItem(name = "Pito fresco (flor cruda)", caloriesPer100g = 36f, proteinPer100g = 2.8f, carbsPer100g = 6.5f, fatPer100g = 0.3f),
                FoodItem(name = "Ayote tierno crudo", caloriesPer100g = 20f, proteinPer100g = 1.1f, carbsPer100g = 4.3f, fatPer100g = 0.1f),
                FoodItem(name = "Ayote maduro (sazón) cocido", caloriesPer100g = 26f, proteinPer100g = 1f, carbsPer100g = 6.5f, fatPer100g = 0.1f),
                FoodItem(name = "Pipián salvadoreño crudo", caloriesPer100g = 17f, proteinPer100g = 1.2f, carbsPer100g = 3.4f, fatPer100g = 0.2f),
                FoodItem(name = "Pipián en crema salvadoreño", caloriesPer100g = 78f, proteinPer100g = 2.4f, carbsPer100g = 5.2f, fatPer100g = 5.4f),
                FoodItem(name = "Güisquil (chayote) cocido", caloriesPer100g = 24f, proteinPer100g = 0.6f, carbsPer100g = 5.1f, fatPer100g = 0.5f),
                FoodItem(name = "Punta de güisquil (guías tiernas) cocida", caloriesPer100g = 28f, proteinPer100g = 2.4f, carbsPer100g = 4.6f, fatPer100g = 0.3f),
                FoodItem(name = "Chilacayote crudo", caloriesPer100g = 19f, proteinPer100g = 0.8f, carbsPer100g = 4.2f, fatPer100g = 0.1f),
                FoodItem(name = "Ejotes frescos cocidos", caloriesPer100g = 35f, proteinPer100g = 1.9f, carbsPer100g = 7.9f, fatPer100g = 0.3f),
                FoodItem(name = "Ejotes en alguashte", caloriesPer100g = 92f, proteinPer100g = 4.8f, carbsPer100g = 9.6f, fatPer100g = 4.2f),
                FoodItem(name = "Papas sancochadas", caloriesPer100g = 87f, proteinPer100g = 1.9f, carbsPer100g = 20.1f, fatPer100g = 0.1f),
                FoodItem(name = "Papas fritas estilo salvadoreño", caloriesPer100g = 312f, proteinPer100g = 3.4f, carbsPer100g = 41.4f, fatPer100g = 14.7f),
                FoodItem(name = "Camote sancochado", caloriesPer100g = 86f, proteinPer100g = 1.6f, carbsPer100g = 20.1f, fatPer100g = 0.1f),
                FoodItem(name = "Malanga sancochada", caloriesPer100g = 112f, proteinPer100g = 1.5f, carbsPer100g = 26.5f, fatPer100g = 0.2f),
                FoodItem(name = "Jícama fresca (con limón y alguashte)", caloriesPer100g = 46f, proteinPer100g = 1.2f, carbsPer100g = 9.4f, fatPer100g = 0.6f),
                FoodItem(name = "Jícama fresca cruda", caloriesPer100g = 38f, proteinPer100g = 0.7f, carbsPer100g = 8.8f, fatPer100g = 0.1f),
                FoodItem(name = "Tomate de cocina fresco", caloriesPer100g = 18f, proteinPer100g = 0.9f, carbsPer100g = 3.9f, fatPer100g = 0.2f),
                FoodItem(name = "Cebolla blanca fresca", caloriesPer100g = 40f, proteinPer100g = 1.1f, carbsPer100g = 9.3f, fatPer100g = 0.1f),
                FoodItem(name = "Cebolla morada fresca", caloriesPer100g = 42f, proteinPer100g = 1.2f, carbsPer100g = 9.6f, fatPer100g = 0.1f),
                FoodItem(name = "Chile verde dulce (pimiento)", caloriesPer100g = 20f, proteinPer100g = 0.9f, carbsPer100g = 4.6f, fatPer100g = 0.2f),
                FoodItem(name = "Chile jalapeño fresco", caloriesPer100g = 29f, proteinPer100g = 0.9f, carbsPer100g = 6.5f, fatPer100g = 0.4f),
                FoodItem(name = "Chile chiltepe fresco", caloriesPer100g = 40f, proteinPer100g = 1.9f, carbsPer100g = 8.8f, fatPer100g = 0.4f),
                FoodItem(name = "Güicoy cocido", caloriesPer100g = 26f, proteinPer100g = 1f, carbsPer100g = 6.2f, fatPer100g = 0.1f),
                FoodItem(name = "Repollo blanco crudo", caloriesPer100g = 25f, proteinPer100g = 1.3f, carbsPer100g = 5.8f, fatPer100g = 0.1f),
                FoodItem(name = "Repollo morado crudo", caloriesPer100g = 31f, proteinPer100g = 1.4f, carbsPer100g = 7.4f, fatPer100g = 0.2f),
                FoodItem(name = "Zanahoria cruda", caloriesPer100g = 41f, proteinPer100g = 0.9f, carbsPer100g = 9.6f, fatPer100g = 0.2f),
                FoodItem(name = "Remolacha cocida", caloriesPer100g = 44f, proteinPer100g = 1.7f, carbsPer100g = 10f, fatPer100g = 0.2f),
                FoodItem(name = "Rábano fresco", caloriesPer100g = 16f, proteinPer100g = 0.7f, carbsPer100g = 3.4f, fatPer100g = 0.1f),
                FoodItem(name = "Pepino fresco con cáscara", caloriesPer100g = 15f, proteinPer100g = 0.7f, carbsPer100g = 3.6f, fatPer100g = 0.1f),
                FoodItem(name = "Aguacate Hass / Criollo", caloriesPer100g = 160f, proteinPer100g = 2f, carbsPer100g = 8.5f, fatPer100g = 14.7f),
                FoodItem(name = "Guacamole salvadoreño (con huevo duro y cebolla)", caloriesPer100g = 152f, proteinPer100g = 4.6f, carbsPer100g = 6.8f, fatPer100g = 12.4f),
                FoodItem(name = "Chirmol salvadoreño asado", caloriesPer100g = 34f, proteinPer100g = 1.1f, carbsPer100g = 6.2f, fatPer100g = 0.6f),
                FoodItem(name = "Chimichurri salvadoreño (cilantro, ajo y aceite)", caloriesPer100g = 246f, proteinPer100g = 1.2f, carbsPer100g = 4.8f, fatPer100g = 25f),
                FoodItem(name = "Alcapate (culantro coyote) fresco", caloriesPer100g = 32f, proteinPer100g = 2.2f, carbsPer100g = 6.4f, fatPer100g = 0.3f),
                FoodItem(name = "Cilantro fresco", caloriesPer100g = 23f, proteinPer100g = 2.1f, carbsPer100g = 3.7f, fatPer100g = 0.5f),
                FoodItem(name = "Hierbabuena fresca", caloriesPer100g = 44f, proteinPer100g = 3.3f, carbsPer100g = 8.4f, fatPer100g = 0.7f),
                FoodItem(name = "Perejil fresco", caloriesPer100g = 36f, proteinPer100g = 3f, carbsPer100g = 6.3f, fatPer100g = 0.8f),
                FoodItem(name = "Berro fresco", caloriesPer100g = 11f, proteinPer100g = 2.3f, carbsPer100g = 1.3f, fatPer100g = 0.1f),
                FoodItem(name = "Espinaca cruda", caloriesPer100g = 23f, proteinPer100g = 2.9f, carbsPer100g = 3.6f, fatPer100g = 0.4f),
                FoodItem(name = "Acelga cocida", caloriesPer100g = 20f, proteinPer100g = 1.9f, carbsPer100g = 4.1f, fatPer100g = 0.1f),
                FoodItem(name = "Lechuga romana fresca", caloriesPer100g = 17f, proteinPer100g = 1.2f, carbsPer100g = 3.3f, fatPer100g = 0.3f),
                FoodItem(name = "Brócoli al vapor", caloriesPer100g = 35f, proteinPer100g = 2.4f, carbsPer100g = 7.2f, fatPer100g = 0.4f),
                FoodItem(name = "Coliflor al vapor", caloriesPer100g = 23f, proteinPer100g = 1.8f, carbsPer100g = 4.1f, fatPer100g = 0.5f),
                FoodItem(name = "Berenjena guisada", caloriesPer100g = 64f, proteinPer100g = 1.2f, carbsPer100g = 8.4f, fatPer100g = 3.2f),
                FoodItem(name = "Okra (quingombó) cocida", caloriesPer100g = 22f, proteinPer100g = 1.9f, carbsPer100g = 4.5f, fatPer100g = 0.2f),
                FoodItem(name = "Guineo de seda (banano maduro)", caloriesPer100g = 89f, proteinPer100g = 1.1f, carbsPer100g = 22.8f, fatPer100g = 0.3f),
                FoodItem(name = "Guineo majoncho cocido", caloriesPer100g = 122f, proteinPer100g = 1.3f, carbsPer100g = 29.8f, fatPer100g = 0.3f),
                FoodItem(name = "Guineo manzano", caloriesPer100g = 94f, proteinPer100g = 1.2f, carbsPer100g = 23.6f, fatPer100g = 0.3f),
                FoodItem(name = "Guineo morado", caloriesPer100g = 92f, proteinPer100g = 1.3f, carbsPer100g = 23.1f, fatPer100g = 0.3f),
                FoodItem(name = "Plátano macho crudo", caloriesPer100g = 122f, proteinPer100g = 1.3f, carbsPer100g = 31.9f, fatPer100g = 0.4f),
                FoodItem(name = "Jocote de corona maduro", caloriesPer100g = 76f, proteinPer100g = 0.9f, carbsPer100g = 18.4f, fatPer100g = 0.2f),
                FoodItem(name = "Jocote barón rojo", caloriesPer100g = 74f, proteinPer100g = 0.8f, carbsPer100g = 17.9f, fatPer100g = 0.2f),
                FoodItem(name = "Jocote verde con sal y alguashte", caloriesPer100g = 68f, proteinPer100g = 1.8f, carbsPer100g = 13.5f, fatPer100g = 1.1f),
                FoodItem(name = "Mango verde en tiras con limón, sal y alguashte", caloriesPer100g = 74f, proteinPer100g = 2.1f, carbsPer100g = 14.2f, fatPer100g = 1.2f),
                FoodItem(name = "Mango maduro (Panadés / Tommy)", caloriesPer100g = 60f, proteinPer100g = 0.8f, carbsPer100g = 15f, fatPer100g = 0.4f),
                FoodItem(name = "Mango ciruela maduro", caloriesPer100g = 65f, proteinPer100g = 0.8f, carbsPer100g = 16.2f, fatPer100g = 0.3f),
                FoodItem(name = "Marañón (falso fruto fresco)", caloriesPer100g = 46f, proteinPer100g = 0.8f, carbsPer100g = 11.4f, fatPer100g = 0.2f),
                FoodItem(name = "Marañón japonés", caloriesPer100g = 32f, proteinPer100g = 0.6f, carbsPer100g = 7.8f, fatPer100g = 0.1f),
                FoodItem(name = "Nance fresco", caloriesPer100g = 73f, proteinPer100g = 0.7f, carbsPer100g = 17f, fatPer100g = 1.2f),
                FoodItem(name = "Anona blanca / rosada salvadoreña", caloriesPer100g = 94f, proteinPer100g = 1.7f, carbsPer100g = 23.6f, fatPer100g = 0.4f),
                FoodItem(name = "Sincuya (cabeza de negro)", caloriesPer100g = 98f, proteinPer100g = 1.6f, carbsPer100g = 24.2f, fatPer100g = 0.4f),
                FoodItem(name = "Guanábana fresca", caloriesPer100g = 66f, proteinPer100g = 1f, carbsPer100g = 16.8f, fatPer100g = 0.3f),
                FoodItem(name = "Zapote salvadoreño (mamey zapote)", caloriesPer100g = 124f, proteinPer100g = 1.5f, carbsPer100g = 32.1f, fatPer100g = 0.5f),
                FoodItem(name = "Mamey amarillo", caloriesPer100g = 51f, proteinPer100g = 0.5f, carbsPer100g = 12.5f, fatPer100g = 0.5f),
                FoodItem(name = "Níspero (chicozapote)", caloriesPer100g = 83f, proteinPer100g = 0.4f, carbsPer100g = 20f, fatPer100g = 1.1f),
                FoodItem(name = "Caimito morado / verde", caloriesPer100g = 67f, proteinPer100g = 1f, carbsPer100g = 14.6f, fatPer100g = 1.6f),
                FoodItem(name = "Paterna (pulpa blanca fresca)", caloriesPer100g = 62f, proteinPer100g = 1.1f, carbsPer100g = 15.2f, fatPer100g = 0.2f),
                FoodItem(name = "Pepeto (pulpa fresca)", caloriesPer100g = 60f, proteinPer100g = 1.1f, carbsPer100g = 14.8f, fatPer100g = 0.2f),
                FoodItem(name = "Cuje (pulpa fresca)", caloriesPer100g = 61f, proteinPer100g = 1f, carbsPer100g = 15f, fatPer100g = 0.2f),
                FoodItem(name = "Arrayán fresco", caloriesPer100g = 42f, proteinPer100g = 0.6f, carbsPer100g = 10.2f, fatPer100g = 0.2f),
                FoodItem(name = "Guayaba rosada fresca", caloriesPer100g = 68f, proteinPer100g = 2.6f, carbsPer100g = 14.3f, fatPer100g = 1f),
                FoodItem(name = "Papaturro fresco", caloriesPer100g = 58f, proteinPer100g = 0.9f, carbsPer100g = 14.1f, fatPer100g = 0.2f),
                FoodItem(name = "Huiscoyol (coyol)", caloriesPer100g = 118f, proteinPer100g = 1.4f, carbsPer100g = 24.6f, fatPer100g = 2.2f),
                FoodItem(name = "Piñuela fresca", caloriesPer100g = 54f, proteinPer100g = 0.8f, carbsPer100g = 13.2f, fatPer100g = 0.2f),
                FoodItem(name = "Carao (pulpa de vaina)", caloriesPer100g = 268f, proteinPer100g = 3.8f, carbsPer100g = 64.2f, fatPer100g = 0.6f),
                FoodItem(name = "Tamarindo (pulpa pura)", caloriesPer100g = 239f, proteinPer100g = 2.8f, carbsPer100g = 62.5f, fatPer100g = 0.6f),
                FoodItem(name = "Papaya madura", caloriesPer100g = 43f, proteinPer100g = 0.5f, carbsPer100g = 10.8f, fatPer100g = 0.3f),
                FoodItem(name = "Sandía fresca", caloriesPer100g = 30f, proteinPer100g = 0.6f, carbsPer100g = 7.6f, fatPer100g = 0.2f),
                FoodItem(name = "Melón cantalupo fresco", caloriesPer100g = 34f, proteinPer100g = 0.8f, carbsPer100g = 8.2f, fatPer100g = 0.2f),
                FoodItem(name = "Piña madura fresca", caloriesPer100g = 50f, proteinPer100g = 0.5f, carbsPer100g = 13.1f, fatPer100g = 0.1f),
                FoodItem(name = "Naranja dulce salvadoreña", caloriesPer100g = 47f, proteinPer100g = 0.9f, carbsPer100g = 11.8f, fatPer100g = 0.1f),
                FoodItem(name = "Mandarina criolla", caloriesPer100g = 53f, proteinPer100g = 0.8f, carbsPer100g = 13.3f, fatPer100g = 0.3f),
                FoodItem(name = "Limón pérsico / indio (jugo)", caloriesPer100g = 22f, proteinPer100g = 0.4f, carbsPer100g = 6.9f, fatPer100g = 0.2f),
                FoodItem(name = "Toronja rosada", caloriesPer100g = 42f, proteinPer100g = 0.8f, carbsPer100g = 10.7f, fatPer100g = 0.1f),
                FoodItem(name = "Coco tierno (agua y comida blanda)", caloriesPer100g = 78f, proteinPer100g = 1.4f, carbsPer100g = 7.2f, fatPer100g = 5.2f),
                FoodItem(name = "Coco rallado fresco (pulpa madura)", caloriesPer100g = 354f, proteinPer100g = 3.3f, carbsPer100g = 15.2f, fatPer100g = 33.5f),
                FoodItem(name = "Granadilla fresca", caloriesPer100g = 97f, proteinPer100g = 2.2f, carbsPer100g = 23.4f, fatPer100g = 0.7f),
                FoodItem(name = "Maracuyá (fruta de la pasión)", caloriesPer100g = 97f, proteinPer100g = 2.2f, carbsPer100g = 23.4f, fatPer100g = 0.7f),
                FoodItem(name = "Carambola (fruta estrella)", caloriesPer100g = 31f, proteinPer100g = 1f, carbsPer100g = 6.7f, fatPer100g = 0.3f),
                FoodItem(name = "Pitahaya (fruta del dragón)", caloriesPer100g = 60f, proteinPer100g = 1.2f, carbsPer100g = 13f, fatPer100g = 0.4f),
                FoodItem(name = "Litchi / Mamón chino (rambután)", caloriesPer100g = 82f, proteinPer100g = 0.7f, carbsPer100g = 20.9f, fatPer100g = 0.2f),
                FoodItem(name = "Mamoncillo (talpa / mamón)", caloriesPer100g = 58f, proteinPer100g = 0.9f, carbsPer100g = 14.2f, fatPer100g = 0.2f),
                FoodItem(name = "Icaco en miel", caloriesPer100g = 178f, proteinPer100g = 0.6f, carbsPer100g = 44.2f, fatPer100g = 0.3f),
                FoodItem(name = "Higo en miel salvadoreño", caloriesPer100g = 212f, proteinPer100g = 1.1f, carbsPer100g = 52.4f, fatPer100g = 0.4f),
                FoodItem(name = "Manzana roja con cáscara", caloriesPer100g = 52f, proteinPer100g = 0.3f, carbsPer100g = 13.8f, fatPer100g = 0.2f),
                FoodItem(name = "Manzana verde Granny Smith", caloriesPer100g = 52f, proteinPer100g = 0.4f, carbsPer100g = 13.6f, fatPer100g = 0.2f),
                FoodItem(name = "Pera fresca", caloriesPer100g = 57f, proteinPer100g = 0.4f, carbsPer100g = 15.2f, fatPer100g = 0.1f),
                FoodItem(name = "Uvas rojas / verdes", caloriesPer100g = 69f, proteinPer100g = 0.7f, carbsPer100g = 18.1f, fatPer100g = 0.2f),
                FoodItem(name = "Fresas frescas de Apaneca / Chalatenango", caloriesPer100g = 32f, proteinPer100g = 0.7f, carbsPer100g = 7.7f, fatPer100g = 0.3f),
                FoodItem(name = "Moras silvestres (zarzamora)", caloriesPer100g = 43f, proteinPer100g = 1.4f, carbsPer100g = 9.6f, fatPer100g = 0.5f),
                FoodItem(name = "Melocotón / Durazno fresco", caloriesPer100g = 39f, proteinPer100g = 0.9f, carbsPer100g = 9.5f, fatPer100g = 0.3f),
                FoodItem(name = "Ciruela roja fresca", caloriesPer100g = 46f, proteinPer100g = 0.7f, carbsPer100g = 11.4f, fatPer100g = 0.3f),
                FoodItem(name = "Kiwi fresco", caloriesPer100g = 61f, proteinPer100g = 1.1f, carbsPer100g = 14.7f, fatPer100g = 0.5f),
                FoodItem(name = "Boca colorada (pargo rojo) frito entero", caloriesPer100g = 218f, proteinPer100g = 24.6f, carbsPer100g = 4.2f, fatPer100g = 11.2f),
                FoodItem(name = "Boca colorada a la plancha (filete)", caloriesPer100g = 128f, proteinPer100g = 26.3f, carbsPer100g = 0f, fatPer100g = 1.7f),
                FoodItem(name = "Boca colorada al ajillo", caloriesPer100g = 168f, proteinPer100g = 24.8f, carbsPer100g = 2.4f, fatPer100g = 6.4f),
                FoodItem(name = "Curvina (corvina) a la plancha", caloriesPer100g = 124f, proteinPer100g = 24.5f, carbsPer100g = 0f, fatPer100g = 2.3f),
                FoodItem(name = "Curvina empanizada frita", caloriesPer100g = 224f, proteinPer100g = 19.8f, carbsPer100g = 14.2f, fatPer100g = 9.6f),
                FoodItem(name = "Lonja de tiburón / cazón a la plancha", caloriesPer100g = 130f, proteinPer100g = 21f, carbsPer100g = 0f, fatPer100g = 4.5f),
                FoodItem(name = "Lonja de mero al ajillo", caloriesPer100g = 158f, proteinPer100g = 24.2f, carbsPer100g = 1.8f, fatPer100g = 5.6f),
                FoodItem(name = "Tilapia frita entera", caloriesPer100g = 208f, proteinPer100g = 23.4f, carbsPer100g = 3.8f, fatPer100g = 10.8f),
                FoodItem(name = "Tilapia al vapor / a la plancha", caloriesPer100g = 128f, proteinPer100g = 26.2f, carbsPer100g = 0f, fatPer100g = 2.7f),
                FoodItem(name = "Macarela frita", caloriesPer100g = 242f, proteinPer100g = 22.8f, carbsPer100g = 2.6f, fatPer100g = 15.4f),
                FoodItem(name = "Robalo a la plancha", caloriesPer100g = 124f, proteinPer100g = 23.6f, carbsPer100g = 0f, fatPer100g = 2.6f),
                FoodItem(name = "Dorado (mahi-mahi) a la plancha", caloriesPer100g = 109f, proteinPer100g = 23.7f, carbsPer100g = 0f, fatPer100g = 0.9f),
                FoodItem(name = "Dorado al ajillo", caloriesPer100g = 152f, proteinPer100g = 22.4f, carbsPer100g = 1.8f, fatPer100g = 5.8f),
                FoodItem(name = "Pepesca (pescaditas de río fritas)", caloriesPer100g = 264f, proteinPer100g = 28.4f, carbsPer100g = 3.2f, fatPer100g = 15.2f),
                FoodItem(name = "Pescado seco salado (bacalao / lonja seca sin hidratar)", caloriesPer100g = 290f, proteinPer100g = 62.8f, carbsPer100g = 0f, fatPer100g = 2.4f),
                FoodItem(name = "Atún enlatado en agua (escurrido)", caloriesPer100g = 116f, proteinPer100g = 25.5f, carbsPer100g = 0f, fatPer100g = 0.8f),
                FoodItem(name = "Atún enlatado en aceite (escurrido)", caloriesPer100g = 198f, proteinPer100g = 29.1f, carbsPer100g = 0f, fatPer100g = 8.2f),
                FoodItem(name = "Sardina enlatada en salsa de tomate", caloriesPer100g = 185f, proteinPer100g = 20.9f, carbsPer100g = 1.8f, fatPer100g = 10.5f),
                FoodItem(name = "Camarones al ajillo", caloriesPer100g = 162f, proteinPer100g = 21.4f, carbsPer100g = 2.4f, fatPer100g = 7.2f),
                FoodItem(name = "Camarones empanizados fritos", caloriesPer100g = 242f, proteinPer100g = 16.8f, carbsPer100g = 18.4f, fatPer100g = 11.2f),
                FoodItem(name = "Camarones a la plancha", caloriesPer100g = 119f, proteinPer100g = 22.8f, carbsPer100g = 1.2f, fatPer100g = 1.9f),
                FoodItem(name = "Camarones en salsa rosada", caloriesPer100g = 184f, proteinPer100g = 18.6f, carbsPer100g = 6.4f, fatPer100g = 9.2f),
                FoodItem(name = "Coctel de camarones en salsa de tomate", caloriesPer100g = 98f, proteinPer100g = 13.4f, carbsPer100g = 8.6f, fatPer100g = 1.2f),
                FoodItem(name = "Coctel de conchas (curiles) salvadoreño", caloriesPer100g = 84f, proteinPer100g = 12.2f, carbsPer100g = 5.8f, fatPer100g = 1.2f),
                FoodItem(name = "Curiles / Conchas negras frescas", caloriesPer100g = 79f, proteinPer100g = 13.8f, carbsPer100g = 3.2f, fatPer100g = 1f),
                FoodItem(name = "Ceviche de pescado salvadoreño", caloriesPer100g = 96f, proteinPer100g = 15.6f, carbsPer100g = 5.4f, fatPer100g = 1.4f),
                FoodItem(name = "Ceviche mixto (pescado, camarón y calamar)", caloriesPer100g = 102f, proteinPer100g = 16.2f, carbsPer100g = 5.8f, fatPer100g = 1.6f),
                FoodItem(name = "Calamar al ajillo", caloriesPer100g = 158f, proteinPer100g = 17.8f, carbsPer100g = 3.6f, fatPer100g = 7.8f),
                FoodItem(name = "Aros de calamar empanizados", caloriesPer100g = 236f, proteinPer100g = 14.8f, carbsPer100g = 18.2f, fatPer100g = 11.4f),
                FoodItem(name = "Pulpo al ajillo", caloriesPer100g = 164f, proteinPer100g = 24.8f, carbsPer100g = 3.2f, fatPer100g = 5.4f),
                FoodItem(name = "Langosta a la plancha con mantequilla", caloriesPer100g = 148f, proteinPer100g = 20.6f, carbsPer100g = 1.2f, fatPer100g = 6.4f),
                FoodItem(name = "Jaiba cocida (carne)", caloriesPer100g = 87f, proteinPer100g = 18.1f, carbsPer100g = 0f, fatPer100g = 1.1f),
                FoodItem(name = "Punche relleno salvadoreño", caloriesPer100g = 174f, proteinPer100g = 14.2f, carbsPer100g = 11.8f, fatPer100g = 7.6f),
                FoodItem(name = "Pupusa de chicharrón con loroco (maíz)", caloriesPer100g = 257f, proteinPer100g = 11.4f, carbsPer100g = 27.6f, fatPer100g = 11.2f),
                FoodItem(name = "Pupusa de chicharrón con loroco (arroz)", caloriesPer100g = 271f, proteinPer100g = 11.1f, carbsPer100g = 30.8f, fatPer100g = 11.5f),
                FoodItem(name = "Pupusa de frijol con loroco y queso (maíz)", caloriesPer100g = 226f, proteinPer100g = 9.4f, carbsPer100g = 31.8f, fatPer100g = 6.8f),
                FoodItem(name = "Pupusa de frijol con loroco y queso (arroz)", caloriesPer100g = 240f, proteinPer100g = 9.1f, carbsPer100g = 35f, fatPer100g = 7.1f),
                FoodItem(name = "Pupusa de frijol con chicharrón (maíz)", caloriesPer100g = 245f, proteinPer100g = 10.6f, carbsPer100g = 29.5f, fatPer100g = 9.4f),
                FoodItem(name = "Pupusa de frijol con chicharrón (arroz)", caloriesPer100g = 259f, proteinPer100g = 10.3f, carbsPer100g = 32.7f, fatPer100g = 9.7f),
                FoodItem(name = "Pupusa de queso con güisquil (maíz)", caloriesPer100g = 218f, proteinPer100g = 8.7f, carbsPer100g = 29.6f, fatPer100g = 7.2f),
                FoodItem(name = "Pupusa de queso con güisquil (arroz)", caloriesPer100g = 232f, proteinPer100g = 8.4f, carbsPer100g = 32.8f, fatPer100g = 7.5f),
                FoodItem(name = "Pupusa de queso con pipián (maíz)", caloriesPer100g = 216f, proteinPer100g = 8.6f, carbsPer100g = 29.4f, fatPer100g = 7.1f),
                FoodItem(name = "Pupusa de queso con pipián (arroz)", caloriesPer100g = 230f, proteinPer100g = 8.3f, carbsPer100g = 32.6f, fatPer100g = 7.4f),
                FoodItem(name = "Pupusa de queso con berro (maíz)", caloriesPer100g = 225f, proteinPer100g = 9.3f, carbsPer100g = 29.5f, fatPer100g = 7.8f),
                FoodItem(name = "Pupusa de queso con berro (arroz)", caloriesPer100g = 240f, proteinPer100g = 9f, carbsPer100g = 32.7f, fatPer100g = 8.1f),
                FoodItem(name = "Pupusa de queso con albahaca (maíz)", caloriesPer100g = 230f, proteinPer100g = 9.5f, carbsPer100g = 29.8f, fatPer100g = 8.1f),
                FoodItem(name = "Pupusa de queso con albahaca (arroz)", caloriesPer100g = 244f, proteinPer100g = 9.2f, carbsPer100g = 33f, fatPer100g = 8.4f),
                FoodItem(name = "Pupusa de queso con cilantro (maíz)", caloriesPer100g = 229f, proteinPer100g = 9.5f, carbsPer100g = 29.8f, fatPer100g = 8f),
                FoodItem(name = "Pupusa de queso con cilantro (arroz)", caloriesPer100g = 244f, proteinPer100g = 9.2f, carbsPer100g = 33f, fatPer100g = 8.3f),
                FoodItem(name = "Pupusa de queso con chiltoma (chile verde) (maíz)", caloriesPer100g = 228f, proteinPer100g = 9.2f, carbsPer100g = 30.1f, fatPer100g = 7.9f),
                FoodItem(name = "Pupusa de queso con chiltoma (chile verde) (arroz)", caloriesPer100g = 243f, proteinPer100g = 8.9f, carbsPer100g = 33.3f, fatPer100g = 8.2f),
                FoodItem(name = "Pupusa de queso con cebolla morada (maíz)", caloriesPer100g = 231f, proteinPer100g = 9.3f, carbsPer100g = 30.4f, fatPer100g = 8f),
                FoodItem(name = "Pupusa de queso con cebolla morada (arroz)", caloriesPer100g = 245f, proteinPer100g = 9f, carbsPer100g = 33.6f, fatPer100g = 8.3f),
                FoodItem(name = "Pupusa de queso con tomate y albahaca (maíz)", caloriesPer100g = 229f, proteinPer100g = 9.2f, carbsPer100g = 30.2f, fatPer100g = 7.9f),
                FoodItem(name = "Pupusa de queso con tomate y albahaca (arroz)", caloriesPer100g = 243f, proteinPer100g = 8.9f, carbsPer100g = 33.4f, fatPer100g = 8.2f),
                FoodItem(name = "Pupusa de carne asada con queso (maíz)", caloriesPer100g = 253f, proteinPer100g = 13.8f, carbsPer100g = 27.4f, fatPer100g = 9.8f),
                FoodItem(name = "Pupusa de carne asada con queso (arroz)", caloriesPer100g = 267f, proteinPer100g = 13.5f, carbsPer100g = 30.6f, fatPer100g = 10.1f),
                FoodItem(name = "Pupusa de curiles con queso (maíz)", caloriesPer100g = 233f, proteinPer100g = 12.2f, carbsPer100g = 28.4f, fatPer100g = 7.8f),
                FoodItem(name = "Pupusa de curiles con queso (arroz)", caloriesPer100g = 247f, proteinPer100g = 11.9f, carbsPer100g = 31.6f, fatPer100g = 8.1f),
                FoodItem(name = "Pupusa de punche con queso (maíz)", caloriesPer100g = 232f, proteinPer100g = 12f, carbsPer100g = 28.2f, fatPer100g = 7.9f),
                FoodItem(name = "Pupusa de punche con queso (arroz)", caloriesPer100g = 246f, proteinPer100g = 11.7f, carbsPer100g = 31.4f, fatPer100g = 8.2f),
                FoodItem(name = "Pupusa de calamar con queso (maíz)", caloriesPer100g = 236f, proteinPer100g = 12.5f, carbsPer100g = 28.3f, fatPer100g = 8.1f),
                FoodItem(name = "Pupusa de calamar con queso (arroz)", caloriesPer100g = 250f, proteinPer100g = 12.2f, carbsPer100g = 31.5f, fatPer100g = 8.4f),
                FoodItem(name = "Pupusa de atún con queso (maíz)", caloriesPer100g = 239f, proteinPer100g = 13.4f, carbsPer100g = 28f, fatPer100g = 8.2f),
                FoodItem(name = "Pupusa de atún con queso (arroz)", caloriesPer100g = 254f, proteinPer100g = 13.1f, carbsPer100g = 31.2f, fatPer100g = 8.5f),
                FoodItem(name = "Pupusa de salchicha con queso (maíz)", caloriesPer100g = 258f, proteinPer100g = 10.8f, carbsPer100g = 28.6f, fatPer100g = 11.2f),
                FoodItem(name = "Pupusa de salchicha con queso (arroz)", caloriesPer100g = 273f, proteinPer100g = 10.5f, carbsPer100g = 31.8f, fatPer100g = 11.5f),
                FoodItem(name = "Pupusa de pepperoni con queso (maíz)", caloriesPer100g = 274f, proteinPer100g = 11.9f, carbsPer100g = 27.8f, fatPer100g = 12.8f),
                FoodItem(name = "Pupusa de pepperoni con queso (arroz)", caloriesPer100g = 288f, proteinPer100g = 11.6f, carbsPer100g = 31f, fatPer100g = 13.1f),
                FoodItem(name = "Pupusa de champiñones con ajo y queso (maíz)", caloriesPer100g = 228f, proteinPer100g = 9.4f, carbsPer100g = 29.6f, fatPer100g = 8f),
                FoodItem(name = "Pupusa de champiñones con ajo y queso (arroz)", caloriesPer100g = 242f, proteinPer100g = 9.1f, carbsPer100g = 32.8f, fatPer100g = 8.3f),
                FoodItem(name = "Pupusa de brócoli con queso (maíz)", caloriesPer100g = 227f, proteinPer100g = 9.5f, carbsPer100g = 29.7f, fatPer100g = 7.8f),
                FoodItem(name = "Pupusa de brócoli con queso (arroz)", caloriesPer100g = 241f, proteinPer100g = 9.2f, carbsPer100g = 32.9f, fatPer100g = 8.1f),
                FoodItem(name = "Pupusa de chipilín, loroco y queso (maíz)", caloriesPer100g = 233f, proteinPer100g = 9.8f, carbsPer100g = 29.9f, fatPer100g = 8.2f),
                FoodItem(name = "Pupusa de chipilín, loroco y queso (arroz)", caloriesPer100g = 247f, proteinPer100g = 9.5f, carbsPer100g = 33.1f, fatPer100g = 8.5f),
                FoodItem(name = "Pupusa de mora, loroco y queso (maíz)", caloriesPer100g = 231f, proteinPer100g = 9.7f, carbsPer100g = 29.8f, fatPer100g = 8.1f),
                FoodItem(name = "Pupusa de mora, loroco y queso (arroz)", caloriesPer100g = 245f, proteinPer100g = 9.4f, carbsPer100g = 33f, fatPer100g = 8.4f),
                FoodItem(name = "Pupusa de ayote, loroco y queso (maíz)", caloriesPer100g = 221f, proteinPer100g = 8.9f, carbsPer100g = 29.8f, fatPer100g = 7.4f),
                FoodItem(name = "Pupusa de ayote, loroco y queso (arroz)", caloriesPer100g = 236f, proteinPer100g = 8.6f, carbsPer100g = 33f, fatPer100g = 7.7f),
                FoodItem(name = "Pupusa de requesón con loroco (maíz)", caloriesPer100g = 220f, proteinPer100g = 10.2f, carbsPer100g = 29.4f, fatPer100g = 6.8f),
                FoodItem(name = "Pupusa de requesón con loroco (arroz)", caloriesPer100g = 234f, proteinPer100g = 9.9f, carbsPer100g = 32.6f, fatPer100g = 7.1f),
                FoodItem(name = "Pupusa de cuajada con loroco (maíz)", caloriesPer100g = 225f, proteinPer100g = 9.6f, carbsPer100g = 29.6f, fatPer100g = 7.6f),
                FoodItem(name = "Pupusa de cuajada con loroco (arroz)", caloriesPer100g = 240f, proteinPer100g = 9.3f, carbsPer100g = 32.8f, fatPer100g = 7.9f),
                FoodItem(name = "Picado de verduras con carne molida salvadoreño", caloriesPer100g = 128f, proteinPer100g = 11.2f, carbsPer100g = 8.6f, fatPer100g = 5.4f),
                FoodItem(name = "Picado de papa con ejotes y carne", caloriesPer100g = 122f, proteinPer100g = 9.8f, carbsPer100g = 11.4f, fatPer100g = 4.2f),
                FoodItem(name = "Picado de güisquil con huevo", caloriesPer100g = 96f, proteinPer100g = 5.8f, carbsPer100g = 7.4f, fatPer100g = 4.8f),
                FoodItem(name = "Picado de pipián con maíz y crema", caloriesPer100g = 92f, proteinPer100g = 3.2f, carbsPer100g = 9.8f, fatPer100g = 4.6f),
                FoodItem(name = "Picado de rábano con hierbabuena y limón", caloriesPer100g = 24f, proteinPer100g = 1.1f, carbsPer100g = 4.4f, fatPer100g = 0.2f),
                FoodItem(name = "Tortitas de carne molida salvadoreñas (con hierbabuena)", caloriesPer100g = 218f, proteinPer100g = 16.8f, carbsPer100g = 8.4f, fatPer100g = 13f),
                FoodItem(name = "Tortitas de carne molida en salsa de tomate", caloriesPer100g = 174f, proteinPer100g = 13.6f, carbsPer100g = 9.2f, fatPer100g = 9.2f),
                FoodItem(name = "Tortitas de papa con queso", caloriesPer100g = 196f, proteinPer100g = 6.4f, carbsPer100g = 24.2f, fatPer100g = 8.2f),
                FoodItem(name = "Tortitas de yuca con queso", caloriesPer100g = 224f, proteinPer100g = 5.8f, carbsPer100g = 31.4f, fatPer100g = 8.4f),
                FoodItem(name = "Tortitas de espinaca con huevo", caloriesPer100g = 142f, proteinPer100g = 8.6f, carbsPer100g = 9.2f, fatPer100g = 7.8f),
                FoodItem(name = "Tortitas de mora con huevo", caloriesPer100g = 144f, proteinPer100g = 8.8f, carbsPer100g = 9.4f, fatPer100g = 7.8f),
                FoodItem(name = "Tortitas de chipilín con huevo", caloriesPer100g = 148f, proteinPer100g = 9.2f, carbsPer100g = 9.6f, fatPer100g = 8f),
                FoodItem(name = "Tortitas de loroco con huevo", caloriesPer100g = 152f, proteinPer100g = 8.9f, carbsPer100g = 9.8f, fatPer100g = 8.4f),
                FoodItem(name = "Tortitas de flor de izote con huevo", caloriesPer100g = 138f, proteinPer100g = 8.2f, carbsPer100g = 9.4f, fatPer100g = 7.4f),
                FoodItem(name = "Tortitas de atún con verdura", caloriesPer100g = 184f, proteinPer100g = 15.4f, carbsPer100g = 10.2f, fatPer100g = 9f),
                FoodItem(name = "Tortitas de sardina salvadoreñas", caloriesPer100g = 192f, proteinPer100g = 14.8f, carbsPer100g = 10.6f, fatPer100g = 10f),
                FoodItem(name = "Tortitas de soya condimentadas", caloriesPer100g = 168f, proteinPer100g = 14.2f, carbsPer100g = 13.8f, fatPer100g = 6.2f),
                FoodItem(name = "Albóndigas de res en salsa de tomate con hierbabuena", caloriesPer100g = 164f, proteinPer100g = 14.8f, carbsPer100g = 7.8f, fatPer100g = 8.2f),
                FoodItem(name = "Albóndigas de pollo en caldo", caloriesPer100g = 126f, proteinPer100g = 14.2f, carbsPer100g = 6.4f, fatPer100g = 4.8f),
                FoodItem(name = "Salpicón de res con limón y rábano", caloriesPer100g = 138f, proteinPer100g = 21.4f, carbsPer100g = 3.4f, fatPer100g = 4.2f),
                FoodItem(name = "Salpicón de pollo salvadoreño", caloriesPer100g = 132f, proteinPer100g = 22f, carbsPer100g = 3.2f, fatPer100g = 3.4f),
                FoodItem(name = "Salpicón de conejo", caloriesPer100g = 128f, proteinPer100g = 21.8f, carbsPer100g = 3f, fatPer100g = 3.2f),
                FoodItem(name = "Conejo asado al carbón", caloriesPer100g = 173f, proteinPer100g = 29f, carbsPer100g = 0f, fatPer100g = 5.6f),
                FoodItem(name = "Conejo en salsa criolla", caloriesPer100g = 154f, proteinPer100g = 22.4f, carbsPer100g = 4.8f, fatPer100g = 5f),
                FoodItem(name = "Garrobo en alguashte (plato tradicional rural)", caloriesPer100g = 168f, proteinPer100g = 21.6f, carbsPer100g = 5.4f, fatPer100g = 6.8f),
                FoodItem(name = "Cusuco asado (referencia tradicional rural)", caloriesPer100g = 182f, proteinPer100g = 23.4f, carbsPer100g = 0f, fatPer100g = 9.8f),
                FoodItem(name = "Codorniz en salsa", caloriesPer100g = 196f, proteinPer100g = 21.2f, carbsPer100g = 4.2f, fatPer100g = 10.4f),
                FoodItem(name = "Chao mein salvadoreño con pollo", caloriesPer100g = 162f, proteinPer100g = 9.8f, carbsPer100g = 20.4f, fatPer100g = 4.6f),
                FoodItem(name = "Chao mein salvadoreño mixto (pollo, res y camarón)", caloriesPer100g = 172f, proteinPer100g = 11.4f, carbsPer100g = 19.8f, fatPer100g = 5.2f),
                FoodItem(name = "Chop suey salvadoreño con pollo", caloriesPer100g = 118f, proteinPer100g = 9.6f, carbsPer100g = 8.8f, fatPer100g = 4.8f),
                FoodItem(name = "Espaguetis con crema y queso duro blando", caloriesPer100g = 214f, proteinPer100g = 8.6f, carbsPer100g = 26.8f, fatPer100g = 8f),
                FoodItem(name = "Espaguetis en salsa de tomate criolla con pollo", caloriesPer100g = 158f, proteinPer100g = 9.8f, carbsPer100g = 22.4f, fatPer100g = 3.4f),
                FoodItem(name = "Espaguetis con carne molida salvadoreños", caloriesPer100g = 168f, proteinPer100g = 10.4f, carbsPer100g = 21.8f, fatPer100g = 4.4f),
                FoodItem(name = "Coditos con mayonesa, jamón y verduras (ensalada de coditos)", caloriesPer100g = 196f, proteinPer100g = 6.4f, carbsPer100g = 22.6f, fatPer100g = 8.8f),
                FoodItem(name = "Ensalada rusa salvadoreña (papa, remolacha, zanahoria y mayonesa)", caloriesPer100g = 148f, proteinPer100g = 2.4f, carbsPer100g = 16.8f, fatPer100g = 8f),
                FoodItem(name = "Ensalada de repollo con zanahoria y mayonesa", caloriesPer100g = 132f, proteinPer100g = 1.6f, carbsPer100g = 8.4f, fatPer100g = 10.2f),
                FoodItem(name = "Ensalada fresca (lechuga, tomate, pepino y rábano)", caloriesPer100g = 22f, proteinPer100g = 1f, carbsPer100g = 4.2f, fatPer100g = 0.2f),
                FoodItem(name = "Escabeche salvadoreño para panes con pollo", caloriesPer100g = 68f, proteinPer100g = 1.4f, carbsPer100g = 8.6f, fatPer100g = 3.2f),
                FoodItem(name = "Recaudo salvadoreño para panes con pavo", caloriesPer100g = 58f, proteinPer100g = 1.6f, carbsPer100g = 8.2f, fatPer100g = 2.1f),
                FoodItem(name = "Salsa criolla salvadoreña (tomate, cebolla y chile verde)", caloriesPer100g = 42f, proteinPer100g = 1.2f, carbsPer100g = 6.8f, fatPer100g = 1.2f),
                FoodItem(name = "Chismol / Pico de gallo salvadoreño", caloriesPer100g = 26f, proteinPer100g = 0.9f, carbsPer100g = 5.4f, fatPer100g = 0.2f),
                FoodItem(name = "Frijoles blancos con costilla de cerdo y verdura", caloriesPer100g = 148f, proteinPer100g = 10.4f, carbsPer100g = 16.8f, fatPer100g = 4.4f),
                FoodItem(name = "Frijoles rojos con chicharrón", caloriesPer100g = 184f, proteinPer100g = 11.2f, carbsPer100g = 18.6f, fatPer100g = 7.2f),
                FoodItem(name = "Frijoles rojos con chorizo", caloriesPer100g = 178f, proteinPer100g = 9.8f, carbsPer100g = 18.4f, fatPer100g = 7.2f),
                FoodItem(name = "Frijoles volteados con queso duro blando", caloriesPer100g = 188f, proteinPer100g = 9.6f, carbsPer100g = 19.8f, fatPer100g = 7.8f),
                FoodItem(name = "Plátanos fritos con crema y frijoles volteados (plato típico)", caloriesPer100g = 218f, proteinPer100g = 5.2f, carbsPer100g = 31.4f, fatPer100g = 8.2f),
                FoodItem(name = "Plátanos fritos con crema y queso fresco", caloriesPer100g = 236f, proteinPer100g = 6.4f, carbsPer100g = 29.8f, fatPer100g = 10.2f),
                FoodItem(name = "Casamiento con crema y queso duro", caloriesPer100g = 182f, proteinPer100g = 7.8f, carbsPer100g = 22.4f, fatPer100g = 6.8f),
                FoodItem(name = "Sopa de tortilla con aguacate y queso", caloriesPer100g = 132f, proteinPer100g = 5.8f, carbsPer100g = 14.2f, fatPer100g = 5.8f),
                FoodItem(name = "Sopa de albóndigas de res con hierbabuena", caloriesPer100g = 98f, proteinPer100g = 8.4f, carbsPer100g = 7.6f, fatPer100g = 3.8f),
                FoodItem(name = "Sopa de pollo con fideos y papa", caloriesPer100g = 84f, proteinPer100g = 7.2f, carbsPer100g = 8.8f, fatPer100g = 2.2f),
                FoodItem(name = "Sopa de menudos de pollo con arroz", caloriesPer100g = 92f, proteinPer100g = 8.4f, carbsPer100g = 9.2f, fatPer100g = 2.4f),
                FoodItem(name = "Sopa de costilla de res al carbón", caloriesPer100g = 104f, proteinPer100g = 8.6f, carbsPer100g = 7.4f, fatPer100g = 4.4f),
                FoodItem(name = "Sopa de garrobo con masa", caloriesPer100g = 96f, proteinPer100g = 8.8f, carbsPer100g = 8.2f, fatPer100g = 3.1f),
                FoodItem(name = "Sopa de verduras con leche y queso", caloriesPer100g = 78f, proteinPer100g = 3.8f, carbsPer100g = 9.4f, fatPer100g = 2.8f),
                FoodItem(name = "Sopa de guías de güisquil con huevo", caloriesPer100g = 68f, proteinPer100g = 4.6f, carbsPer100g = 7.2f, fatPer100g = 2.4f),
                FoodItem(name = "Sopa de flor de ayote con masa", caloriesPer100g = 64f, proteinPer100g = 2.8f, carbsPer100g = 9.6f, fatPer100g = 1.6f),
                FoodItem(name = "Sopa de elote con crema", caloriesPer100g = 108f, proteinPer100g = 3.4f, carbsPer100g = 15.6f, fatPer100g = 3.6f),
                FoodItem(name = "Sopa instantánea de vaso con pollo (preparada)", caloriesPer100g = 88f, proteinPer100g = 2.4f, carbsPer100g = 11.8f, fatPer100g = 3.4f),
                FoodItem(name = "Sopa de sobre de pollo con fideos (preparada)", caloriesPer100g = 42f, proteinPer100g = 1.4f, carbsPer100g = 6.8f, fatPer100g = 0.9f),
                FoodItem(name = "Sopa de caracol en leche de coco", caloriesPer100g = 124f, proteinPer100g = 10.8f, carbsPer100g = 6.2f, fatPer100g = 6.2f),
                FoodItem(name = "Sopa de siete mares salvadoreña", caloriesPer100g = 116f, proteinPer100g = 12.4f, carbsPer100g = 5.2f, fatPer100g = 5f),
                FoodItem(name = "Maíz blanco en grano seco", caloriesPer100g = 365f, proteinPer100g = 9.4f, carbsPer100g = 74.3f, fatPer100g = 4.7f),
                FoodItem(name = "Maíz amarillo en grano seco", caloriesPer100g = 365f, proteinPer100g = 9.4f, carbsPer100g = 74.3f, fatPer100g = 4.7f),
                FoodItem(name = "Maíz negro / morado en grano seco", caloriesPer100g = 362f, proteinPer100g = 9.2f, carbsPer100g = 73.8f, fatPer100g = 4.5f),
                FoodItem(name = "Maíz blanco sancochado (pozol)", caloriesPer100g = 138f, proteinPer100g = 3.6f, carbsPer100g = 28.4f, fatPer100g = 1.4f),
                FoodItem(name = "Elote tierno desgranado crudo", caloriesPer100g = 86f, proteinPer100g = 3.3f, carbsPer100g = 18.7f, fatPer100g = 1.4f),
                FoodItem(name = "Maicillo (sorgo blanco) en grano seco", caloriesPer100g = 329f, proteinPer100g = 10.6f, carbsPer100g = 72.1f, fatPer100g = 3.5f),
                FoodItem(name = "Arroz blanco en grano crudo", caloriesPer100g = 365f, proteinPer100g = 7.1f, carbsPer100g = 80f, fatPer100g = 0.7f),
                FoodItem(name = "Arroz precocido en grano crudo", caloriesPer100g = 371f, proteinPer100g = 7.5f, carbsPer100g = 81.7f, fatPer100g = 0.6f),
                FoodItem(name = "Arroz integral en grano crudo", caloriesPer100g = 367f, proteinPer100g = 7.5f, carbsPer100g = 76.2f, fatPer100g = 3.2f),
                FoodItem(name = "Arroz integral cocido", caloriesPer100g = 111f, proteinPer100g = 2.6f, carbsPer100g = 23f, fatPer100g = 0.9f),
                FoodItem(name = "Frijol rojo de seda en grano seco", caloriesPer100g = 333f, proteinPer100g = 23.6f, carbsPer100g = 60f, fatPer100g = 0.8f),
                FoodItem(name = "Frijol tinto en grano seco", caloriesPer100g = 335f, proteinPer100g = 22.8f, carbsPer100g = 60.8f, fatPer100g = 1f),
                FoodItem(name = "Frijol negro en grano seco", caloriesPer100g = 341f, proteinPer100g = 21.6f, carbsPer100g = 62.4f, fatPer100g = 1.4f),
                FoodItem(name = "Frijol blanco en grano seco", caloriesPer100g = 333f, proteinPer100g = 23.4f, carbsPer100g = 60.3f, fatPer100g = 0.9f),
                FoodItem(name = "Frijol mica (frijol pequeño rojo) cocido", caloriesPer100g = 129f, proteinPer100g = 8.8f, carbsPer100g = 23f, fatPer100g = 0.6f),
                FoodItem(name = "Frijol chilipuca tierno cocido", caloriesPer100g = 118f, proteinPer100g = 7.6f, carbsPer100g = 21.2f, fatPer100g = 0.5f),
                FoodItem(name = "Frijol de soya en grano seco", caloriesPer100g = 446f, proteinPer100g = 36.5f, carbsPer100g = 30.2f, fatPer100g = 19.9f),
                FoodItem(name = "Carne de soya texturizada seca", caloriesPer100g = 336f, proteinPer100g = 51.5f, carbsPer100g = 30.8f, fatPer100g = 1.2f),
                FoodItem(name = "Carne de soya texturizada hidratada y guisada", caloriesPer100g = 128f, proteinPer100g = 16.4f, carbsPer100g = 9.8f, fatPer100g = 2.6f),
                FoodItem(name = "Garbanzos en grano seco", caloriesPer100g = 364f, proteinPer100g = 19.3f, carbsPer100g = 60.7f, fatPer100g = 6f),
                FoodItem(name = "Garbanzos cocidos", caloriesPer100g = 164f, proteinPer100g = 8.9f, carbsPer100g = 27.4f, fatPer100g = 2.6f),
                FoodItem(name = "Lentejas en grano seco", caloriesPer100g = 352f, proteinPer100g = 24.6f, carbsPer100g = 63.4f, fatPer100g = 1.1f),
                FoodItem(name = "Lentejas cocidas guisadas", caloriesPer100g = 116f, proteinPer100g = 9f, carbsPer100g = 20.1f, fatPer100g = 0.4f),
                FoodItem(name = "Arvejas / Chícharos verdes cocidos", caloriesPer100g = 84f, proteinPer100g = 5.4f, carbsPer100g = 15.6f, fatPer100g = 0.2f),
                FoodItem(name = "Habas cocidas", caloriesPer100g = 110f, proteinPer100g = 7.6f, carbsPer100g = 19.7f, fatPer100g = 0.4f),
                FoodItem(name = "Avena en hojuelas cruda", caloriesPer100g = 389f, proteinPer100g = 16.9f, carbsPer100g = 66.3f, fatPer100g = 6.9f),
                FoodItem(name = "Avena molida para refresco o atol", caloriesPer100g = 389f, proteinPer100g = 16.9f, carbsPer100g = 66.3f, fatPer100g = 6.9f),
                FoodItem(name = "Avena cocida en agua", caloriesPer100g = 68f, proteinPer100g = 2.4f, carbsPer100g = 12f, fatPer100g = 1.4f),
                FoodItem(name = "Avena cocida con leche y canela", caloriesPer100g = 98f, proteinPer100g = 4.2f, carbsPer100g = 15.2f, fatPer100g = 2.4f),
                FoodItem(name = "Fresco de avena con leche y canela", caloriesPer100g = 76f, proteinPer100g = 2.4f, carbsPer100g = 13.4f, fatPer100g = 1.4f),
                FoodItem(name = "Incaparina en polvo (harina nutricional fortificada)", caloriesPer100g = 372f, proteinPer100g = 21.3f, carbsPer100g = 65f, fatPer100g = 3f),
                FoodItem(name = "Atol de Incaparina preparado con leche y azúcar", caloriesPer100g = 96f, proteinPer100g = 4.6f, carbsPer100g = 15.4f, fatPer100g = 1.8f),
                FoodItem(name = "Atol de Incaparina preparado en agua", caloriesPer100g = 68f, proteinPer100g = 3.2f, carbsPer100g = 12.4f, fatPer100g = 0.6f),
                FoodItem(name = "Bienestarina / Harina de maíz y soya fortificada", caloriesPer100g = 368f, proteinPer100g = 20.5f, carbsPer100g = 64.8f, fatPer100g = 3.2f),
                FoodItem(name = "Fécula de maíz (Maicena) en polvo", caloriesPer100g = 381f, proteinPer100g = 0.3f, carbsPer100g = 91.3f, fatPer100g = 0.1f),
                FoodItem(name = "Almidón de yuca en polvo", caloriesPer100g = 358f, proteinPer100g = 0.2f, carbsPer100g = 88.7f, fatPer100g = 0f),
                FoodItem(name = "Harina de trigo todo uso (suave)", caloriesPer100g = 364f, proteinPer100g = 10.3f, carbsPer100g = 76.3f, fatPer100g = 1f),
                FoodItem(name = "Harina de trigo fuerte (para panificación)", caloriesPer100g = 361f, proteinPer100g = 12f, carbsPer100g = 72.5f, fatPer100g = 1.7f),
                FoodItem(name = "Harina de trigo integral", caloriesPer100g = 340f, proteinPer100g = 13.2f, carbsPer100g = 72f, fatPer100g = 2.5f),
                FoodItem(name = "Harina para panqueques (mezcla lista)", caloriesPer100g = 355f, proteinPer100g = 8.4f, carbsPer100g = 73.2f, fatPer100g = 3.1f),
                FoodItem(name = "Panqueques preparados (sin miel)", caloriesPer100g = 227f, proteinPer100g = 6.4f, carbsPer100g = 28.3f, fatPer100g = 9.7f),
                FoodItem(name = "Cebada perlada cruda", caloriesPer100g = 352f, proteinPer100g = 9.9f, carbsPer100g = 77.7f, fatPer100g = 1.2f),
                FoodItem(name = "Polvo para fresco de cebada salvadoreño", caloriesPer100g = 374f, proteinPer100g = 6.2f, carbsPer100g = 84.5f, fatPer100g = 1.2f),
                FoodItem(name = "Quinoa en grano cruda", caloriesPer100g = 368f, proteinPer100g = 14.1f, carbsPer100g = 64.2f, fatPer100g = 6.1f),
                FoodItem(name = "Quinoa cocida", caloriesPer100g = 120f, proteinPer100g = 4.4f, carbsPer100g = 21.3f, fatPer100g = 1.9f),
                FoodItem(name = "Chía (semilla seca)", caloriesPer100g = 486f, proteinPer100g = 16.5f, carbsPer100g = 42.1f, fatPer100g = 30.7f),
                FoodItem(name = "Linaza (semilla seca)", caloriesPer100g = 534f, proteinPer100g = 18.3f, carbsPer100g = 28.9f, fatPer100g = 42.2f),
                FoodItem(name = "Semilla de morro cruda", caloriesPer100g = 518f, proteinPer100g = 22.4f, carbsPer100g = 24.6f, fatPer100g = 39.8f),
                FoodItem(name = "Cacao en grano tostado (para tiste o tablilla)", caloriesPer100g = 546f, proteinPer100g = 14.2f, carbsPer100g = 34.8f, fatPer100g = 42.6f),
                FoodItem(name = "Pasta espagueti cruda (seca)", caloriesPer100g = 371f, proteinPer100g = 13f, carbsPer100g = 74.7f, fatPer100g = 1.5f),
                FoodItem(name = "Pasta espagueti cocida simple", caloriesPer100g = 158f, proteinPer100g = 5.8f, carbsPer100g = 30.9f, fatPer100g = 0.9f),
                FoodItem(name = "Pasta coditos cocida simple", caloriesPer100g = 157f, proteinPer100g = 5.8f, carbsPer100g = 30.7f, fatPer100g = 0.9f),
                FoodItem(name = "Pasta caracolitos cocida simple", caloriesPer100g = 157f, proteinPer100g = 5.8f, carbsPer100g = 30.7f, fatPer100g = 0.9f),
                FoodItem(name = "Pasta tornillitos (fusilli) cocida simple", caloriesPer100g = 158f, proteinPer100g = 5.8f, carbsPer100g = 30.8f, fatPer100g = 0.9f),
                FoodItem(name = "Pasta corbatitas cocida simple", caloriesPer100g = 158f, proteinPer100g = 5.8f, carbsPer100g = 30.8f, fatPer100g = 0.9f),
                FoodItem(name = "Fideos cabello de ángel cocidos", caloriesPer100g = 154f, proteinPer100g = 5.4f, carbsPer100g = 30.2f, fatPer100g = 0.8f),
                FoodItem(name = "Lasaña de carne horneada", caloriesPer100g = 168f, proteinPer100g = 11.4f, carbsPer100g = 15.2f, fatPer100g = 6.8f),
                FoodItem(name = "Lasaña de pollo en salsa blanca", caloriesPer100g = 178f, proteinPer100g = 12.2f, carbsPer100g = 15.6f, fatPer100g = 7.4f),
                FoodItem(name = "Jamón de pavo en rebanadas", caloriesPer100g = 104f, proteinPer100g = 17.2f, carbsPer100g = 3.8f, fatPer100g = 2.1f),
                FoodItem(name = "Jamón de cerdo cocido", caloriesPer100g = 145f, proteinPer100g = 16.5f, carbsPer100g = 2.4f, fatPer100g = 7.6f),
                FoodItem(name = "Jamón Virginia con miel", caloriesPer100g = 128f, proteinPer100g = 16.8f, carbsPer100g = 5.2f, fatPer100g = 4.2f),
                FoodItem(name = "Jamón serrano", caloriesPer100g = 241f, proteinPer100g = 31f, carbsPer100g = 0.5f, fatPer100g = 13f),
                FoodItem(name = "Mortadela popular en rebanadas", caloriesPer100g = 288f, proteinPer100g = 12.4f, carbsPer100g = 4.8f, fatPer100g = 24.2f),
                FoodItem(name = "Mortadela con chile y pimiento", caloriesPer100g = 282f, proteinPer100g = 12.2f, carbsPer100g = 5.1f, fatPer100g = 23.6f),
                FoodItem(name = "Salchicha de pollo (hot dog)", caloriesPer100g = 232f, proteinPer100g = 12.6f, carbsPer100g = 4.2f, fatPer100g = 18.4f),
                FoodItem(name = "Salchicha de pavo", caloriesPer100g = 216f, proteinPer100g = 13.2f, carbsPer100g = 3.8f, fatPer100g = 16.4f),
                FoodItem(name = "Salchicha de cerdo y res (Viena)", caloriesPer100g = 276f, proteinPer100g = 11.8f, carbsPer100g = 3.6f, fatPer100g = 23.8f),
                FoodItem(name = "Salchicha tipo Frankfurt", caloriesPer100g = 292f, proteinPer100g = 12.4f, carbsPer100g = 2.8f, fatPer100g = 25.6f),
                FoodItem(name = "Salchicha parrillera con queso", caloriesPer100g = 304f, proteinPer100g = 13.6f, carbsPer100g = 3.2f, fatPer100g = 26.2f),
                FoodItem(name = "Chorizo argentino parrillero", caloriesPer100g = 318f, proteinPer100g = 16.8f, carbsPer100g = 2.4f, fatPer100g = 26.8f),
                FoodItem(name = "Chorizo español curado", caloriesPer100g = 455f, proteinPer100g = 24.1f, carbsPer100g = 1.9f, fatPer100g = 38.3f),
                FoodItem(name = "Chorizo de pavo", caloriesPer100g = 214f, proteinPer100g = 18.2f, carbsPer100g = 2.6f, fatPer100g = 14.4f),
                FoodItem(name = "Chorizo de pollo", caloriesPer100g = 208f, proteinPer100g = 17.8f, carbsPer100g = 2.8f, fatPer100g = 13.8f),
                FoodItem(name = "Tocino / Bacon de cerdo crudo", caloriesPer100g = 417f, proteinPer100g = 12.6f, carbsPer100g = 1.3f, fatPer100g = 39.7f),
                FoodItem(name = "Tocino / Bacon de cerdo frito crujiente", caloriesPer100g = 541f, proteinPer100g = 37f, carbsPer100g = 1.4f, fatPer100g = 41.8f),
                FoodItem(name = "Tocino de pavo", caloriesPer100g = 218f, proteinPer100g = 15.8f, carbsPer100g = 2.2f, fatPer100g = 16.2f),
                FoodItem(name = "Salami rebanado", caloriesPer100g = 336f, proteinPer100g = 21.8f, carbsPer100g = 2.4f, fatPer100g = 26.2f),
                FoodItem(name = "Pepperoni rebanado", caloriesPer100g = 494f, proteinPer100g = 22.9f, carbsPer100g = 1.2f, fatPer100g = 43.9f),
                FoodItem(name = "Salchichón popular", caloriesPer100g = 296f, proteinPer100g = 13.8f, carbsPer100g = 4.2f, fatPer100g = 24.8f),
                FoodItem(name = "Paté de hígado de cerdo (enlatado)", caloriesPer100g = 319f, proteinPer100g = 11.4f, carbsPer100g = 3.8f, fatPer100g = 28f),
                FoodItem(name = "Jamón del diablo (carne endiablada enlatada)", caloriesPer100g = 294f, proteinPer100g = 13.2f, carbsPer100g = 2.8f, fatPer100g = 25.4f),
                FoodItem(name = "Carne prensada enlatada (Spam)", caloriesPer100g = 315f, proteinPer100g = 13.4f, carbsPer100g = 3.1f, fatPer100g = 27.4f),
                FoodItem(name = "Salchichas de Viena enlatadas", caloriesPer100g = 230f, proteinPer100g = 10.5f, carbsPer100g = 2.2f, fatPer100g = 19.8f),
                FoodItem(name = "Sardinas enlatadas en aceite vegetal", caloriesPer100g = 208f, proteinPer100g = 24.6f, carbsPer100g = 0f, fatPer100g = 11.5f),
                FoodItem(name = "Sardinas enlatadas picantes en tomate", caloriesPer100g = 188f, proteinPer100g = 20.6f, carbsPer100g = 2.1f, fatPer100g = 10.6f),
                FoodItem(name = "Atún enlatado con vegetales (ensalada de atún)", caloriesPer100g = 128f, proteinPer100g = 14.2f, carbsPer100g = 8.4f, fatPer100g = 4.1f),
                FoodItem(name = "Atún enlatado con jalapeño", caloriesPer100g = 122f, proteinPer100g = 23.8f, carbsPer100g = 1.8f, fatPer100g = 1.6f),
                FoodItem(name = "Salmón rosado enlatado", caloriesPer100g = 136f, proteinPer100g = 21.4f, carbsPer100g = 0f, fatPer100g = 5.2f),
                FoodItem(name = "Elotitos dulces enlatados (escurridos)", caloriesPer100g = 81f, proteinPer100g = 2.6f, carbsPer100g = 17.1f, fatPer100g = 1f),
                FoodItem(name = "Guisantes / Petit pois enlatados", caloriesPer100g = 69f, proteinPer100g = 4.4f, carbsPer100g = 12.6f, fatPer100g = 0.4f),
                FoodItem(name = "Vegetales mixtos enlatados", caloriesPer100g = 49f, proteinPer100g = 2.6f, carbsPer100g = 9.4f, fatPer100g = 0.2f),
                FoodItem(name = "Hongos / Champiñones rebanados enlatados", caloriesPer100g = 25f, proteinPer100g = 1.9f, carbsPer100g = 5.1f, fatPer100g = 0.3f),
                FoodItem(name = "Garbanzos enlatados", caloriesPer100g = 139f, proteinPer100g = 7f, carbsPer100g = 22.6f, fatPer100g = 2.2f),
                FoodItem(name = "Frijoles rojos volteados en bolsa / lata (comerciales)", caloriesPer100g = 138f, proteinPer100g = 6.8f, carbsPer100g = 18.6f, fatPer100g = 4.1f),
                FoodItem(name = "Frijoles negros volteados en bolsa / lata (comerciales)", caloriesPer100g = 134f, proteinPer100g = 6.6f, carbsPer100g = 18.2f, fatPer100g = 3.9f),
                FoodItem(name = "Frijoles rojos con queso en bolsa", caloriesPer100g = 152f, proteinPer100g = 7.6f, carbsPer100g = 18.2f, fatPer100g = 5.4f),
                FoodItem(name = "Frijoles rojos con chorizo en bolsa", caloriesPer100g = 162f, proteinPer100g = 7.8f, carbsPer100g = 17.8f, fatPer100g = 6.4f),
                FoodItem(name = "Chiles jalapeños en escabeche enlatados", caloriesPer100g = 27f, proteinPer100g = 0.9f, carbsPer100g = 4.7f, fatPer100g = 0.9f),
                FoodItem(name = "Aceitunas verdes rellenas", caloriesPer100g = 145f, proteinPer100g = 1f, carbsPer100g = 3.8f, fatPer100g = 15.3f),
                FoodItem(name = "Aceitunas negras sin hueso", caloriesPer100g = 115f, proteinPer100g = 0.8f, carbsPer100g = 6.3f, fatPer100g = 10.7f),
                FoodItem(name = "Alcaparras en vinagre", caloriesPer100g = 23f, proteinPer100g = 2.4f, carbsPer100g = 4.9f, fatPer100g = 0.9f),
                FoodItem(name = "Melocotones en almíbar enlatados", caloriesPer100g = 74f, proteinPer100g = 0.5f, carbsPer100g = 18.9f, fatPer100g = 0.1f),
                FoodItem(name = "Coctel de frutas en almíbar enlatado", caloriesPer100g = 72f, proteinPer100g = 0.4f, carbsPer100g = 18.5f, fatPer100g = 0.1f),
                FoodItem(name = "Piña en rodajas en almíbar", caloriesPer100g = 78f, proteinPer100g = 0.4f, carbsPer100g = 20.1f, fatPer100g = 0.1f),
                FoodItem(name = "Pescado relleno en salsa (Semana Santa)", caloriesPer100g = 196f, proteinPer100g = 18.2f, carbsPer100g = 12.4f, fatPer100g = 8.2f),
                FoodItem(name = "Lonja de pescado empanizada", caloriesPer100g = 216f, proteinPer100g = 19.4f, carbsPer100g = 14.8f, fatPer100g = 8.8f),
                FoodItem(name = "Lonja de pescado en salsa de ajo y mantequilla", caloriesPer100g = 168f, proteinPer100g = 22.4f, carbsPer100g = 1.8f, fatPer100g = 7.8f),
                FoodItem(name = "Lonja de pescado a la diabla (salsa picante)", caloriesPer100g = 148f, proteinPer100g = 21.8f, carbsPer100g = 5.4f, fatPer100g = 4.2f),
                FoodItem(name = "Lonja de pescado en crema y loroco", caloriesPer100g = 184f, proteinPer100g = 21.2f, carbsPer100g = 4.2f, fatPer100g = 9.2f),
                FoodItem(name = "Pargo rojo relleno de camarones", caloriesPer100g = 188f, proteinPer100g = 23.8f, carbsPer100g = 4.6f, fatPer100g = 8.2f),
                FoodItem(name = "Filete de salmón a la plancha", caloriesPer100g = 208f, proteinPer100g = 20.4f, carbsPer100g = 0f, fatPer100g = 13.4f),
                FoodItem(name = "Filete de salmón crudo", caloriesPer100g = 206f, proteinPer100g = 20f, carbsPer100g = 0f, fatPer100g = 13f),
                FoodItem(name = "Trucha arcoíris a la plancha", caloriesPer100g = 168f, proteinPer100g = 23.8f, carbsPer100g = 0f, fatPer100g = 7.2f),
                FoodItem(name = "Bagre / Guapote frito", caloriesPer100g = 228f, proteinPer100g = 21.6f, carbsPer100g = 3.8f, fatPer100g = 14f),
                FoodItem(name = "Mojarra frita entera (Lago de Ilopango / Coatepeque)", caloriesPer100g = 214f, proteinPer100g = 23.2f, carbsPer100g = 3.6f, fatPer100g = 11.8f),
                FoodItem(name = "Mojarra al ajillo", caloriesPer100g = 174f, proteinPer100g = 23.6f, carbsPer100g = 2.4f, fatPer100g = 7.6f),
                FoodItem(name = "Cangrejo azul cocido", caloriesPer100g = 83f, proteinPer100g = 17.9f, carbsPer100g = 0f, fatPer100g = 1.1f),
                FoodItem(name = "Camarones de río al ajillo", caloriesPer100g = 156f, proteinPer100g = 20.8f, carbsPer100g = 2.2f, fatPer100g = 6.8f),
                FoodItem(name = "Chacalines (camaroncitos secos salados)", caloriesPer100g = 295f, proteinPer100g = 62.4f, carbsPer100g = 1.8f, fatPer100g = 3.8f),
                FoodItem(name = "Tortitas de chacalines en salsa", caloriesPer100g = 198f, proteinPer100g = 16.4f, carbsPer100g = 15.8f, fatPer100g = 7.8f),
                FoodItem(name = "Arroz con camarones salvadoreño", caloriesPer100g = 164f, proteinPer100g = 10.8f, carbsPer100g = 22.4f, fatPer100g = 3.4f),
                FoodItem(name = "Arroz con mariscos mixto", caloriesPer100g = 168f, proteinPer100g = 11.4f, carbsPer100g = 22f, fatPer100g = 3.8f),
                FoodItem(name = "Arroz con curiles (oriente de El Salvador)", caloriesPer100g = 158f, proteinPer100g = 9.8f, carbsPer100g = 22.8f, fatPer100g = 3f),
                FoodItem(name = "Ceviche de camarón en limón", caloriesPer100g = 98f, proteinPer100g = 16.4f, carbsPer100g = 5.6f, fatPer100g = 1.2f),
                FoodItem(name = "Ceviche de curiles con cebolla y hierbabuena", caloriesPer100g = 88f, proteinPer100g = 13.2f, carbsPer100g = 6.2f, fatPer100g = 1.1f),
                FoodItem(name = "Ceviche de pulpo", caloriesPer100g = 104f, proteinPer100g = 17.2f, carbsPer100g = 5.4f, fatPer100g = 1.6f),
                FoodItem(name = "Aguachile de camarón", caloriesPer100g = 94f, proteinPer100g = 16.2f, carbsPer100g = 5f, fatPer100g = 1.1f),
                FoodItem(name = "Ostiones frescos en su concha", caloriesPer100g = 68f, proteinPer100g = 7f, carbsPer100g = 3.9f, fatPer100g = 2.5f),
                FoodItem(name = "Almejas cocidas", caloriesPer100g = 148f, proteinPer100g = 25.6f, carbsPer100g = 5.1f, fatPer100g = 2f),
                FoodItem(name = "Mejillones al vapor", caloriesPer100g = 172f, proteinPer100g = 23.8f, carbsPer100g = 7.4f, fatPer100g = 4.5f),
                FoodItem(name = "Colas de langosta al ajillo", caloriesPer100g = 162f, proteinPer100g = 21.2f, carbsPer100g = 2f, fatPer100g = 7.6f),
                FoodItem(name = "Camarones rellenos de queso y envueltos en tocino", caloriesPer100g = 268f, proteinPer100g = 20.4f, carbsPer100g = 4.2f, fatPer100g = 18.8f),
                FoodItem(name = "Pincho de camarones y vegetales a la parrilla", caloriesPer100g = 124f, proteinPer100g = 17.8f, carbsPer100g = 5.2f, fatPer100g = 3.4f),
                FoodItem(name = "Lonja de pescado al vapor con vegetales", caloriesPer100g = 112f, proteinPer100g = 21.4f, carbsPer100g = 4.2f, fatPer100g = 1.2f),
                FoodItem(name = "Queso mozzarella rallado", caloriesPer100g = 300f, proteinPer100g = 22.2f, carbsPer100g = 2.2f, fatPer100g = 22.4f),
                FoodItem(name = "Queso cheddar amarillo", caloriesPer100g = 403f, proteinPer100g = 24.9f, carbsPer100g = 1.3f, fatPer100g = 33.1f),
                FoodItem(name = "Queso amarillo procesado (en rebanadas)", caloriesPer100g = 331f, proteinPer100g = 16.4f, carbsPer100g = 7.3f, fatPer100g = 26.5f),
                FoodItem(name = "Queso crema (untable)", caloriesPer100g = 342f, proteinPer100g = 5.9f, carbsPer100g = 4.1f, fatPer100g = 34.2f),
                FoodItem(name = "Queso crema con loroco", caloriesPer100g = 328f, proteinPer100g = 6.2f, carbsPer100g = 4.6f, fatPer100g = 32f),
                FoodItem(name = "Queso parmesano rallado", caloriesPer100g = 431f, proteinPer100g = 38.5f, carbsPer100g = 4.1f, fatPer100g = 28.6f),
                FoodItem(name = "Queso suizo", caloriesPer100g = 380f, proteinPer100g = 26.9f, carbsPer100g = 5.4f, fatPer100g = 27.8f),
                FoodItem(name = "Queso gouda", caloriesPer100g = 356f, proteinPer100g = 24.9f, carbsPer100g = 2.2f, fatPer100g = 27.4f),
                FoodItem(name = "Queso manchego", caloriesPer100g = 376f, proteinPer100g = 25f, carbsPer100g = 0.5f, fatPer100g = 30f),
                FoodItem(name = "Queso provolone", caloriesPer100g = 351f, proteinPer100g = 25.6f, carbsPer100g = 2.1f, fatPer100g = 26.6f),
                FoodItem(name = "Queso cottage bajo en grasa", caloriesPer100g = 81f, proteinPer100g = 11.1f, carbsPer100g = 4.3f, fatPer100g = 2.3f),
                FoodItem(name = "Queso ricotta", caloriesPer100g = 174f, proteinPer100g = 11.3f, carbsPer100g = 3f, fatPer100g = 13f),
                FoodItem(name = "Leche de soya natural", caloriesPer100g = 43f, proteinPer100g = 3.3f, carbsPer100g = 2.8f, fatPer100g = 1.8f),
                FoodItem(name = "Leche de almendras sin azúcar", caloriesPer100g = 15f, proteinPer100g = 0.6f, carbsPer100g = 0.6f, fatPer100g = 1.1f),
                FoodItem(name = "Leche de almendras con vainilla", caloriesPer100g = 32f, proteinPer100g = 0.5f, carbsPer100g = 5.2f, fatPer100g = 1f),
                FoodItem(name = "Leche de coco para cocinar (enlatada)", caloriesPer100g = 197f, proteinPer100g = 2f, carbsPer100g = 2.8f, fatPer100g = 21.3f),
                FoodItem(name = "Leche con chocolate comercial (en cajita)", caloriesPer100g = 83f, proteinPer100g = 3.2f, carbsPer100g = 12.4f, fatPer100g = 2.2f),
                FoodItem(name = "Leche con fresa comercial (en cajita)", caloriesPer100g = 82f, proteinPer100g = 3f, carbsPer100g = 12.8f, fatPer100g = 2f),
                FoodItem(name = "Leche con vainilla comercial (en cajita)", caloriesPer100g = 80f, proteinPer100g = 3.1f, carbsPer100g = 12.2f, fatPer100g = 2f),
                FoodItem(name = "Yogurt griego con miel", caloriesPer100g = 94f, proteinPer100g = 8.8f, carbsPer100g = 12.4f, fatPer100g = 0.8f),
                FoodItem(name = "Yogurt griego con frutos rojos", caloriesPer100g = 92f, proteinPer100g = 8.4f, carbsPer100g = 12.6f, fatPer100g = 0.8f),
                FoodItem(name = "Yogurt líquido bebible de fresa", caloriesPer100g = 78f, proteinPer100g = 2.6f, carbsPer100g = 13.8f, fatPer100g = 1.4f),
                FoodItem(name = "Yogurt líquido bebible de melocotón", caloriesPer100g = 78f, proteinPer100g = 2.6f, carbsPer100g = 13.8f, fatPer100g = 1.4f),
                FoodItem(name = "Yogurt con cereal azucarado", caloriesPer100g = 118f, proteinPer100g = 3.8f, carbsPer100g = 19.6f, fatPer100g = 2.6f),
                FoodItem(name = "Minuta salvadoreña (hielo raspado con jarabe de frutas)", caloriesPer100g = 86f, proteinPer100g = 0f, carbsPer100g = 21.5f, fatPer100g = 0f),
                FoodItem(name = "Minuta salvadoreña con leche condensada y tamarindo", caloriesPer100g = 142f, proteinPer100g = 2.2f, carbsPer100g = 28.6f, fatPer100g = 2.2f),
                FoodItem(name = "Minuta de jalea de piña y fresa", caloriesPer100g = 104f, proteinPer100g = 0.1f, carbsPer100g = 25.8f, fatPer100g = 0.1f),
                FoodItem(name = "Charamusca / Topogigio de leche con maní", caloriesPer100g = 148f, proteinPer100g = 4.6f, carbsPer100g = 20.8f, fatPer100g = 5.2f),
                FoodItem(name = "Charamusca / Topogigio de coco con leche", caloriesPer100g = 156f, proteinPer100g = 3.4f, carbsPer100g = 21.4f, fatPer100g = 6.4f),
                FoodItem(name = "Charamusca / Topogigio de tamarindo", caloriesPer100g = 76f, proteinPer100g = 0.4f, carbsPer100g = 18.6f, fatPer100g = 0.1f),
                FoodItem(name = "Charamusca / Topogigio de arrayán", caloriesPer100g = 72f, proteinPer100g = 0.3f, carbsPer100g = 17.6f, fatPer100g = 0.1f),
                FoodItem(name = "Charamusca / Topogigio de nance", caloriesPer100g = 88f, proteinPer100g = 0.6f, carbsPer100g = 20.4f, fatPer100g = 0.4f),
                FoodItem(name = "Charamusca / Topogigio de chocolate con leche", caloriesPer100g = 132f, proteinPer100g = 3.6f, carbsPer100g = 21.8f, fatPer100g = 3.4f),
                FoodItem(name = "Chocobanano salvadoreño (guineo congelado cubierto de chocolate y maní)", caloriesPer100g = 218f, proteinPer100g = 3.8f, carbsPer100g = 30.4f, fatPer100g = 9.6f),
                FoodItem(name = "Chocofresa salvadoreña", caloriesPer100g = 168f, proteinPer100g = 2.4f, carbsPer100g = 21.6f, fatPer100g = 8.2f),
                FoodItem(name = "Chocosandía salvadoreña", caloriesPer100g = 142f, proteinPer100g = 1.8f, carbsPer100g = 19.4f, fatPer100g = 6.6f),
                FoodItem(name = "Paleta de sombrilla salvadoreña (helado de fruta artesanal)", caloriesPer100g = 94f, proteinPer100g = 0.6f, carbsPer100g = 22.8f, fatPer100g = 0.2f),
                FoodItem(name = "Sorbete artesanal de carretón (coco y mamón)", caloriesPer100g = 138f, proteinPer100g = 1.8f, carbsPer100g = 24.6f, fatPer100g = 3.8f),
                FoodItem(name = "Sorbete de vainilla (helado de crema)", caloriesPer100g = 207f, proteinPer100g = 3.5f, carbsPer100g = 23.6f, fatPer100g = 11f),
                FoodItem(name = "Sorbete de chocolate (helado de crema)", caloriesPer100g = 216f, proteinPer100g = 3.8f, carbsPer100g = 28.2f, fatPer100g = 11f),
                FoodItem(name = "Sorbete de fresa (helado de crema)", caloriesPer100g = 192f, proteinPer100g = 3.2f, carbsPer100g = 24.4f, fatPer100g = 9.4f),
                FoodItem(name = "Sorbete de ron con pasas", caloriesPer100g = 224f, proteinPer100g = 3.4f, carbsPer100g = 27.6f, fatPer100g = 10.6f),
                FoodItem(name = "Sorbete de pistacho", caloriesPer100g = 228f, proteinPer100g = 4.4f, carbsPer100g = 25.4f, fatPer100g = 12.2f),
                FoodItem(name = "Sorbete de napolitano", caloriesPer100g = 204f, proteinPer100g = 3.5f, carbsPer100g = 25.4f, fatPer100g = 10.2f),
                FoodItem(name = "Cono de sorbete suave (vainilla)", caloriesPer100g = 212f, proteinPer100g = 4.2f, carbsPer100g = 31.8f, fatPer100g = 7.6f),
                FoodItem(name = "Flan de caramelo casero", caloriesPer100g = 146f, proteinPer100g = 4.6f, carbsPer100g = 22.8f, fatPer100g = 4.1f),
                FoodItem(name = "Gelatina de sabores preparada (con azúcar)", caloriesPer100g = 62f, proteinPer100g = 1.2f, carbsPer100g = 14.2f, fatPer100g = 0f),
                FoodItem(name = "Gelatina mosaico con leche condensada", caloriesPer100g = 128f, proteinPer100g = 3.2f, carbsPer100g = 22.4f, fatPer100g = 2.8f),
                FoodItem(name = "Tres leches salvadoreño (pastel)", caloriesPer100g = 286f, proteinPer100g = 6.4f, carbsPer100g = 41.2f, fatPer100g = 10.6f),
                FoodItem(name = "Pastel de chocolate con lustre", caloriesPer100g = 371f, proteinPer100g = 4.9f, carbsPer100g = 53.4f, fatPer100g = 15.6f),
                FoodItem(name = "Pastel de zanahoria con queso crema", caloriesPer100g = 385f, proteinPer100g = 4.6f, carbsPer100g = 48.2f, fatPer100g = 19.4f),
                FoodItem(name = "Cheesecake de fresa", caloriesPer100g = 321f, proteinPer100g = 5.5f, carbsPer100g = 29.8f, fatPer100g = 20.2f),
                FoodItem(name = "Tiramisú", caloriesPer100g = 284f, proteinPer100g = 5.2f, carbsPer100g = 28.6f, fatPer100g = 16.4f),
                FoodItem(name = "Pudín de chocolate", caloriesPer100g = 138f, proteinPer100g = 3.2f, carbsPer100g = 22.8f, fatPer100g = 3.8f),
                FoodItem(name = "Dulce de tamarindo con azúcar", caloriesPer100g = 312f, proteinPer100g = 1.8f, carbsPer100g = 75.4f, fatPer100g = 0.4f),
                FoodItem(name = "Dulce de tamarindo con chile y sal", caloriesPer100g = 298f, proteinPer100g = 1.9f, carbsPer100g = 71.8f, fatPer100g = 0.4f),
                FoodItem(name = "Dulce de nance", caloriesPer100g = 324f, proteinPer100g = 1.2f, carbsPer100g = 78.2f, fatPer100g = 0.8f),
                FoodItem(name = "Dulce de leche con canela (tableta de feria)", caloriesPer100g = 378f, proteinPer100g = 6.8f, carbsPer100g = 72.4f, fatPer100g = 6.8f),
                FoodItem(name = "Dulce de coco rallado (conserva blanca)", caloriesPer100g = 392f, proteinPer100g = 3.4f, carbsPer100g = 64.2f, fatPer100g = 14.2f),
                FoodItem(name = "Dulce de toronja cristalizado", caloriesPer100g = 318f, proteinPer100g = 0.6f, carbsPer100g = 78.8f, fatPer100g = 0.2f),
                FoodItem(name = "Dulce de higo cristalizado", caloriesPer100g = 326f, proteinPer100g = 1.2f, carbsPer100g = 79.4f, fatPer100g = 0.4f),
                FoodItem(name = "Dulce de chilacayote cristalizado", caloriesPer100g = 314f, proteinPer100g = 0.6f, carbsPer100g = 77.8f, fatPer100g = 0.1f),
                FoodItem(name = "Dulce de camote (camote rellenito)", caloriesPer100g = 294f, proteinPer100g = 1.4f, carbsPer100g = 71.8f, fatPer100g = 0.3f),
                FoodItem(name = "Dulce de zapote", caloriesPer100g = 308f, proteinPer100g = 1.4f, carbsPer100g = 75f, fatPer100g = 0.4f),
                FoodItem(name = "Dulce de guayaba (bocadillo / ate)", caloriesPer100g = 316f, proteinPer100g = 0.8f, carbsPer100g = 77.8f, fatPer100g = 0.2f),
                FoodItem(name = "Espumilla salvadoreña (merengue de azúcar)", caloriesPer100g = 382f, proteinPer100g = 3.8f, carbsPer100g = 91.4f, fatPer100g = 0.1f),
                FoodItem(name = "Quiebradientes / Melcocha de feria", caloriesPer100g = 386f, proteinPer100g = 0.4f, carbsPer100g = 96f, fatPer100g = 0.1f),
                FoodItem(name = "Jalea de chan con dulce de panela", caloriesPer100g = 248f, proteinPer100g = 2.4f, carbsPer100g = 56.8f, fatPer100g = 1.4f),
                FoodItem(name = "Churros españoles con azúcar y canela (de feria)", caloriesPer100g = 396f, proteinPer100g = 5.2f, carbsPer100g = 51.4f, fatPer100g = 18.8f),
                FoodItem(name = "Platanitos fritos salados (boquita en bolsa)", caloriesPer100g = 519f, proteinPer100g = 2.3f, carbsPer100g = 60.2f, fatPer100g = 30.2f),
                FoodItem(name = "Platanitos maduros fritos dulces (en bolsa)", caloriesPer100g = 502f, proteinPer100g = 2.1f, carbsPer100g = 64.8f, fatPer100g = 26.4f),
                FoodItem(name = "Yuquitas fritas crujientes (en bolsa)", caloriesPer100g = 512f, proteinPer100g = 1.8f, carbsPer100g = 62.4f, fatPer100g = 28.6f),
                FoodItem(name = "Malanguitas fritas (en bolsa)", caloriesPer100g = 508f, proteinPer100g = 2.1f, carbsPer100g = 61.8f, fatPer100g = 28.2f),
                FoodItem(name = "Tortillitas / Nachos de maíz en bolsa", caloriesPer100g = 498f, proteinPer100g = 7f, carbsPer100g = 63.4f, fatPer100g = 24.2f),
                FoodItem(name = "Boquita de maíz inflado con queso (churritos de queso)", caloriesPer100g = 514f, proteinPer100g = 6.8f, carbsPer100g = 56.4f, fatPer100g = 29f),
                FoodItem(name = "Chicharrones de harina (churritos de rueda)", caloriesPer100g = 486f, proteinPer100g = 6.4f, carbsPer100g = 61.2f, fatPer100g = 24f),
                FoodItem(name = "Piel de cerdo frita (chicharrón de bolsita)", caloriesPer100g = 544f, proteinPer100g = 61.3f, carbsPer100g = 0f, fatPer100g = 31.3f),
                FoodItem(name = "Papas fritas de bolsa (clásicas con sal)", caloriesPer100g = 536f, proteinPer100g = 7f, carbsPer100g = 52.9f, fatPer100g = 34.6f),
                FoodItem(name = "Papas fritas de bolsa con crema y especias", caloriesPer100g = 528f, proteinPer100g = 6.8f, carbsPer100g = 53.8f, fatPer100g = 33.4f),
                FoodItem(name = "Papas fritas de bolsa picantes (chile y limón)", caloriesPer100g = 524f, proteinPer100g = 6.6f, carbsPer100g = 54.2f, fatPer100g = 32.8f),
                FoodItem(name = "Taquitos de maíz enrollados picantes (boquita)", caloriesPer100g = 504f, proteinPer100g = 6.4f, carbsPer100g = 60.8f, fatPer100g = 26.2f),
                FoodItem(name = "Palomitas de maíz hechas con aceite", caloriesPer100g = 500f, proteinPer100g = 9f, carbsPer100g = 57.2f, fatPer100g = 28.1f),
                FoodItem(name = "Palomitas de maíz para microondas (sabor mantequilla)", caloriesPer100g = 482f, proteinPer100g = 8.6f, carbsPer100g = 56.4f, fatPer100g = 25.8f),
                FoodItem(name = "Palomitas de maíz acarameladas", caloriesPer100g = 431f, proteinPer100g = 3.8f, carbsPer100g = 76.8f, fatPer100g = 12.8f),
                FoodItem(name = "Sorbete de barquillo con chocolate", caloriesPer100g = 286f, proteinPer100g = 4.6f, carbsPer100g = 38.4f, fatPer100g = 12.6f),
                FoodItem(name = "Galleta María simple", caloriesPer100g = 436f, proteinPer100g = 7.6f, carbsPer100g = 75.4f, fatPer100g = 11.4f),
                FoodItem(name = "Galleta de soda salada", caloriesPer100g = 421f, proteinPer100g = 9.2f, carbsPer100g = 71.8f, fatPer100g = 10.4f),
                FoodItem(name = "Galleta rellena de crema de chocolate (tipo Oreo)", caloriesPer100g = 478f, proteinPer100g = 4.8f, carbsPer100g = 70.4f, fatPer100g = 19.6f),
                FoodItem(name = "Galleta rellena de crema de vainilla", caloriesPer100g = 482f, proteinPer100g = 4.6f, carbsPer100g = 71.2f, fatPer100g = 19.8f),
                FoodItem(name = "Galleta rellena de fresa", caloriesPer100g = 476f, proteinPer100g = 4.5f, carbsPer100g = 71.6f, fatPer100g = 19f),
                FoodItem(name = "Galleta tipo wafer de vainilla", caloriesPer100g = 512f, proteinPer100g = 4.8f, carbsPer100g = 65.4f, fatPer100g = 25.6f),
                FoodItem(name = "Galleta tipo wafer de chocolate", caloriesPer100g = 508f, proteinPer100g = 5.2f, carbsPer100g = 64.8f, fatPer100g = 25.4f),
                FoodItem(name = "Galleta con chispas de chocolate", caloriesPer100g = 488f, proteinPer100g = 5.4f, carbsPer100g = 64.2f, fatPer100g = 23.4f),
                FoodItem(name = "Galleta de avena y pasas", caloriesPer100g = 442f, proteinPer100g = 6.8f, carbsPer100g = 68.4f, fatPer100g = 15.6f),
                FoodItem(name = "Galleta de mantequilla", caloriesPer100g = 496f, proteinPer100g = 6.2f, carbsPer100g = 62.8f, fatPer100g = 24.4f),
                FoodItem(name = "Galleta integral con fibra", caloriesPer100g = 428f, proteinPer100g = 8.4f, carbsPer100g = 66.2f, fatPer100g = 14.2f),
                FoodItem(name = "Barra de granola con avena y miel", caloriesPer100g = 452f, proteinPer100g = 8.2f, carbsPer100g = 66.8f, fatPer100g = 16.8f),
                FoodItem(name = "Barra de granola con chocolate", caloriesPer100g = 464f, proteinPer100g = 7.6f, carbsPer100g = 65.4f, fatPer100g = 19.2f),
                FoodItem(name = "Chocolate con leche en barra", caloriesPer100g = 535f, proteinPer100g = 7.7f, carbsPer100g = 59.4f, fatPer100g = 29.7f),
                FoodItem(name = "Chocolate oscuro (70% cacao)", caloriesPer100g = 598f, proteinPer100g = 7.8f, carbsPer100g = 45.9f, fatPer100g = 42.6f),
                FoodItem(name = "Chocolate blanco", caloriesPer100g = 539f, proteinPer100g = 5.9f, carbsPer100g = 59.2f, fatPer100g = 32.1f),
                FoodItem(name = "Bombones de chocolate rellenos", caloriesPer100g = 512f, proteinPer100g = 5.8f, carbsPer100g = 58.6f, fatPer100g = 28.4f),
                FoodItem(name = "Malvaviscos / Angelitos (marshmallows)", caloriesPer100g = 318f, proteinPer100g = 1.8f, carbsPer100g = 81.3f, fatPer100g = 0.2f),
                FoodItem(name = "Gomitas de frutas azucaradas", caloriesPer100g = 338f, proteinPer100g = 4.2f, carbsPer100g = 78.4f, fatPer100g = 0.1f),
                FoodItem(name = "Hojuelas de maíz simples (Corn Flakes)", caloriesPer100g = 378f, proteinPer100g = 7.5f, carbsPer100g = 84.1f, fatPer100g = 0.4f),
                FoodItem(name = "Hojuelas de maíz azucaradas (Zucaritas)", caloriesPer100g = 382f, proteinPer100g = 4.6f, carbsPer100g = 89.2f, fatPer100g = 0.6f),
                FoodItem(name = "Cereal de arroz inflado con chocolate", caloriesPer100g = 386f, proteinPer100g = 5.2f, carbsPer100g = 86.4f, fatPer100g = 2.1f),
                FoodItem(name = "Cereal de aros de frutas", caloriesPer100g = 384f, proteinPer100g = 4.8f, carbsPer100g = 87.2f, fatPer100g = 1.8f),
                FoodItem(name = "Cereal de trigo integral con pasas", caloriesPer100g = 332f, proteinPer100g = 8.4f, carbsPer100g = 76.8f, fatPer100g = 1.6f),
                FoodItem(name = "Granola con frutos secos y pasas", caloriesPer100g = 471f, proteinPer100g = 10.2f, carbsPer100g = 64.4f, fatPer100g = 19.6f),
                FoodItem(name = "Almendras enteras tostadas", caloriesPer100g = 579f, proteinPer100g = 21.2f, carbsPer100g = 21.6f, fatPer100g = 49.9f),
                FoodItem(name = "Nueces de nogal (walnuts)", caloriesPer100g = 654f, proteinPer100g = 15.2f, carbsPer100g = 13.7f, fatPer100g = 65.2f),
                FoodItem(name = "Nueces pecanas", caloriesPer100g = 691f, proteinPer100g = 9.2f, carbsPer100g = 13.9f, fatPer100g = 72f),
                FoodItem(name = "Pistachos tostados con sal", caloriesPer100g = 562f, proteinPer100g = 20.2f, carbsPer100g = 27.2f, fatPer100g = 45.3f),
                FoodItem(name = "Avellanas tostadas", caloriesPer100g = 628f, proteinPer100g = 15f, carbsPer100g = 16.7f, fatPer100g = 60.8f),
                FoodItem(name = "Nueces de macadamia", caloriesPer100g = 718f, proteinPer100g = 7.9f, carbsPer100g = 13.8f, fatPer100g = 75.8f),
                FoodItem(name = "Semillas de girasol peladas", caloriesPer100g = 584f, proteinPer100g = 20.8f, carbsPer100g = 20f, fatPer100g = 51.5f),
                FoodItem(name = "Pasas oscuras sin semilla", caloriesPer100g = 299f, proteinPer100g = 3.1f, carbsPer100g = 79.2f, fatPer100g = 0.5f),
                FoodItem(name = "Ciruelas pasas", caloriesPer100g = 240f, proteinPer100g = 2.2f, carbsPer100g = 63.9f, fatPer100g = 0.4f),
                FoodItem(name = "Arándanos deshidratados (cranberries)", caloriesPer100g = 308f, proteinPer100g = 0.2f, carbsPer100g = 82.8f, fatPer100g = 1.1f),
                FoodItem(name = "Dátiles sin hueso", caloriesPer100g = 282f, proteinPer100g = 2.5f, carbsPer100g = 75f, fatPer100g = 0.4f),
                FoodItem(name = "Coco rallado deshidratado sin azúcar", caloriesPer100g = 660f, proteinPer100g = 6.9f, carbsPer100g = 23.7f, fatPer100g = 64.5f),
                FoodItem(name = "Mantequilla de maní cremosa", caloriesPer100g = 588f, proteinPer100g = 25.1f, carbsPer100g = 20f, fatPer100g = 50.4f),
                FoodItem(name = "Mantequilla de almendras", caloriesPer100g = 614f, proteinPer100g = 21f, carbsPer100g = 18.8f, fatPer100g = 55.5f),
                FoodItem(name = "Crema de avellanas con cacao (tipo Nutella)", caloriesPer100g = 539f, proteinPer100g = 6.3f, carbsPer100g = 57.5f, fatPer100g = 30.9f),
                FoodItem(name = "Jalea / Mermelada de fresa", caloriesPer100g = 278f, proteinPer100g = 0.4f, carbsPer100g = 68.9f, fatPer100g = 0.1f),
                FoodItem(name = "Jalea / Mermelada de piña salvadoreña", caloriesPer100g = 274f, proteinPer100g = 0.4f, carbsPer100g = 68f, fatPer100g = 0.1f),
                FoodItem(name = "Jalea / Mermelada de guayaba", caloriesPer100g = 282f, proteinPer100g = 0.5f, carbsPer100g = 69.8f, fatPer100g = 0.1f),
                FoodItem(name = "Jalea / Mermelada de naranja", caloriesPer100g = 272f, proteinPer100g = 0.3f, carbsPer100g = 67.5f, fatPer100g = 0.1f),
                FoodItem(name = "Miel de maple / Jarabe para panqueques", caloriesPer100g = 268f, proteinPer100g = 0f, carbsPer100g = 67f, fatPer100g = 0f),
                FoodItem(name = "Azúcar blanca refinada", caloriesPer100g = 387f, proteinPer100g = 0f, carbsPer100g = 100f, fatPer100g = 0f),
                FoodItem(name = "Azúcar morena (cruda)", caloriesPer100g = 380f, proteinPer100g = 0.1f, carbsPer100g = 98.1f, fatPer100g = 0f),
                FoodItem(name = "Aceite vegetal (maíz / soya / girasol)", caloriesPer100g = 884f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 100f),
                FoodItem(name = "Aceite de oliva extra virgen", caloriesPer100g = 884f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 100f),
                FoodItem(name = "Aceite de coco virgen", caloriesPer100g = 862f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 100f),
                FoodItem(name = "Manteca de cerdo", caloriesPer100g = 902f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 100f),
                FoodItem(name = "Manteca vegetal hidrogenada", caloriesPer100g = 884f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 100f),
                FoodItem(name = "Mayonesa regular", caloriesPer100g = 680f, proteinPer100g = 1f, carbsPer100g = 0.6f, fatPer100g = 75f),
                FoodItem(name = "Mayonesa reducida en grasa (light)", caloriesPer100g = 324f, proteinPer100g = 0.9f, carbsPer100g = 8.6f, fatPer100g = 31.2f),
                FoodItem(name = "Mayonesa con limón", caloriesPer100g = 665f, proteinPer100g = 1f, carbsPer100g = 1.4f, fatPer100g = 73f),
                FoodItem(name = "Salsa de tomate tipo Ketchup", caloriesPer100g = 112f, proteinPer100g = 1.2f, carbsPer100g = 27.4f, fatPer100g = 0.1f),
                FoodItem(name = "Mostaza preparada amarilla", caloriesPer100g = 66f, proteinPer100g = 4.4f, carbsPer100g = 5.3f, fatPer100g = 3.3f),
                FoodItem(name = "Salsa inglesa (Worcestershire)", caloriesPer100g = 78f, proteinPer100g = 0f, carbsPer100g = 19.5f, fatPer100g = 0f),
                FoodItem(name = "Salsa de soya", caloriesPer100g = 53f, proteinPer100g = 8.1f, carbsPer100g = 4.9f, fatPer100g = 0.6f),
                FoodItem(name = "Salsa negra salvadoreña (para carnes y cocteles)", caloriesPer100g = 72f, proteinPer100g = 1.2f, carbsPer100g = 16.8f, fatPer100g = 0.1f),
                FoodItem(name = "Salsa picante roja (tipo Tabasco / chiltepe)", caloriesPer100g = 32f, proteinPer100g = 1.3f, carbsPer100g = 5.8f, fatPer100g = 0.5f),
                FoodItem(name = "Salsa barbacoa (BBQ)", caloriesPer100g = 172f, proteinPer100g = 0.8f, carbsPer100g = 40.8f, fatPer100g = 0.6f),
                FoodItem(name = "Salsa rosada / Mil Islas", caloriesPer100g = 370f, proteinPer100g = 1.1f, carbsPer100g = 15.2f, fatPer100g = 34f),
                FoodItem(name = "Aderezo Ranch", caloriesPer100g = 430f, proteinPer100g = 1.3f, carbsPer100g = 5.9f, fatPer100g = 44.5f),
                FoodItem(name = "Aderezo César", caloriesPer100g = 482f, proteinPer100g = 2.4f, carbsPer100g = 3.6f, fatPer100g = 50.2f),
                FoodItem(name = "Aderezo italiano (vinagreta)", caloriesPer100g = 236f, proteinPer100g = 0.4f, carbsPer100g = 10.2f, fatPer100g = 21.8f),
                FoodItem(name = "Vinagre blanco / de manzana", caloriesPer100g = 19f, proteinPer100g = 0f, carbsPer100g = 0.9f, fatPer100g = 0f),
                FoodItem(name = "Vinagre de piña artesanal salvadoreño", caloriesPer100g = 22f, proteinPer100g = 0.1f, carbsPer100g = 2.4f, fatPer100g = 0f),
                FoodItem(name = "Consomé de pollo en polvo", caloriesPer100g = 214f, proteinPer100g = 14.2f, carbsPer100g = 24.8f, fatPer100g = 6.4f),
                FoodItem(name = "Consomé de res en polvo", caloriesPer100g = 208f, proteinPer100g = 13.8f, carbsPer100g = 25.2f, fatPer100g = 5.8f),
                FoodItem(name = "Consomé de camarón en polvo", caloriesPer100g = 218f, proteinPer100g = 14.6f, carbsPer100g = 24.4f, fatPer100g = 6.8f),
                FoodItem(name = "Sazonador completo en polvo", caloriesPer100g = 192f, proteinPer100g = 8.4f, carbsPer100g = 32.6f, fatPer100g = 3.2f),
                FoodItem(name = "Ajo fresco pelado", caloriesPer100g = 149f, proteinPer100g = 6.4f, carbsPer100g = 33.1f, fatPer100g = 0.5f),
                FoodItem(name = "Pollo campero / Pollo frito crujiente (pechuga)", caloriesPer100g = 264f, proteinPer100g = 24.2f, carbsPer100g = 9.8f, fatPer100g = 14f),
                FoodItem(name = "Pollo campero / Pollo frito crujiente (pierna)", caloriesPer100g = 252f, proteinPer100g = 22.8f, carbsPer100g = 8.6f, fatPer100g = 14.2f),
                FoodItem(name = "Pollo campero / Pollo frito crujiente (muslo)", caloriesPer100g = 284f, proteinPer100g = 21.6f, carbsPer100g = 9.2f, fatPer100g = 18f),
                FoodItem(name = "Pollo campero / Pollo frito crujiente (ala)", caloriesPer100g = 302f, proteinPer100g = 22.4f, carbsPer100g = 8.8f, fatPer100g = 19.8f),
                FoodItem(name = "Camperitos / Nuggets de pollo empanizados", caloriesPer100g = 296f, proteinPer100g = 15.8f, carbsPer100g = 16.4f, fatPer100g = 18.6f),
                FoodItem(name = "Tiras de pollo empanizadas (chicken fingers)", caloriesPer100g = 278f, proteinPer100g = 19.2f, carbsPer100g = 16.8f, fatPer100g = 14.8f),
                FoodItem(name = "Hamburguesa sencilla de res con queso", caloriesPer100g = 268f, proteinPer100g = 14.2f, carbsPer100g = 24.8f, fatPer100g = 12.4f),
                FoodItem(name = "Hamburguesa doble con queso y tocino", caloriesPer100g = 294f, proteinPer100g = 16.8f, carbsPer100g = 20.4f, fatPer100g = 16.2f),
                FoodItem(name = "Hamburguesa de pollo empanizado", caloriesPer100g = 256f, proteinPer100g = 13.4f, carbsPer100g = 26.2f, fatPer100g = 10.8f),
                FoodItem(name = "Hot dog / Perro caliente tradicional", caloriesPer100g = 262f, proteinPer100g = 10.4f, carbsPer100g = 24.2f, fatPer100g = 13.8f),
                FoodItem(name = "Hot dog salvadoreño con repollo, escabeche y salsa", caloriesPer100g = 238f, proteinPer100g = 9.6f, carbsPer100g = 25.4f, fatPer100g = 11f),
                FoodItem(name = "Pizza de queso (masa tradicional)", caloriesPer100g = 266f, proteinPer100g = 11.4f, carbsPer100g = 33.6f, fatPer100g = 9.8f),
                FoodItem(name = "Pizza de pepperoni", caloriesPer100g = 284f, proteinPer100g = 12.2f, carbsPer100g = 31.8f, fatPer100g = 12f),
                FoodItem(name = "Pizza suprema (carne, pepperoni, chile verde y cebolla)", caloriesPer100g = 258f, proteinPer100g = 11.6f, carbsPer100g = 29.4f, fatPer100g = 10.4f),
                FoodItem(name = "Pizza hawaiana (jamón y piña)", caloriesPer100g = 248f, proteinPer100g = 11f, carbsPer100g = 32.4f, fatPer100g = 8.4f),
                FoodItem(name = "Pizza de pollo con barbacoa", caloriesPer100g = 254f, proteinPer100g = 12.6f, carbsPer100g = 31.8f, fatPer100g = 8.6f),
                FoodItem(name = "Pizza con orilla rellena de queso", caloriesPer100g = 288f, proteinPer100g = 12.8f, carbsPer100g = 32f, fatPer100g = 12.2f),
                FoodItem(name = "Pan con ajo y queso gratinado", caloriesPer100g = 336f, proteinPer100g = 11.2f, carbsPer100g = 38.4f, fatPer100g = 15.2f),
                FoodItem(name = "Calzone relleno de jamón y queso", caloriesPer100g = 272f, proteinPer100g = 12.4f, carbsPer100g = 31.6f, fatPer100g = 10.8f),
                FoodItem(name = "Taco de carne asada en tortilla de maíz", caloriesPer100g = 214f, proteinPer100g = 13.8f, carbsPer100g = 21.2f, fatPer100g = 8.2f),
                FoodItem(name = "Taco de pollo en tortilla de maíz", caloriesPer100g = 196f, proteinPer100g = 13.4f, carbsPer100g = 21.6f, fatPer100g = 6.2f),
                FoodItem(name = "Taco al pastor con piña", caloriesPer100g = 218f, proteinPer100g = 12.8f, carbsPer100g = 22.4f, fatPer100g = 8.6f),
                FoodItem(name = "Taco de lengua", caloriesPer100g = 224f, proteinPer100g = 13.2f, carbsPer100g = 20.8f, fatPer100g = 9.8f),
                FoodItem(name = "Taco de birria con queso (quesabirria)", caloriesPer100g = 268f, proteinPer100g = 15.4f, carbsPer100g = 19.8f, fatPer100g = 14.2f),
                FoodItem(name = "Tacos dorados / Flautas de pollo con crema y queso", caloriesPer100g = 252f, proteinPer100g = 10.8f, carbsPer100g = 26.4f, fatPer100g = 11.4f),
                FoodItem(name = "Torta mexicana de carne asada (estilo salvadoreño)", caloriesPer100g = 236f, proteinPer100g = 12.8f, carbsPer100g = 26.8f, fatPer100g = 8.6f),
                FoodItem(name = "Torta de jamón, queso y aguacate", caloriesPer100g = 228f, proteinPer100g = 10.6f, carbsPer100g = 27.4f, fatPer100g = 8.4f),
                FoodItem(name = "Torta de pastor", caloriesPer100g = 242f, proteinPer100g = 12.4f, carbsPer100g = 26.6f, fatPer100g = 9.4f),
                FoodItem(name = "Burrito de carne asada, frijol y arroz", caloriesPer100g = 212f, proteinPer100g = 10.8f, carbsPer100g = 26.4f, fatPer100g = 7f),
                FoodItem(name = "Burrito de pollo con frijol y queso", caloriesPer100g = 204f, proteinPer100g = 11.2f, carbsPer100g = 26f, fatPer100g = 6.2f),
                FoodItem(name = "Quesadilla mexicana en tortilla de harina con carne", caloriesPer100g = 286f, proteinPer100g = 14.6f, carbsPer100g = 24.8f, fatPer100g = 14.2f),
                FoodItem(name = "Quesadilla en tortilla de harina solo queso", caloriesPer100g = 304f, proteinPer100g = 13.8f, carbsPer100g = 26.2f, fatPer100g = 16f),
                FoodItem(name = "Gringa de carne al pastor y queso en tortilla de harina", caloriesPer100g = 292f, proteinPer100g = 14.2f, carbsPer100g = 25.4f, fatPer100g = 14.8f),
                FoodItem(name = "Nachos con queso derretido y jalapeño", caloriesPer100g = 346f, proteinPer100g = 8.8f, carbsPer100g = 38.4f, fatPer100g = 17.6f),
                FoodItem(name = "Nachos supremos con carne molida, frijol, crema y guacamole", caloriesPer100g = 268f, proteinPer100g = 10.4f, carbsPer100g = 27.8f, fatPer100g = 12.8f),
                FoodItem(name = "Alambre de res con tocino, chile verde y queso", caloriesPer100g = 228f, proteinPer100g = 18.4f, carbsPer100g = 6.2f, fatPer100g = 14.4f),
                FoodItem(name = "Fajitas de res con chile verde y cebolla", caloriesPer100g = 168f, proteinPer100g = 17.6f, carbsPer100g = 6.8f, fatPer100g = 7.8f),
                FoodItem(name = "Fajitas de pollo con chile verde y cebolla", caloriesPer100g = 148f, proteinPer100g = 18.8f, carbsPer100g = 6.6f, fatPer100g = 5.2f),
                FoodItem(name = "Fajitas mixtas (res, pollo y camarón)", caloriesPer100g = 158f, proteinPer100g = 18.6f, carbsPer100g = 6.4f, fatPer100g = 6.4f),
                FoodItem(name = "Sándwich de pollo desmenuzado con mayonesa", caloriesPer100g = 242f, proteinPer100g = 12.4f, carbsPer100g = 26.8f, fatPer100g = 9.4f),
                FoodItem(name = "Sándwich de jamón y queso en pan de caja", caloriesPer100g = 252f, proteinPer100g = 12.6f, carbsPer100g = 28.4f, fatPer100g = 9.6f),
                FoodItem(name = "Club sándwich con pollo, tocino, huevo y papas", caloriesPer100g = 268f, proteinPer100g = 13.8f, carbsPer100g = 26.2f, fatPer100g = 12f),
                FoodItem(name = "Sándwich de atún con mayonesa", caloriesPer100g = 234f, proteinPer100g = 12.8f, carbsPer100g = 26.4f, fatPer100g = 8.4f),
                FoodItem(name = "Papas fritas con queso cheddar y tocino", caloriesPer100g = 338f, proteinPer100g = 7.8f, carbsPer100g = 34.2f, fatPer100g = 19f),
                FoodItem(name = "Aros de cebolla empanizados fritos", caloriesPer100g = 332f, proteinPer100g = 4.4f, carbsPer100g = 38.6f, fatPer100g = 17.8f),
                FoodItem(name = "Dedos de queso mozzarella empanizados", caloriesPer100g = 328f, proteinPer100g = 14.8f, carbsPer100g = 24.2f, fatPer100g = 19.2f),
                FoodItem(name = "Empanadas chilenas / argentinas de carne horneadas", caloriesPer100g = 268f, proteinPer100g = 10.2f, carbsPer100g = 28.6f, fatPer100g = 12.4f),
                FoodItem(name = "Sushi roll California (cangrejo, aguacate y pepino)", caloriesPer100g = 142f, proteinPer100g = 4.8f, carbsPer100g = 24.6f, fatPer100g = 2.8f),
                FoodItem(name = "Sushi roll empanizado con queso crema y camarón", caloriesPer100g = 196f, proteinPer100g = 6.8f, carbsPer100g = 26.8f, fatPer100g = 6.8f),
                FoodItem(name = "Tacos chinos / Rollitos primavera fritos", caloriesPer100g = 248f, proteinPer100g = 6.8f, carbsPer100g = 28.4f, fatPer100g = 11.8f),
                FoodItem(name = "Wantán frito con cerdo", caloriesPer100g = 292f, proteinPer100g = 9.4f, carbsPer100g = 31.2f, fatPer100g = 14.4f),
                FoodItem(name = "Gaseosa de cola regular", caloriesPer100g = 42f, proteinPer100g = 0f, carbsPer100g = 10.6f, fatPer100g = 0f),
                FoodItem(name = "Gaseosa de cola sin azúcar (Zero / Light)", caloriesPer100g = 0f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 0f),
                FoodItem(name = "Gaseosa sabor naranja", caloriesPer100g = 48f, proteinPer100g = 0f, carbsPer100g = 12f, fatPer100g = 0f),
                FoodItem(name = "Gaseosa sabor lima-limón", caloriesPer100g = 41f, proteinPer100g = 0f, carbsPer100g = 10.2f, fatPer100g = 0f),
                FoodItem(name = "Gaseosa sabor uva", caloriesPer100g = 49f, proteinPer100g = 0f, carbsPer100g = 12.2f, fatPer100g = 0f),
                FoodItem(name = "Gaseosa sabor fresa / roja", caloriesPer100g = 47f, proteinPer100g = 0f, carbsPer100g = 11.8f, fatPer100g = 0f),
                FoodItem(name = "Té frío de limón embotellado", caloriesPer100g = 34f, proteinPer100g = 0f, carbsPer100g = 8.5f, fatPer100g = 0f),
                FoodItem(name = "Té frío de melocotón embotellado", caloriesPer100g = 36f, proteinPer100g = 0f, carbsPer100g = 9f, fatPer100g = 0f),
                FoodItem(name = "Jugo de naranja natural recién exprimido", caloriesPer100g = 45f, proteinPer100g = 0.7f, carbsPer100g = 10.4f, fatPer100g = 0.2f),
                FoodItem(name = "Jugo de naranja con zanahoria natural", caloriesPer100g = 43f, proteinPer100g = 0.8f, carbsPer100g = 9.8f, fatPer100g = 0.2f),
                FoodItem(name = "Jugo verde (piña, apio, pepino y espinaca)", caloriesPer100g = 32f, proteinPer100g = 0.7f, carbsPer100g = 7.4f, fatPer100g = 0.1f),
                FoodItem(name = "Jugo de tomate preparado", caloriesPer100g = 21f, proteinPer100g = 0.8f, carbsPer100g = 4.4f, fatPer100g = 0.1f),
                FoodItem(name = "Néctar de manzana en lata / cajita", caloriesPer100g = 48f, proteinPer100g = 0.1f, carbsPer100g = 11.9f, fatPer100g = 0.1f),
                FoodItem(name = "Néctar de pera en lata / cajita", caloriesPer100g = 50f, proteinPer100g = 0.1f, carbsPer100g = 12.4f, fatPer100g = 0.1f),
                FoodItem(name = "Néctar de melocotón en lata / cajita", caloriesPer100g = 52f, proteinPer100g = 0.2f, carbsPer100g = 12.8f, fatPer100g = 0.1f),
                FoodItem(name = "Néctar de piña en lata / cajita", caloriesPer100g = 51f, proteinPer100g = 0.2f, carbsPer100g = 12.6f, fatPer100g = 0.1f),
                FoodItem(name = "Néctar de guayaba en cajita", caloriesPer100g = 54f, proteinPer100g = 0.3f, carbsPer100g = 13.2f, fatPer100g = 0.1f),
                FoodItem(name = "Néctar de mango en cajita", caloriesPer100g = 54f, proteinPer100g = 0.2f, carbsPer100g = 13.3f, fatPer100g = 0.1f),
                FoodItem(name = "Licuado de guineo con leche y azúcar", caloriesPer100g = 92f, proteinPer100g = 3.1f, carbsPer100g = 16.4f, fatPer100g = 1.8f),
                FoodItem(name = "Licuado de fresa con leche", caloriesPer100g = 84f, proteinPer100g = 2.9f, carbsPer100g = 14.6f, fatPer100g = 1.7f),
                FoodItem(name = "Licuado de zapote con leche (típico salvadoreño)", caloriesPer100g = 108f, proteinPer100g = 3.2f, carbsPer100g = 20.4f, fatPer100g = 1.8f),
                FoodItem(name = "Licuado de papaya con leche", caloriesPer100g = 78f, proteinPer100g = 2.8f, carbsPer100g = 13.4f, fatPer100g = 1.6f),
                FoodItem(name = "Licuado de melón con leche", caloriesPer100g = 76f, proteinPer100g = 2.8f, carbsPer100g = 12.8f, fatPer100g = 1.6f),
                FoodItem(name = "Licuado de avena con guineo y leche", caloriesPer100g = 112f, proteinPer100g = 4.2f, carbsPer100g = 19.6f, fatPer100g = 2.1f),
                FoodItem(name = "Licuado de mamey con leche", caloriesPer100g = 88f, proteinPer100g = 2.8f, carbsPer100g = 15.8f, fatPer100g = 1.7f),
                FoodItem(name = "Licuado de guanábana con leche", caloriesPer100g = 92f, proteinPer100g = 2.9f, carbsPer100g = 16.8f, fatPer100g = 1.7f),
                FoodItem(name = "Licuado de mora con leche", caloriesPer100g = 82f, proteinPer100g = 2.9f, carbsPer100g = 14.2f, fatPer100g = 1.7f),
                FoodItem(name = "Café capuchino con leche entera", caloriesPer100g = 58f, proteinPer100g = 3f, carbsPer100g = 5.2f, fatPer100g = 2.8f),
                FoodItem(name = "Café latte con leche entera", caloriesPer100g = 54f, proteinPer100g = 2.9f, carbsPer100g = 4.8f, fatPer100g = 2.6f),
                FoodItem(name = "Café mocaccino con chocolate y leche", caloriesPer100g = 78f, proteinPer100g = 3.1f, carbsPer100g = 10.8f, fatPer100g = 2.6f),
                FoodItem(name = "Café helado (frappé de café con crema batida)", caloriesPer100g = 124f, proteinPer100g = 2.8f, carbsPer100g = 18.6f, fatPer100g = 4.4f),
                FoodItem(name = "Bebida hidratante deportiva (isotónica)", caloriesPer100g = 24f, proteinPer100g = 0f, carbsPer100g = 6f, fatPer100g = 0f),
                FoodItem(name = "Bebida hidratante sin azúcar", caloriesPer100g = 2f, proteinPer100g = 0f, carbsPer100g = 0.4f, fatPer100g = 0f),
                FoodItem(name = "Bebida energética regular", caloriesPer100g = 45f, proteinPer100g = 0.3f, carbsPer100g = 11f, fatPer100g = 0f),
                FoodItem(name = "Cerveza lager nacional (tipo Pilsener / Suprema)", caloriesPer100g = 43f, proteinPer100g = 0.5f, carbsPer100g = 3.6f, fatPer100g = 0f),
                FoodItem(name = "Cerveza ligera (tipo Golden / Light)", caloriesPer100g = 29f, proteinPer100g = 0.3f, carbsPer100g = 1.8f, fatPer100g = 0f),
                FoodItem(name = "Michelada salvadoreña con limón, salsa inglesa y sal", caloriesPer100g = 46f, proteinPer100g = 0.6f, carbsPer100g = 4.6f, fatPer100g = 0.1f),
                FoodItem(name = "Michelada con clamato y camarón", caloriesPer100g = 54f, proteinPer100g = 1.8f, carbsPer100g = 6.4f, fatPer100g = 0.2f),
                FoodItem(name = "Chaparro salvadoreño (aguardiente artesanal de maíz)", caloriesPer100g = 231f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 0f),
                FoodItem(name = "Chicha salvadoreña fermentada de maíz y piña", caloriesPer100g = 68f, proteinPer100g = 0.4f, carbsPer100g = 10.2f, fatPer100g = 0.1f),
                FoodItem(name = "Ron blanco / añejo", caloriesPer100g = 231f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 0f),
                FoodItem(name = "Vino tinto de mesa", caloriesPer100g = 85f, proteinPer100g = 0.1f, carbsPer100g = 2.6f, fatPer100g = 0f),
                FoodItem(name = "Vino blanco de mesa", caloriesPer100g = 82f, proteinPer100g = 0.1f, carbsPer100g = 2.6f, fatPer100g = 0f),
                FoodItem(name = "Huevo revuelto con jamón", caloriesPer100g = 174f, proteinPer100g = 13.2f, carbsPer100g = 1.6f, fatPer100g = 12.6f),
                FoodItem(name = "Huevo revuelto con salchicha", caloriesPer100g = 198f, proteinPer100g = 12.4f, carbsPer100g = 2.1f, fatPer100g = 15.4f),
                FoodItem(name = "Huevo revuelto con tocino", caloriesPer100g = 224f, proteinPer100g = 14.8f, carbsPer100g = 1.2f, fatPer100g = 17.6f),
                FoodItem(name = "Huevo revuelto con queso fresco", caloriesPer100g = 188f, proteinPer100g = 14.2f, carbsPer100g = 1.8f, fatPer100g = 13.8f),
                FoodItem(name = "Huevo revuelto con quesillo", caloriesPer100g = 204f, proteinPer100g = 14.6f, carbsPer100g = 2f, fatPer100g = 15.2f),
                FoodItem(name = "Huevo revuelto con hongos y cebolla", caloriesPer100g = 148f, proteinPer100g = 11.2f, carbsPer100g = 2.8f, fatPer100g = 10.2f),
                FoodItem(name = "Huevo revuelto con espinaca", caloriesPer100g = 142f, proteinPer100g = 11.4f, carbsPer100g = 2.2f, fatPer100g = 9.8f),
                FoodItem(name = "Huevo revuelto con ayote tierno", caloriesPer100g = 136f, proteinPer100g = 10.4f, carbsPer100g = 3.2f, fatPer100g = 9.2f),
                FoodItem(name = "Huevo revuelto con pipián", caloriesPer100g = 134f, proteinPer100g = 10.4f, carbsPer100g = 3f, fatPer100g = 9.1f),
                FoodItem(name = "Huevo revuelto con güisquil", caloriesPer100g = 136f, proteinPer100g = 10.2f, carbsPer100g = 3.4f, fatPer100g = 9.1f),
                FoodItem(name = "Huevo revuelto con flor de izote", caloriesPer100g = 140f, proteinPer100g = 10.8f, carbsPer100g = 3.2f, fatPer100g = 9.4f),
                FoodItem(name = "Huevo revuelto con flor de ayote", caloriesPer100g = 132f, proteinPer100g = 10.2f, carbsPer100g = 2.8f, fatPer100g = 9f),
                FoodItem(name = "Huevo revuelto con pito", caloriesPer100g = 142f, proteinPer100g = 11f, carbsPer100g = 3.4f, fatPer100g = 9.4f),
                FoodItem(name = "Huevo revuelto con pacaya", caloriesPer100g = 144f, proteinPer100g = 11.2f, carbsPer100g = 3.2f, fatPer100g = 9.6f),
                FoodItem(name = "Huevo revuelto con papa", caloriesPer100g = 152f, proteinPer100g = 10.2f, carbsPer100g = 8.6f, fatPer100g = 8.6f),
                FoodItem(name = "Huevo revuelto con plátano maduro", caloriesPer100g = 168f, proteinPer100g = 9.8f, carbsPer100g = 12.4f, fatPer100g = 8.8f),
                FoodItem(name = "Huevo revuelto con tortilla frita (migas con huevo)", caloriesPer100g = 212f, proteinPer100g = 10.6f, carbsPer100g = 16.8f, fatPer100g = 11.4f),
                FoodItem(name = "Huevo en salsa de tomate criolla", caloriesPer100g = 138f, proteinPer100g = 10.2f, carbsPer100g = 4.8f, fatPer100g = 8.6f),
                FoodItem(name = "Huevo en alguashte", caloriesPer100g = 186f, proteinPer100g = 13.8f, carbsPer100g = 4.6f, fatPer100g = 12.6f),
                FoodItem(name = "Huevo duro con alguashte y limón", caloriesPer100g = 178f, proteinPer100g = 14.2f, carbsPer100g = 3.2f, fatPer100g = 12.2f),
                FoodItem(name = "Omelette de claras con espinaca y queso fresco", caloriesPer100g = 98f, proteinPer100g = 13.4f, carbsPer100g = 2.4f, fatPer100g = 3.8f),
                FoodItem(name = "Omelette de loroco y quesillo", caloriesPer100g = 208f, proteinPer100g = 14.4f, carbsPer100g = 2.6f, fatPer100g = 15.6f),
                FoodItem(name = "Omelette de chorizo y queso", caloriesPer100g = 236f, proteinPer100g = 14.8f, carbsPer100g = 2.2f, fatPer100g = 18.6f),
                FoodItem(name = "Omelette de vegetales (chile verde, cebolla, tomate y hongos)", caloriesPer100g = 152f, proteinPer100g = 11.2f, carbsPer100g = 3.8f, fatPer100g = 10.2f),
                FoodItem(name = "Tamal de gallina con aceituna y alcaparra", caloriesPer100g = 212f, proteinPer100g = 8.4f, carbsPer100g = 22.2f, fatPer100g = 9.8f),
                FoodItem(name = "Tamal de pollo con papa y ejote", caloriesPer100g = 194f, proteinPer100g = 7.6f, carbsPer100g = 23f, fatPer100g = 7.9f),
                FoodItem(name = "Tamal de cerdo con ciruela pasa", caloriesPer100g = 224f, proteinPer100g = 7.5f, carbsPer100g = 23.8f, fatPer100g = 11f),
                FoodItem(name = "Tamal de costilla de cerdo", caloriesPer100g = 228f, proteinPer100g = 8.2f, carbsPer100g = 21.6f, fatPer100g = 12f),
                FoodItem(name = "Tamal de loroco con queso", caloriesPer100g = 196f, proteinPer100g = 6.6f, carbsPer100g = 26.4f, fatPer100g = 7f),
                FoodItem(name = "Tamal de mora con queso", caloriesPer100g = 190f, proteinPer100g = 6.4f, carbsPer100g = 26.2f, fatPer100g = 6.6f),
                FoodItem(name = "Tamal de elote con crema", caloriesPer100g = 198f, proteinPer100g = 4.6f, carbsPer100g = 28.8f, fatPer100g = 7.2f),
                FoodItem(name = "Tamal de elote relleno de chicharrón", caloriesPer100g = 226f, proteinPer100g = 6.8f, carbsPer100g = 28.4f, fatPer100g = 9.6f),
                FoodItem(name = "Tamal de elote relleno de queso", caloriesPer100g = 214f, proteinPer100g = 6.2f, carbsPer100g = 28.6f, fatPer100g = 8.4f),
                FoodItem(name = "Tamal de cambray salvadoreño", caloriesPer100g = 218f, proteinPer100g = 5.8f, carbsPer100g = 32.4f, fatPer100g = 7.2f),
                FoodItem(name = "Tamal de pescado seco", caloriesPer100g = 204f, proteinPer100g = 10.4f, carbsPer100g = 24.2f, fatPer100g = 7.2f),
                FoodItem(name = "Tamal de chipilín con pollo", caloriesPer100g = 202f, proteinPer100g = 8.8f, carbsPer100g = 24.6f, fatPer100g = 7.6f),
                FoodItem(name = "Tamal de frijol con chicharrón", caloriesPer100g = 212f, proteinPer100g = 7.8f, carbsPer100g = 27.6f, fatPer100g = 7.8f),
                FoodItem(name = "Tamal de maicillo", caloriesPer100g = 184f, proteinPer100g = 5.4f, carbsPer100g = 30.2f, fatPer100g = 4.6f),
                FoodItem(name = "Desayuno típico salvadoreño (huevo picado, frijoles volteados, plátano, crema y queso)", caloriesPer100g = 194f, proteinPer100g = 7.8f, carbsPer100g = 21.8f, fatPer100g = 8.4f),
                FoodItem(name = "Desayuno con huevo estrellado, casamiento, aguacate y cuajada", caloriesPer100g = 182f, proteinPer100g = 8.2f, carbsPer100g = 18.6f, fatPer100g = 8.2f),
                FoodItem(name = "Desayuno de tamal de pollo con pan francés y crema", caloriesPer100g = 228f, proteinPer100g = 7.2f, carbsPer100g = 31.4f, fatPer100g = 8.2f),
                FoodItem(name = "Desayuno de tamal pisque con crema y queso fresco", caloriesPer100g = 208f, proteinPer100g = 7.6f, carbsPer100g = 26.4f, fatPer100g = 8f),
                FoodItem(name = "Cena típica salvadoreña (plátano frito, frijoles en bala, huevo duro y crema)", caloriesPer100g = 178f, proteinPer100g = 6.8f, carbsPer100g = 23.4f, fatPer100g = 6.4f),
                FoodItem(name = "Cena de casamiento con chorizo asado, aguacate y queso duro", caloriesPer100g = 216f, proteinPer100g = 9.6f, carbsPer100g = 19.4f, fatPer100g = 11.2f),
                FoodItem(name = "Cena de riguas con cuajada y crema", caloriesPer100g = 204f, proteinPer100g = 7.4f, carbsPer100g = 25.8f, fatPer100g = 7.8f),
                FoodItem(name = "Almuerzo de carne asada con casamiento, chirmol y cuajada", caloriesPer100g = 186f, proteinPer100g = 14.2f, carbsPer100g = 16.4f, fatPer100g = 7.2f),
                FoodItem(name = "Almuerzo de pollo guisado con arroz y ensalada fresca", caloriesPer100g = 142f, proteinPer100g = 12.8f, carbsPer100g = 15.2f, fatPer100g = 3.4f),
                FoodItem(name = "Almuerzo de pescado frito con arroz y curtido de cebolla", caloriesPer100g = 174f, proteinPer100g = 14.6f, carbsPer100g = 16.8f, fatPer100g = 5.4f),
                FoodItem(name = "Almuerzo de tortitas de carne con arroz y frijoles", caloriesPer100g = 168f, proteinPer100g = 10.8f, carbsPer100g = 19.6f, fatPer100g = 5.2f),
                FoodItem(name = "Almuerzo de rellenos de güisquil con arroz blanco", caloriesPer100g = 146f, proteinPer100g = 5.2f, carbsPer100g = 19.8f, fatPer100g = 5f),
                FoodItem(name = "Almuerzo de rellenos de pacaya con arroz y ensalada", caloriesPer100g = 142f, proteinPer100g = 5.4f, carbsPer100g = 18.4f, fatPer100g = 5.2f),
                FoodItem(name = "Almuerzo de bistec encebollado con arroz y frijoles", caloriesPer100g = 164f, proteinPer100g = 13.4f, carbsPer100g = 17.6f, fatPer100g = 4.6f),
                )
        val existingNames = dao.getAllFoodsSync().map { it.name.trim().lowercase() }.toHashSet()
        val missingFoods = defaultFoods.filter { it.name.trim().lowercase() !in existingNames }
        if (missingFoods.isNotEmpty()) {
            dao.insertFoods(missingFoods)
            Log.d(TAG, "Inserted ${missingFoods.size} new foods into local catalog (total default: ${defaultFoods.size})")
        }

        // Initialize User Nutrition Config if not present
        if (dao.getUserConfigSync() == null) {
            val initialConfig = UserNutritionConfig(
                id = 1,
                sex = "MALE",
                weightKg = 0f,
                targetWeightKg = 0f,
                autoSyncWeight = true,
                heightCm = 0,
                age = 0,
                activityLevel = "MODERATE",
                goalType = "MAINTENANCE",
                calorieAdjustment = 0,
                bmr = 0,
                tdee = 0,
                dailyCalorieTarget = 0
            )
            dao.insertOrUpdateConfig(initialConfig)
        }
    }

    suspend fun clearUserData() {
        dao.clearAllFoodLogs()
        dao.clearAllWeightLogs()
        dao.insertOrUpdateConfig(
            UserNutritionConfig(
                id = 1,
                sex = "MALE",
                weightKg = 0f,
                targetWeightKg = 0f,
                autoSyncWeight = true,
                heightCm = 0,
                age = 0,
                activityLevel = "MODERATE",
                goalType = "MAINTENANCE",
                calorieAdjustment = 0,
                bmr = 0,
                tdee = 0,
                dailyCalorieTarget = 0
            )
        )
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
