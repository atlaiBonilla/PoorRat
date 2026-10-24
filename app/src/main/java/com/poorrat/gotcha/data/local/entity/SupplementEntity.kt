package com.poorrat.gotcha.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val intakeMoment: String,
    val alarmEnabled: Boolean = false,
    val alarmHour: Int = 8,
    val alarmMinute: Int = 0,
    val lastTakenDate: String? = null
)
