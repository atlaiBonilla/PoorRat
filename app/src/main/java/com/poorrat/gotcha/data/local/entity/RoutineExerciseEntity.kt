package com.poorrat.gotcha.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineDayEntity::class,
            parentColumns = ["dayOfWeek"],
            childColumns = ["dayOfWeek"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dayOfWeek"])]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: Int,
    val exerciseOrder: Int,
    val name: String,
    val targetSets: Int,
    val targetReps: String,
    val targetWeightKg: Float
)
