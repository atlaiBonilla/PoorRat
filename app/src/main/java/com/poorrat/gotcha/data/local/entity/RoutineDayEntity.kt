package com.poorrat.gotcha.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_days")
data class RoutineDayEntity(
    @PrimaryKey
    val dayOfWeek: Int, // 1 = Lunes .. 7 = Domingo
    val dayLabel: String, // ej. "Día 1 · Lunes"
    val routineName: String, // ej. "Upper de fuerza"
    val bodyPart: String // "Piernas", "Empuje", "Jalón", "Upper", "Lower", "Brazos", "Descanso"
)
