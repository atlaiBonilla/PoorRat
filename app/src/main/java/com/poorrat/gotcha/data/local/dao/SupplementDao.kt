package com.poorrat.gotcha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.poorrat.gotcha.data.local.entity.SupplementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementDao {

    @Query("SELECT * FROM supplements ORDER BY id ASC")
    fun getAllSupplements(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements WHERE id = :id LIMIT 1")
    fun getSupplementById(id: Long): Flow<SupplementEntity?>

    @Query("SELECT * FROM supplements WHERE id = :id LIMIT 1")
    suspend fun getSupplementByIdSync(id: Long): SupplementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplement(supplement: SupplementEntity): Long

    @Update
    suspend fun updateSupplement(supplement: SupplementEntity)

    @Query("DELETE FROM supplements WHERE id = :id")
    suspend fun deleteSupplementById(id: Long)

    @Query("UPDATE supplements SET lastTakenDate = :dateString WHERE id = :id")
    suspend fun updateTakenState(id: Long, dateString: String?)

    @Query("UPDATE supplements SET alarmEnabled = :enabled, alarmHour = :hour, alarmMinute = :minute WHERE id = :id")
    suspend fun updateAlarmConfig(id: Long, enabled: Boolean, hour: Int, minute: Int)

    @Query("SELECT COUNT(*) FROM supplements")
    suspend fun getSupplementCount(): Int
}
