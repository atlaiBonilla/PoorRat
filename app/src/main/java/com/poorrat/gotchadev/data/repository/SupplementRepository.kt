package com.poorrat.gotchadev.data.repository

import android.util.Log
import com.poorrat.gotchadev.data.firebase.FirebaseSyncRepository
import com.poorrat.gotchadev.data.local.dao.SupplementDao
import com.poorrat.gotchadev.data.local.entity.SupplementEntity
import kotlinx.coroutines.flow.Flow

private const val TAG = "SupplementRepo"

class SupplementRepository(
    private val dao: SupplementDao,
    private val firebaseSyncRepository: FirebaseSyncRepository? = null
) {

    val allSupplements: Flow<List<SupplementEntity>> = dao.getAllSupplements()

    fun getSupplement(id: Long): Flow<SupplementEntity?> = dao.getSupplementById(id)

    suspend fun getSupplementByIdSync(id: Long): SupplementEntity? = dao.getSupplementByIdSync(id)

    suspend fun insertSupplement(supplement: SupplementEntity): Long {
        val id = dao.insertSupplement(supplement)
        try {
            firebaseSyncRepository?.saveSupplement(supplement.copy(id = id))
        } catch (_: Exception) {
        }
        return id
    }

    suspend fun updateSupplement(supplement: SupplementEntity) {
        dao.updateSupplement(supplement)
        try {
            firebaseSyncRepository?.saveSupplement(supplement)
        } catch (_: Exception) {
        }
    }

    suspend fun deleteSupplement(id: Long) {
        dao.deleteSupplementById(id)
        try {
            firebaseSyncRepository?.deleteSupplement(id)
        } catch (_: Exception) {
        }
    }

    suspend fun updateTakenState(id: Long, isTaken: Boolean, todayDate: String) {
        val dateString = if (isTaken) todayDate else null
        dao.updateTakenState(id, dateString)
        try {
            val sup = dao.getSupplementByIdSync(id)
            if (sup != null) {
                firebaseSyncRepository?.saveSupplement(sup)
            }
        } catch (_: Exception) {
        }
    }

    suspend fun updateAlarmConfig(id: Long, enabled: Boolean, hour: Int, minute: Int) {
        dao.updateAlarmConfig(id, enabled, hour, minute)
        try {
            val sup = dao.getSupplementByIdSync(id)
            if (sup != null) {
                firebaseSyncRepository?.saveSupplement(sup)
            }
        } catch (_: Exception) {
        }
    }

    suspend fun syncWithCloud() {
        val fbSync = firebaseSyncRepository ?: return
        try {
            val cloudSups = fbSync.fetchSupplements()
            if (cloudSups.isNotEmpty()) {
                val localSups = dao.getAllSupplementsSync()
                val localNames = localSups.map { it.name.lowercase().trim() }.toSet()
                val toInsert = cloudSups.map { cs ->
                    SupplementEntity(
                        id = 0L,
                        name = cs.name,
                        dosage = cs.dosage,
                        intakeMoment = cs.intakeMoment,
                        alarmEnabled = cs.alarmEnabled,
                        alarmHour = cs.alarmHour,
                        alarmMinute = cs.alarmMinute,
                        lastTakenDate = cs.lastTakenDate,
                        hasCalories = cs.hasCalories,
                        isDailyConsumption = cs.isDailyConsumption,
                        calories = cs.calories,
                        protein = cs.protein.toFloat(),
                        carbs = cs.carbs.toFloat(),
                        fat = cs.fat.toFloat()
                    )
                }.filter { !localNames.contains(it.name.lowercase().trim()) }

                if (toInsert.isNotEmpty()) {
                    dao.insertSupplements(toInsert)
                    Log.d(TAG, "Restored ${toInsert.size} supplements from Firestore")
                }
            } else {
                val localSups = dao.getAllSupplementsSync()
                if (localSups.isNotEmpty()) {
                    fbSync.syncAllSupplements(localSups)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supplement syncWithCloud encountered non-fatal error: ${e.message}")
        }
    }

    suspend fun prepopulateIfEmpty(todayDate: String) {
        // Starts clean without dummy data per user request
    }

    suspend fun clearUserData() {
        dao.clearAllSupplements()
    }
}
