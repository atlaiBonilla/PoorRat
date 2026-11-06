package com.poorrat.gotchadev.data.firebase.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FirebaseUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val unitSystemWeight: String = "kg",
    val unitSystemHeight: String = "cm",
    val weightKg: Double = 0.0,
    val targetWeightKg: Double = 0.0,
    val autoSyncWeight: Boolean = true,
    val heightCm: Int = 0,
    val age: Int = 0,
    val sex: String = "MALE",
    val activityLevel: String = "MODERATE",
    val goalType: String = "MAINTENANCE",
    val calorieAdjustment: Int = 0,
    val bmr: Int = 0,
    val tdee: Int = 0,
    val dailyCalorieTarget: Int = 0,
    val updatedAt: Long = 0L
)

@IgnoreExtraProperties
data class FirebaseWeightLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val weightKg: Double = 0.0,
    val note: String = "",
    val dateString: String = "",
    val timestampMillis: Long = 0L
)

@IgnoreExtraProperties
data class FirebaseFoodLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val foodName: String = "",
    val gramsConsumed: Double = 0.0,
    val calories: Int = 0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val dateString: String = "",
    val timestampMillis: Long = 0L,
    val supplementId: Long? = null
)

@IgnoreExtraProperties
data class FirebaseSupplement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val dosage: String = "",
    val intakeMoment: String = "",
    val alarmEnabled: Boolean = false,
    val alarmHour: Int = 8,
    val alarmMinute: Int = 0,
    val lastTakenDate: String? = null,
    val hasCalories: Boolean = false,
    val isDailyConsumption: Boolean = false,
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val updatedAt: Long = 0L
)

@IgnoreExtraProperties
data class FirebaseWorkoutPr(
    @DocumentId val id: String = "",
    val userId: String = "",
    val exerciseName: String = "",
    val weightKg: Double = 0.0,
    val reps: Long = 0L,
    val dateString: String = "",
    val timestampMillis: Long = 0L
)

@IgnoreExtraProperties
data class FirebaseWeeklyNutrition(
    @DocumentId val id: String = "",
    val weekKey: String = "",
    val userId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val totalCalories: Double = 0.0,
    val avgCaloriesPerDay: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val daysLogged: Long = 0L,
    val updatedAt: Long = 0L
)

@IgnoreExtraProperties
data class FirebaseRoutineExercise(
    val id: Long = 0L,
    val dayOfWeek: Int = 1,
    val exerciseOrder: Int = 1,
    val name: String = "",
    val targetSets: Int = 3,
    val targetReps: String = "10",
    val targetWeightKg: Double = 0.0
)

@IgnoreExtraProperties
data class FirebaseRoutineDay(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dayOfWeek: Int = 1,
    val dayLabel: String = "",
    val routineName: String = "",
    val bodyPart: String = "",
    val exercises: List<FirebaseRoutineExercise> = emptyList(),
    val updatedAt: Long = 0L
)
