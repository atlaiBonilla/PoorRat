package com.poorrat.gotcha.data.repository

import com.poorrat.gotcha.data.local.dao.SupplementDao
import com.poorrat.gotcha.data.local.entity.SupplementEntity
import kotlinx.coroutines.flow.Flow

class SupplementRepository(private val dao: SupplementDao) {

    val allSupplements: Flow<List<SupplementEntity>> = dao.getAllSupplements()

    fun getSupplement(id: Long): Flow<SupplementEntity?> = dao.getSupplementById(id)

    suspend fun insertSupplement(supplement: SupplementEntity): Long {
        return dao.insertSupplement(supplement)
    }

    suspend fun updateSupplement(supplement: SupplementEntity) {
        dao.updateSupplement(supplement)
    }

    suspend fun deleteSupplement(id: Long) {
        dao.deleteSupplementById(id)
    }

    suspend fun updateTakenState(id: Long, isTaken: Boolean, todayDate: String) {
        val dateString = if (isTaken) todayDate else null
        dao.updateTakenState(id, dateString)
    }

    suspend fun updateAlarmConfig(id: Long, enabled: Boolean, hour: Int, minute: Int) {
        dao.updateAlarmConfig(id, enabled, hour, minute)
    }

    suspend fun prepopulateIfEmpty(todayDate: String) {
        if (dao.getSupplementCount() == 0) {
            val defaults = listOf(
                SupplementEntity(
                    name = "Creatina Monohidrato Creapure",
                    dosage = "5g",
                    intakeMoment = "Con el desayuno",
                    alarmEnabled = true,
                    alarmHour = 8,
                    alarmMinute = 0,
                    lastTakenDate = todayDate
                ),
                SupplementEntity(
                    name = "Whey Protein Isolate",
                    dosage = "30g",
                    intakeMoment = "Post-entrenamiento",
                    alarmEnabled = true,
                    alarmHour = 19,
                    alarmMinute = 0,
                    lastTakenDate = null
                ),
                SupplementEntity(
                    name = "Multivitamínico Mineral",
                    dosage = "1 comprimido",
                    intakeMoment = "Con comida 1",
                    alarmEnabled = true,
                    alarmHour = 13,
                    alarmMinute = 30,
                    lastTakenDate = todayDate
                ),
                SupplementEntity(
                    name = "Omega 3 Ultra EPA/DHA",
                    dosage = "2 perlas",
                    intakeMoment = "Con la cena",
                    alarmEnabled = false,
                    alarmHour = 22,
                    alarmMinute = 30,
                    lastTakenDate = null
                ),
                SupplementEntity(
                    name = "Pre-Workout PoorRat Focus",
                    dosage = "1 scoop (10g)",
                    intakeMoment = "30m antes de entrenar",
                    alarmEnabled = false,
                    alarmHour = 17,
                    alarmMinute = 0,
                    lastTakenDate = null
                )
            )
            defaults.forEach { dao.insertSupplement(it) }
        }
    }
}
