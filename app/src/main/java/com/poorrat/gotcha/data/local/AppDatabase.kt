package com.poorrat.gotcha.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.poorrat.gotcha.data.local.dao.NutritionDao
import com.poorrat.gotcha.data.local.dao.WorkoutDao
import com.poorrat.gotcha.data.local.entity.DailyFoodLog
import com.poorrat.gotcha.data.local.entity.FoodItem
import com.poorrat.gotcha.data.local.entity.RoutineDayEntity
import com.poorrat.gotcha.data.local.entity.RoutineExerciseEntity
import com.poorrat.gotcha.data.local.entity.UserNutritionConfig
import com.poorrat.gotcha.data.local.entity.WeightLog
import com.poorrat.gotcha.data.local.dao.SupplementDao
import com.poorrat.gotcha.data.local.entity.SupplementEntity
import com.poorrat.gotcha.data.local.entity.WorkoutSessionLogEntity

@Database(
    entities = [
        UserNutritionConfig::class,
        FoodItem::class,
        DailyFoodLog::class,
        WeightLog::class,
        RoutineDayEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionLogEntity::class,
        SupplementEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun nutritionDao(): NutritionDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun supplementDao(): SupplementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "poorrat_nutrition.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
