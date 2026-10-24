package com.poorrat.gotcha.data.repository

import com.poorrat.gotcha.data.local.dao.WorkoutDao
import com.poorrat.gotcha.data.local.entity.RoutineDayEntity
import com.poorrat.gotcha.data.local.entity.RoutineExerciseEntity
import com.poorrat.gotcha.data.local.entity.WorkoutSessionLogEntity
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val dao: WorkoutDao) {

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
                    routineName = "Upper de fuerza",
                    bodyPart = "Upper"
                ),
                RoutineDayEntity(
                    dayOfWeek = 2,
                    dayLabel = "Día 2 · Martes",
                    routineName = "Lower de fuerza",
                    bodyPart = "Lower"
                ),
                RoutineDayEntity(
                    dayOfWeek = 3,
                    dayLabel = "Día 3 · Miércoles",
                    routineName = "Recuperación activa",
                    bodyPart = "Descanso"
                ),
                RoutineDayEntity(
                    dayOfWeek = 4,
                    dayLabel = "Día 4 · Jueves",
                    routineName = "Empuje hipertrofia",
                    bodyPart = "Empuje"
                ),
                RoutineDayEntity(
                    dayOfWeek = 5,
                    dayLabel = "Día 5 · Viernes",
                    routineName = "Jalón & Brazos",
                    bodyPart = "Jalón"
                ),
                RoutineDayEntity(
                    dayOfWeek = 6,
                    dayLabel = "Día 6 · Sábado",
                    routineName = "Piernas & Core",
                    bodyPart = "Piernas"
                ),
                RoutineDayEntity(
                    dayOfWeek = 7,
                    dayLabel = "Día 7 · Domingo",
                    routineName = "Descanso total",
                    bodyPart = "Descanso"
                )
            )
            dao.insertDays(defaultDays)

            val defaultExercises = listOf(
                // Day 1: Upper (matching images 9, 10, 11)
                RoutineExerciseEntity(
                    dayOfWeek = 1,
                    exerciseOrder = 1,
                    name = "Press de Banca Plano con Barra",
                    targetSets = 4,
                    targetReps = "8-10",
                    targetWeightKg = 85.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 1,
                    exerciseOrder = 2,
                    name = "Remo con Barra Pendlay",
                    targetSets = 4,
                    targetReps = "8-10",
                    targetWeightKg = 75.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 1,
                    exerciseOrder = 3,
                    name = "Press Militar con Mancuernas",
                    targetSets = 3,
                    targetReps = "10-12",
                    targetWeightKg = 24.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 1,
                    exerciseOrder = 4,
                    name = "Fondos en Paralelas Lastrados",
                    targetSets = 3,
                    targetReps = "12",
                    targetWeightKg = 10.0f
                ),

                // Day 2: Lower
                RoutineExerciseEntity(
                    dayOfWeek = 2,
                    exerciseOrder = 1,
                    name = "Sentadilla Trasera con Barra",
                    targetSets = 4,
                    targetReps = "6-8",
                    targetWeightKg = 110.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 2,
                    exerciseOrder = 2,
                    name = "Peso Muerto Rumano",
                    targetSets = 3,
                    targetReps = "8-10",
                    targetWeightKg = 100.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 2,
                    exerciseOrder = 3,
                    name = "Prensa de Piernas",
                    targetSets = 3,
                    targetReps = "10-12",
                    targetWeightKg = 180.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 2,
                    exerciseOrder = 4,
                    name = "Elevación de Talones",
                    targetSets = 4,
                    targetReps = "15",
                    targetWeightKg = 60.0f
                ),

                // Day 4: Empuje
                RoutineExerciseEntity(
                    dayOfWeek = 4,
                    exerciseOrder = 1,
                    name = "Press Inclinado con Mancuernas",
                    targetSets = 4,
                    targetReps = "10-12",
                    targetWeightKg = 30.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 4,
                    exerciseOrder = 2,
                    name = "Cruces en Polea",
                    targetSets = 3,
                    targetReps = "15",
                    targetWeightKg = 15.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 4,
                    exerciseOrder = 3,
                    name = "Elevaciones Laterales",
                    targetSets = 4,
                    targetReps = "12-15",
                    targetWeightKg = 12.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 4,
                    exerciseOrder = 4,
                    name = "Extensión de Tríceps en Polea",
                    targetSets = 4,
                    targetReps = "12",
                    targetWeightKg = 25.0f
                ),

                // Day 5: Jalón
                RoutineExerciseEntity(
                    dayOfWeek = 5,
                    exerciseOrder = 1,
                    name = "Dominadas Lastradas",
                    targetSets = 4,
                    targetReps = "6-8",
                    targetWeightKg = 15.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 5,
                    exerciseOrder = 2,
                    name = "Remo en Polea Baja",
                    targetSets = 4,
                    targetReps = "10-12",
                    targetWeightKg = 60.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 5,
                    exerciseOrder = 3,
                    name = "Curl de Bíceps con Barra Z",
                    targetSets = 3,
                    targetReps = "10",
                    targetWeightKg = 30.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 5,
                    exerciseOrder = 4,
                    name = "Face Pulls",
                    targetSets = 4,
                    targetReps = "15",
                    targetWeightKg = 20.0f
                ),

                // Day 6: Piernas
                RoutineExerciseEntity(
                    dayOfWeek = 6,
                    exerciseOrder = 1,
                    name = "Hip Thrust",
                    targetSets = 4,
                    targetReps = "8-10",
                    targetWeightKg = 120.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 6,
                    exerciseOrder = 2,
                    name = "Zancadas con Mancuernas",
                    targetSets = 3,
                    targetReps = "12",
                    targetWeightKg = 20.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 6,
                    exerciseOrder = 3,
                    name = "Curl Femoral Tumbado",
                    targetSets = 3,
                    targetReps = "12",
                    targetWeightKg = 45.0f
                ),
                RoutineExerciseEntity(
                    dayOfWeek = 6,
                    exerciseOrder = 4,
                    name = "Plancha Abdominal",
                    targetSets = 3,
                    targetReps = "60s",
                    targetWeightKg = 0.0f
                )
            )
            dao.insertExercises(defaultExercises)
        }
    }
}
