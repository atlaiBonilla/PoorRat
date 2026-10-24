package com.poorrat.gotcha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.poorrat.gotcha.data.local.entity.RoutineDayEntity
import com.poorrat.gotcha.data.local.entity.RoutineExerciseEntity
import com.poorrat.gotcha.data.local.entity.WorkoutSessionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // --- Routine Days ---
    @Query("SELECT * FROM routine_days ORDER BY dayOfWeek ASC")
    fun getAllRoutineDays(): Flow<List<RoutineDayEntity>>

    @Query("SELECT * FROM routine_days WHERE dayOfWeek = :dayOfWeek LIMIT 1")
    fun getRoutineDay(dayOfWeek: Int): Flow<RoutineDayEntity?>

    @Query("SELECT * FROM routine_days WHERE dayOfWeek = :dayOfWeek LIMIT 1")
    suspend fun getRoutineDaySync(dayOfWeek: Int): RoutineDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDay(day: RoutineDayEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDays(days: List<RoutineDayEntity>)

    @Query("SELECT COUNT(*) FROM routine_days")
    suspend fun getRoutineDayCount(): Int

    // --- Routine Exercises ---
    @Query("SELECT * FROM routine_exercises WHERE dayOfWeek = :dayOfWeek ORDER BY exerciseOrder ASC, id ASC")
    fun getExercisesForDay(dayOfWeek: Int): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercises WHERE dayOfWeek = :dayOfWeek ORDER BY exerciseOrder ASC, id ASC")
    suspend fun getExercisesForDaySync(dayOfWeek: Int): List<RoutineExerciseEntity>

    @Query("SELECT * FROM routine_exercises ORDER BY dayOfWeek ASC, exerciseOrder ASC")
    fun getAllExercises(): Flow<List<RoutineExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: RoutineExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(exercises: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Long)

    @Query("DELETE FROM routine_exercises WHERE dayOfWeek = :dayOfWeek")
    suspend fun deleteExercisesForDay(dayOfWeek: Int)

    @Query("UPDATE routine_exercises SET targetWeightKg = :achievedWeightKg, targetReps = :achievedReps WHERE id = :id")
    suspend fun updateExerciseProgress(id: Long, achievedWeightKg: Float, achievedReps: String)

    // --- Workout Session Logs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionLog(session: WorkoutSessionLogEntity): Long

    @Query("SELECT * FROM workout_session_logs ORDER BY timestampMillis DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionLogEntity>>

    // --- Transactions ---
    @Transaction
    suspend fun saveDaySchedule(
        day: RoutineDayEntity,
        exercises: List<RoutineExerciseEntity>
    ) {
        insertOrUpdateDay(day)
        deleteExercisesForDay(day.dayOfWeek)
        if (day.bodyPart != "Descanso") {
            insertExercises(exercises)
        }
    }

    @Transaction
    suspend fun finishWorkout(
        session: WorkoutSessionLogEntity,
        completedUpdates: List<Triple<Long, Float, String>> // id, achievedWeight, achievedReps
    ) {
        insertSessionLog(session)
        for ((exerciseId, weight, reps) in completedUpdates) {
            updateExerciseProgress(exerciseId, weight, reps)
        }
    }
}
