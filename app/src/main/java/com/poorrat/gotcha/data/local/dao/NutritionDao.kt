package com.poorrat.gotcha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.poorrat.gotcha.data.local.entity.DailyFoodLog
import com.poorrat.gotcha.data.local.entity.FoodItem
import com.poorrat.gotcha.data.local.entity.UserNutritionConfig
import com.poorrat.gotcha.data.local.entity.WeightLog
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {

    // --- User Nutrition Config ---
    @Query("SELECT * FROM user_nutrition_config WHERE id = 1 LIMIT 1")
    fun getUserConfig(): Flow<UserNutritionConfig?>

    @Query("SELECT * FROM user_nutrition_config WHERE id = 1 LIMIT 1")
    suspend fun getUserConfigSync(): UserNutritionConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: UserNutritionConfig)

    // --- Food Catalog ---
    @Query("SELECT * FROM food_catalog WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchFood(query: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_catalog ORDER BY name ASC")
    fun getAllFoods(): Flow<List<FoodItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoods(foods: List<FoodItem>)

    @Query("SELECT COUNT(*) FROM food_catalog")
    suspend fun getFoodCount(): Int

    // --- Daily Food Logs ---
    @Query("SELECT * FROM daily_food_logs WHERE dateString = :dateString ORDER BY timestampMillis DESC")
    fun getLogsByDate(dateString: String): Flow<List<DailyFoodLog>>

    @Query("SELECT * FROM daily_food_logs WHERE dateString >= :startDate AND dateString <= :endDate ORDER BY dateString ASC, timestampMillis ASC")
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyFoodLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyFoodLog): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLogs(logs: List<DailyFoodLog>)

    @Query("DELETE FROM daily_food_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("SELECT COUNT(*) FROM daily_food_logs")
    suspend fun getLogCount(): Int

    // --- Weight Logs ---
    @Query("SELECT * FROM weight_logs ORDER BY timestampMillis DESC")
    fun getAllWeightLogs(): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs ORDER BY timestampMillis DESC LIMIT 1")
    fun getLatestWeightLog(): Flow<WeightLog?>

    @Query("SELECT * FROM weight_logs ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLatestWeightLogSync(): WeightLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: WeightLog): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWeightLogs(logs: List<WeightLog>)

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteWeightLogById(id: Long)

    @Query("SELECT COUNT(*) FROM weight_logs")
    suspend fun getWeightLogCount(): Int
}
