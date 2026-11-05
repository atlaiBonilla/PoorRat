package com.poorrat.gotcha.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.poorrat.gotcha.R
import com.poorrat.gotcha.data.firebase.models.FirebaseFoodLog
import com.poorrat.gotcha.data.firebase.models.FirebaseRoutineDay
import com.poorrat.gotcha.data.firebase.models.FirebaseRoutineExercise
import com.poorrat.gotcha.data.firebase.models.FirebaseSupplement
import com.poorrat.gotcha.data.firebase.models.FirebaseUserProfile
import com.poorrat.gotcha.data.firebase.models.FirebaseWeeklyNutrition
import com.poorrat.gotcha.data.firebase.models.FirebaseWeightLog
import com.poorrat.gotcha.data.firebase.models.FirebaseWorkoutPr
import com.poorrat.gotcha.data.local.entity.DailyFoodLog
import com.poorrat.gotcha.data.local.entity.RoutineDayEntity
import com.poorrat.gotcha.data.local.entity.RoutineExerciseEntity
import com.poorrat.gotcha.data.local.entity.SupplementEntity
import com.poorrat.gotcha.data.local.entity.UserNutritionConfig
import com.poorrat.gotcha.data.local.entity.WeightLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "FirebaseSyncRepo"

class FirebaseSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: error("User must be authenticated for Firestore operations")
    }

    // ==========================================
    // USER PROFILE & NUTRITION GOAL CONFIGURATION
    // ==========================================

    suspend fun saveUserProfile(profile: FirebaseUserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val payload = profile.copy(userId = uid, updatedAt = System.currentTimeMillis())
            docRef.set(payload).await()
            Log.d(TAG, "Saved user profile & nutrition goals for uid $uid")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun getUserProfile(userId: String = requireUserId()): FirebaseUserProfile? {
        val docRef = db.collection("users").document(userId)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.toObject(FirebaseUserProfile::class.java)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            null
        }
    }

    suspend fun saveNutritionConfig(config: UserNutritionConfig) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val currentProfile = getUserProfile(uid) ?: FirebaseUserProfile(id = uid, userId = uid)
            val updatedProfile = currentProfile.copy(
                weightKg = config.weightKg.toDouble(),
                targetWeightKg = config.targetWeightKg.toDouble(),
                autoSyncWeight = config.autoSyncWeight,
                heightCm = config.heightCm,
                age = config.age,
                sex = config.sex,
                activityLevel = config.activityLevel,
                goalType = config.goalType,
                calorieAdjustment = config.calorieAdjustment,
                bmr = config.bmr,
                tdee = config.tdee,
                dailyCalorieTarget = config.dailyCalorieTarget,
                updatedAt = System.currentTimeMillis()
            )
            docRef.set(updatedProfile).await()
            Log.d(TAG, "Saved nutrition config to user profile: dailyTarget=${config.dailyCalorieTarget}, targetWeight=${config.targetWeightKg}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    // ==========================================
    // DAILY FOOD LOGS (WHAT I ATE TODAY & WEEKLY)
    // ==========================================

    suspend fun saveFoodLog(log: DailyFoodLog) {
        val uid = requireUserId()
        val docId = if (log.id > 0) log.id.toString() else "${log.dateString}_${log.timestampMillis}"
        val docRef = db.collection("users").document(uid).collection("food_logs").document(docId)
        val payload = FirebaseFoodLog(
            id = docId,
            userId = uid,
            foodName = log.foodName,
            gramsConsumed = log.gramsConsumed.toDouble(),
            calories = log.calories,
            proteinGrams = log.proteinGrams.toDouble(),
            carbsGrams = log.carbsGrams.toDouble(),
            fatGrams = log.fatGrams.toDouble(),
            dateString = log.dateString,
            timestampMillis = log.timestampMillis,
            supplementId = log.supplementId
        )
        try {
            docRef.set(payload).await()
            Log.d(TAG, "Uploaded food log to Firestore: ${log.foodName}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun deleteFoodLog(logId: Long) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("food_logs").document(logId.toString())
        try {
            docRef.delete().await()
            Log.d(TAG, "Deleted food log from Firestore: $logId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    suspend fun fetchFoodLogs(): List<FirebaseFoodLog> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("food_logs")
        return try {
            val snapshot = colRef.get().await()
            snapshot.toObjects(FirebaseFoodLog::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, colRef.path)
            emptyList()
        }
    }

    suspend fun syncAllFoodLogs(logs: List<DailyFoodLog>) {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("food_logs")
        for (log in logs) {
            val docId = if (log.id > 0) log.id.toString() else "${log.dateString}_${log.timestampMillis}"
            val payload = FirebaseFoodLog(
                id = docId,
                userId = uid,
                foodName = log.foodName,
                gramsConsumed = log.gramsConsumed.toDouble(),
                calories = log.calories,
                proteinGrams = log.proteinGrams.toDouble(),
                carbsGrams = log.carbsGrams.toDouble(),
                fatGrams = log.fatGrams.toDouble(),
                dateString = log.dateString,
                timestampMillis = log.timestampMillis,
                supplementId = log.supplementId
            )
            try {
                colRef.document(docId).set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "${colRef.path}/$docId")
            }
        }
    }

    // ==========================================
    // SUPPLEMENTS (MY SUPPLEMENTS & MACROS)
    // ==========================================

    suspend fun saveSupplement(supplement: SupplementEntity) {
        val uid = requireUserId()
        val docId = if (supplement.id > 0) supplement.id.toString() else supplement.name.replace(" ", "_").lowercase()
        val docRef = db.collection("users").document(uid).collection("supplements").document(docId)
        val payload = FirebaseSupplement(
            id = docId,
            userId = uid,
            name = supplement.name,
            dosage = supplement.dosage,
            intakeMoment = supplement.intakeMoment,
            alarmEnabled = supplement.alarmEnabled,
            alarmHour = supplement.alarmHour,
            alarmMinute = supplement.alarmMinute,
            lastTakenDate = supplement.lastTakenDate,
            hasCalories = supplement.hasCalories,
            isDailyConsumption = supplement.isDailyConsumption,
            calories = supplement.calories,
            protein = supplement.protein.toDouble(),
            carbs = supplement.carbs.toDouble(),
            fat = supplement.fat.toDouble(),
            updatedAt = System.currentTimeMillis()
        )
        try {
            docRef.set(payload).await()
            Log.d(TAG, "Uploaded supplement to Firestore: ${supplement.name}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun deleteSupplement(supplementId: Long) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("supplements").document(supplementId.toString())
        try {
            docRef.delete().await()
            Log.d(TAG, "Deleted supplement from Firestore: $supplementId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    suspend fun fetchSupplements(): List<FirebaseSupplement> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("supplements")
        return try {
            val snapshot = colRef.get().await()
            snapshot.toObjects(FirebaseSupplement::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, colRef.path)
            emptyList()
        }
    }

    suspend fun syncAllSupplements(supplements: List<SupplementEntity>) {
        for (sup in supplements) {
            saveSupplement(sup)
        }
    }

    // ==========================================
    // BODY WEIGHT LOGS (PRESERVES DATES & NOTES)
    // ==========================================

    suspend fun saveWeightLog(log: WeightLog) {
        val uid = requireUserId()
        val docId = if (log.id > 0) log.id.toString() else "${log.dateString}_${log.timestampMillis}"
        val docRef = db.collection("users").document(uid).collection("weight_logs").document(docId)
        val firebaseLog = FirebaseWeightLog(
            id = docId,
            userId = uid,
            weightKg = log.weightKg.toDouble(),
            note = log.note,
            dateString = log.dateString,
            timestampMillis = log.timestampMillis
        )
        try {
            docRef.set(firebaseLog).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun deleteWeightLog(logId: Long) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("weight_logs").document(logId.toString())
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    suspend fun syncWeightLogs(logs: List<WeightLog>) {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("weight_logs")
        for (log in logs) {
            val docId = if (log.id > 0) log.id.toString() else "${log.dateString}_${log.timestampMillis}"
            val docRef = collectionRef.document(docId)
            val firebaseLog = FirebaseWeightLog(
                id = docId,
                userId = uid,
                weightKg = log.weightKg.toDouble(),
                note = log.note,
                dateString = log.dateString,
                timestampMillis = log.timestampMillis
            )
            try {
                docRef.set(firebaseLog).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, docRef.path)
            }
        }
    }

    suspend fun fetchWeightLogs(): List<FirebaseWeightLog> {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("weight_logs")
        return try {
            val snapshot = collectionRef.get().await()
            snapshot.toObjects(FirebaseWeightLog::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, collectionRef.path)
            emptyList()
        }
    }

    // ==========================================
    // WORKOUT ROUTINES & EXERCISES BY DAY
    // ==========================================

    suspend fun saveRoutineDay(day: RoutineDayEntity, exercises: List<RoutineExerciseEntity>) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("routine_days").document(day.dayOfWeek.toString())
        try {
            val fbExercises = exercises.map { ex ->
                FirebaseRoutineExercise(
                    id = ex.id,
                    dayOfWeek = ex.dayOfWeek,
                    exerciseOrder = ex.exerciseOrder,
                    name = ex.name,
                    targetSets = ex.targetSets,
                    targetReps = ex.targetReps,
                    targetWeightKg = ex.targetWeightKg.toDouble()
                )
            }
            val payload = FirebaseRoutineDay(
                id = day.dayOfWeek.toString(),
                userId = uid,
                dayOfWeek = day.dayOfWeek,
                dayLabel = day.dayLabel,
                routineName = day.routineName,
                bodyPart = day.bodyPart,
                exercises = fbExercises,
                updatedAt = System.currentTimeMillis()
            )
            docRef.set(payload).await()
            Log.d(TAG, "Saved routine day ${day.dayOfWeek} (${day.routineName}) with ${exercises.size} exercises to Firestore")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun fetchRoutineDays(): List<FirebaseRoutineDay> {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("routine_days")
        return try {
            val snapshot = collectionRef.get().await()
            snapshot.toObjects(FirebaseRoutineDay::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, collectionRef.path)
            emptyList()
        }
    }

    suspend fun syncWorkoutPrs(prs: List<FirebaseWorkoutPr>) {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("workout_prs")
        for (pr in prs) {
            val docId = if (pr.id.isNotBlank()) pr.id else pr.exerciseName.replace(" ", "_").lowercase()
            val docRef = collectionRef.document(docId)
            val payload = pr.copy(id = docId, userId = uid, timestampMillis = System.currentTimeMillis())
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, docRef.path)
            }
        }
    }

    suspend fun syncWeeklyNutrition(weekData: FirebaseWeeklyNutrition) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("weekly_nutrition").document(weekData.weekKey)
        try {
            val payload = weekData.copy(userId = uid, updatedAt = System.currentTimeMillis())
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    fun observeWeightLogs(userId: String = requireUserId()): Flow<List<FirebaseWeightLog>> {
        return db.collection("users").document(userId).collection("weight_logs")
            .snapshots()
            .map { it.toObjects(FirebaseWeightLog::class.java) }
    }

    fun observeWorkoutPrs(userId: String = requireUserId()): Flow<List<FirebaseWorkoutPr>> {
        return db.collection("users").document(userId).collection("workout_prs")
            .snapshots()
            .map { it.toObjects(FirebaseWorkoutPr::class.java) }
    }

    fun observeWeeklyNutrition(userId: String = requireUserId()): Flow<List<FirebaseWeeklyNutrition>> {
        return db.collection("users").document(userId).collection("weekly_nutrition")
            .snapshots()
            .map { it.toObjects(FirebaseWeeklyNutrition::class.java) }
    }

    fun observeRoutineDays(userId: String = requireUserId()): Flow<List<FirebaseRoutineDay>> {
        return db.collection("users").document(userId).collection("routine_days")
            .snapshots()
            .map { it.toObjects(FirebaseRoutineDay::class.java) }
    }

    /**
     * Fetches food catalog items added to Firestore /food_catalog collection.
     */
    suspend fun fetchCloudFoodCatalog(): List<com.poorrat.gotcha.data.local.entity.FoodItem> {
        return try {
            val snapshot = db.collection("food_catalog").get().await()
            snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: doc.getString("foodName") ?: doc.id
                val cal = doc.getDouble("caloriesPer100g")?.toFloat()
                    ?: doc.getLong("caloriesPer100g")?.toFloat()
                    ?: doc.getDouble("calories")?.toFloat()
                    ?: doc.getLong("calories")?.toFloat()
                    ?: 0f
                val prot = doc.getDouble("proteinPer100g")?.toFloat()
                    ?: doc.getLong("proteinPer100g")?.toFloat()
                    ?: doc.getDouble("protein")?.toFloat()
                    ?: doc.getLong("protein")?.toFloat()
                    ?: 0f
                val carbs = doc.getDouble("carbsPer100g")?.toFloat()
                    ?: doc.getLong("carbsPer100g")?.toFloat()
                    ?: doc.getDouble("carbs")?.toFloat()
                    ?: doc.getLong("carbs")?.toFloat()
                    ?: 0f
                val fat = doc.getDouble("fatPer100g")?.toFloat()
                    ?: doc.getLong("fatPer100g")?.toFloat()
                    ?: doc.getDouble("fat")?.toFloat()
                    ?: doc.getLong("fat")?.toFloat()
                    ?: 0f
                if (name.isNotBlank()) {
                    com.poorrat.gotcha.data.local.entity.FoodItem(
                        name = name,
                        caloriesPer100g = cal,
                        proteinPer100g = prot,
                        carbsPer100g = carbs,
                        fatPer100g = fat
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal: failed to fetch cloud food catalog: ${e.message}")
            emptyList()
        }
    }

    /**
     * Seeds initial food catalog items to Firestore /food_catalog if the collection is empty,
     * so the collection and structure are immediately visible in Firebase Console.
     */
    suspend fun seedCloudFoodCatalog(foods: List<com.poorrat.gotcha.data.local.entity.FoodItem>) {
        if (foods.isEmpty()) return
        try {
            val col = db.collection("food_catalog")
            for (chunk in foods.chunked(400)) {
                val batch = db.batch()
                for (food in chunk) {
                    val docId = food.name.lowercase()
                        .replace("á", "a")
                        .replace("é", "e")
                        .replace("í", "i")
                        .replace("ó", "o")
                        .replace("ú", "u")
                        .replace("ü", "u")
                        .replace("ñ", "n")
                        .replace(Regex("[^a-z0-9]+"), "_")
                        .trim('_')
                        .ifBlank { "food_${food.hashCode()}" }
                    val data = mapOf(
                        "name" to food.name,
                        "caloriesPer100g" to food.caloriesPer100g.toDouble(),
                        "proteinPer100g" to food.proteinPer100g.toDouble(),
                        "carbsPer100g" to food.carbsPer100g.toDouble(),
                        "fatPer100g" to food.fatPer100g.toDouble()
                    )
                    batch.set(col.document(docId), data)
                }
                batch.commit().await()
            }
            Log.d(TAG, "Seeded ${foods.size} items to Firestore /food_catalog")
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal: failed to seed cloud food catalog: ${e.message}")
        }
    }
}

