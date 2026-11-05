package com.poorrat.gotcha.data.repository

import com.poorrat.gotcha.data.firebase.FirebaseSyncRepository
import com.poorrat.gotcha.data.local.dao.WorkoutDao
import com.poorrat.gotcha.data.local.entity.RoutineDayEntity
import com.poorrat.gotcha.data.local.entity.RoutineExerciseEntity
import com.poorrat.gotcha.data.local.entity.WorkoutSessionLogEntity
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val dao: WorkoutDao,
    private val firebaseSyncRepository: FirebaseSyncRepository? = null
) {

    val allRoutineDays: Flow<List<RoutineDayEntity>> = dao.getAllRoutineDays()
    val allExercises: Flow<List<RoutineExerciseEntity>> = dao.getAllExercises()

    fun getRoutineDay(dayOfWeek: Int): Flow<RoutineDayEntity?> = dao.getRoutineDay(dayOfWeek)

    fun getExercisesForDay(dayOfWeek: Int): Flow<List<RoutineExerciseEntity>> =
        dao.getExercisesForDay(dayOfWeek)

    suspend fun getExercisesForDaySync(dayOfWeek: Int): List<RoutineExerciseEntity> =
        dao.getExercisesForDaySync(dayOfWeek)

    suspend fun saveDaySchedule(
        day: RoutineDayEntity,
        exercises: List<RoutineExerciseEntity>
    ) {
        dao.saveDaySchedule(day, exercises)
        try {
            firebaseSyncRepository?.saveRoutineDay(day, exercises)
        } catch (_: Exception) {
            // Keep local changes if offline or unauthenticated
        }
    }

    suspend fun syncWithCloud() {
        val fbSync = firebaseSyncRepository ?: return
        try {
            val cloudDays = fbSync.fetchRoutineDays()
            if (cloudDays.isNotEmpty()) {
                for (cloudDay in cloudDays) {
                    val dayEntity = RoutineDayEntity(
                        dayOfWeek = cloudDay.dayOfWeek,
                        dayLabel = cloudDay.dayLabel.ifBlank { getDayLabel(cloudDay.dayOfWeek) },
                        routineName = cloudDay.routineName,
                        bodyPart = cloudDay.bodyPart
                    )
                    val exercises = cloudDay.exercises.map { fbEx ->
                        RoutineExerciseEntity(
                            id = 0,
                            dayOfWeek = fbEx.dayOfWeek,
                            exerciseOrder = fbEx.exerciseOrder,
                            name = fbEx.name,
                            targetSets = fbEx.targetSets,
                            targetReps = fbEx.targetReps,
                            targetWeightKg = fbEx.targetWeightKg.toFloat()
                        )
                    }
                    dao.saveDaySchedule(dayEntity, exercises)
                }
            } else {
                // If cloud has no routines yet, upload any customized local routines
                val localDays = dao.getAllRoutineDaysSync()
                for (day in localDays) {
                    if (day.routineName != "Sin rutina" || day.bodyPart != "Descanso") {
                        val exercises = dao.getExercisesForDaySync(day.dayOfWeek)
                        fbSync.saveRoutineDay(day, exercises)
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback to local cache
        }
    }

    private fun getDayLabel(day: Int): String {
        return when (day) {
            1 -> "Día 1 · Lunes"
            2 -> "Día 2 · Martes"
            3 -> "Día 3 · Miércoles"
            4 -> "Día 4 · Jueves"
            5 -> "Día 5 · Viernes"
            6 -> "Día 6 · Sábado"
            7 -> "Día 7 · Domingo"
            else -> "Día $day"
        }
    }

    suspend fun deleteExercise(id: Long) {
        dao.deleteExerciseById(id)
    }

    suspend fun finishWorkoutSession(
        session: WorkoutSessionLogEntity,
        completedUpdates: List<Triple<Long, Float, String>>
    ) {
        dao.finishWorkout(session, completedUpdates)
    }

    suspend fun prepopulateIfEmpty() {
        if (dao.getRoutineDayCount() == 0) {
            val defaultDays = listOf(
                RoutineDayEntity(
                    dayOfWeek = 1,
                    dayLabel = "Día 1 · Lunes",
                    routineName = "Sin rutina",
                    bodyPart = "Descanso"
                ),
                RoutineDayEntity(
                    dayOfWeek = 2,
                    dayLabel = "Día 2 · Martes",
                    routineName = "Sin rutina",
                    bodyPart = "Descanso"
                ),
                RoutineDayEntity(
                    dayOfWeek = 3,
                    dayLabel = "Día 3 · Miércoles",
                    routineName = "Sin rutina",
                    bodyPart = "Descanso"
                ),
                RoutineDayEntity(
                    dayOfWeek = 4,
                    dayLabel = "Día 4 · Jueves",
                    routineName = "Sin rutina",
                    bodyPart = "Descanso"
                ),
                RoutineDayEntity(
                    dayOfWeek = 5,
                    dayLabel = "Día 5 · Viernes",
                    routineName = "Sin rutina",
                    bodyPart = "Descanso"
                ),
                RoutineDayEntity(
                    dayOfWeek = 6,
                    dayLabel = "Día 6 · Sábado",
                    routineName = "Sin rutina",
                    bodyPart = "Descanso"
                ),
                RoutineDayEntity(
                    dayOfWeek = 7,
                    dayLabel = "Día 7 · Domingo",
                    routineName = "Sin rutina",
                    bodyPart = "Descanso"
                )
            )
            dao.insertDays(defaultDays)
        }
    }

    suspend fun clearUserData() {
        dao.clearAllExercises()
        dao.clearAllSessions()
    }
}
