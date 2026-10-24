package com.poorrat.gotcha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.poorrat.gotcha.data.local.AppDatabase
import com.poorrat.gotcha.alarm.SupplementAlarmScheduler
import com.poorrat.gotcha.data.repository.NutritionRepository
import com.poorrat.gotcha.data.repository.SupplementRepository
import com.poorrat.gotcha.data.repository.WorkoutRepository
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.poorrat.gotcha.ui.shop.ShopNotificationHelper
import com.poorrat.gotcha.ui.shop.ShopUnavailableDialog
import com.poorrat.gotcha.ui.nutrition.NutritionScreen
import com.poorrat.gotcha.ui.nutrition.NutritionViewModel
import com.poorrat.gotcha.ui.supplements.SupplementsScreen
import com.poorrat.gotcha.ui.supplements.SupplementsViewModel
import com.poorrat.gotcha.ui.theme.PoorRatTheme
import com.poorrat.gotcha.ui.weight.WeightScreen
import com.poorrat.gotcha.ui.weight.WeightViewModel
import com.poorrat.gotcha.ui.workout.WorkoutScreen
import com.poorrat.gotcha.ui.workout.WorkoutViewModel

class MainActivity : ComponentActivity() {

    private val nutritionRepository by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        NutritionRepository(database.nutritionDao())
    }

    private val workoutRepository by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        WorkoutRepository(database.workoutDao())
    }

    private val supplementRepository by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        SupplementRepository(database.supplementDao())
    }

    private val alarmScheduler by lazy {
        SupplementAlarmScheduler(applicationContext)
    }

    private val nutritionViewModel: NutritionViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NutritionViewModel(nutritionRepository) as T
            }
        }
    }

    private val weightViewModel: WeightViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WeightViewModel(nutritionRepository) as T
            }
        }
    }

    private val workoutViewModel: WorkoutViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkoutViewModel(workoutRepository) as T
            }
        }
    }

    private val supplementsViewModel: SupplementsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SupplementsViewModel(supplementRepository, alarmScheduler) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(true) }
            PoorRatTheme(darkTheme = isDarkMode) {
                val context = LocalContext.current
                var currentModule by rememberSaveable { mutableStateOf("Alimentación") }
                var showShopUnavailableDialog by rememberSaveable { mutableStateOf(false) }
                val toggleTheme = { isDarkMode = !isDarkMode }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    ShopNotificationHelper.triggerShopNotification(context)
                }

                val handleShopClick: () -> Unit = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            ShopNotificationHelper.triggerShopNotification(context)
                        } else {
                            ShopNotificationHelper.setReminderMarked(context, true)
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        ShopNotificationHelper.triggerShopNotification(context)
                    }

                    showShopUnavailableDialog = true
                    Toast.makeText(
                        context,
                        "La opción de tienda no está disponible aún. ¡Aviso registrado!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                if (currentModule != "Alimentación") {
                    BackHandler {
                        currentModule = "Alimentación"
                    }
                }

                when (currentModule) {
                    "Peso" -> {
                        WeightScreen(
                            viewModel = weightViewModel,
                            onNavigateToNutrition = { currentModule = "Alimentación" },
                            onNavigateToWorkout = { currentModule = "Entreno" },
                            onNavigateToSupplements = { currentModule = "Suplementos" },
                            onNavigateToShop = { handleShopClick() },
                            onToggleTheme = toggleTheme
                        )
                    }
                    "Entreno" -> {
                        WorkoutScreen(
                            viewModel = workoutViewModel,
                            onNavigateToNutrition = { currentModule = "Alimentación" },
                            onNavigateToWeight = { currentModule = "Peso" },
                            onNavigateToSupplements = { currentModule = "Suplementos" },
                            onNavigateToShop = { handleShopClick() },
                            onToggleTheme = toggleTheme
                        )
                    }
                    "Suplementos" -> {
                        SupplementsScreen(
                            viewModel = supplementsViewModel,
                            onNavigateToTab = { tab ->
                                if (tab == "Tienda") {
                                    handleShopClick()
                                } else if (tab != "Suplementos") {
                                    currentModule = tab
                                }
                            },
                            onToggleTheme = toggleTheme
                        )
                    }
                    "Tienda" -> {
                        com.poorrat.gotcha.ui.shop.ShopScreen(
                            onNavigateToTab = { tab ->
                                if (tab != "Tienda") {
                                    currentModule = tab
                                } else {
                                    handleShopClick()
                                }
                            },
                            onToggleTheme = toggleTheme
                        )
                    }
                    else -> {
                        NutritionScreen(
                            viewModel = nutritionViewModel,
                            onNavigateToWeight = { currentModule = "Peso" },
                            onNavigateToWorkout = { currentModule = "Entreno" },
                            onNavigateToSupplements = { currentModule = "Suplementos" },
                            onNavigateToShop = { handleShopClick() },
                            onToggleTheme = toggleTheme
                        )
                    }
                }

                if (showShopUnavailableDialog) {
                    ShopUnavailableDialog(
                        onDismiss = { showShopUnavailableDialog = false },
                        onSendNotification = {
                            ShopNotificationHelper.triggerShopNotification(context)
                        }
                    )
                }
            }
        }
    }
}