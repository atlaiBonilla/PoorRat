package com.poorrat.gotchadev

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.poorrat.gotchadev.alarm.SupplementAlarmScheduler
import com.poorrat.gotchadev.data.firebase.FirebaseSyncRepository
import com.poorrat.gotchadev.data.firebase.models.FirebaseUserProfile
import com.poorrat.gotchadev.data.local.AppDatabase
import com.poorrat.gotchadev.data.preferences.UserPreferencesRepository
import com.poorrat.gotchadev.data.repository.NutritionRepository
import com.poorrat.gotchadev.data.repository.SupplementRepository
import com.poorrat.gotchadev.data.repository.WorkoutRepository
import com.poorrat.gotchadev.ui.auth.AuthScreen
import com.poorrat.gotchadev.ui.nutrition.NutritionScreen
import com.poorrat.gotchadev.ui.nutrition.NutritionViewModel
import com.poorrat.gotchadev.ui.settings.SettingsBottomSheet
import com.poorrat.gotchadev.ui.shop.ShopNotificationHelper
import com.poorrat.gotchadev.ui.shop.ShopUnavailableDialog
import com.poorrat.gotchadev.ui.supplements.SupplementsScreen
import com.poorrat.gotchadev.ui.supplements.SupplementsViewModel
import com.poorrat.gotchadev.ui.theme.PoorRatTheme
import com.poorrat.gotchadev.ui.weight.WeightScreen
import com.poorrat.gotchadev.ui.weight.WeightViewModel
import com.poorrat.gotchadev.ui.workout.WorkoutScreen
import com.poorrat.gotchadev.ui.workout.WorkoutViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val firebaseSyncRepository by lazy {
        FirebaseSyncRepository(applicationContext)
    }

    private val nutritionRepository by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        NutritionRepository(database.nutritionDao(), firebaseSyncRepository)
    }

    private val workoutRepository by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        WorkoutRepository(database.workoutDao(), firebaseSyncRepository)
    }

    private val supplementRepository by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        SupplementRepository(database.supplementDao(), firebaseSyncRepository)
    }

    private val alarmScheduler by lazy {
        SupplementAlarmScheduler(applicationContext)
    }

    private val userPreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
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
                return SupplementsViewModel(supplementRepository, alarmScheduler, nutritionRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.poorrat.gotchadev.data.firebase.configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val preferences by userPreferencesRepository.preferences.collectAsStateWithLifecycle()
            val isDarkMode = preferences.isDarkMode

            PoorRatTheme(darkTheme = isDarkMode) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val auth = Firebase.auth
                val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
                val toggleTheme = {
                    userPreferencesRepository.setDarkMode(!isDarkMode)
                }

                if (currentUser == null) {
                    // Auth gate: user is unauthenticated
                    AuthScreen(
                        onAuthSuccess = {
                            // Handled reactively by authStateFlow
                        },
                        onToggleTheme = toggleTheme
                    )
                } else {
                    val uid = currentUser?.uid ?: ""
                    var currentModule by rememberSaveable { mutableStateOf("Alimentación") }
                    var showShopUnavailableDialog by rememberSaveable { mutableStateOf(false) }
                    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
                    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    val openSettings = {
                        showSettingsSheet = true
                    }

                    val syncAllData: suspend () -> Unit = {
                        val activeUid = auth.currentUser?.uid ?: ""
                        if (activeUid.isNotEmpty()) {
                            val email = auth.currentUser?.email ?: ""
                            val name = auth.currentUser?.displayName ?: "Usuario PoorRat"
                            val existingProfile = firebaseSyncRepository.getUserProfile(activeUid)
                            firebaseSyncRepository.saveUserProfile(
                                (existingProfile ?: FirebaseUserProfile(id = activeUid, userId = activeUid)).copy(
                                    email = email,
                                    displayName = name,
                                    unitSystemWeight = preferences.weightUnit.code,
                                    unitSystemHeight = preferences.heightUnit.code
                                )
                            )
                            // 1. Restore/Sync goals, targets, target weight, daily and weekly food logs, and weigh-ins
                            nutritionRepository.syncWithCloud()
                            // 2. Restore/Sync supplements, dosages, times, and daily intake links
                            supplementRepository.syncWithCloud()
                            // 3. Restore/Sync workout routines, days of the week, and exercises
                            workoutRepository.syncWithCloud()
                        }
                    }

                    // Background auto-sync of all user data (nutrition, food logs, supplements, routines, weight) on launch
                    LaunchedEffect(uid) {
                        if (uid.isNotEmpty()) {
                            try {
                                syncAllData()
                            } catch (_: Exception) {
                                // Silent fallback to local cache
                            }
                        }
                    }

                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ ->
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
                                onToggleTheme = toggleTheme,
                                onLogoClick = openSettings,
                                weightUnit = preferences.weightUnit
                            )
                        }
                        "Entreno" -> {
                            WorkoutScreen(
                                viewModel = workoutViewModel,
                                onNavigateToNutrition = { currentModule = "Alimentación" },
                                onNavigateToWeight = { currentModule = "Peso" },
                                onNavigateToSupplements = { currentModule = "Suplementos" },
                                onNavigateToShop = { handleShopClick() },
                                onToggleTheme = toggleTheme,
                                onLogoClick = openSettings
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
                                onToggleTheme = toggleTheme,
                                onLogoClick = openSettings
                            )
                        }
                        "Tienda" -> {
                            com.poorrat.gotchadev.ui.shop.ShopScreen(
                                onNavigateToTab = { tab ->
                                    if (tab != "Tienda") {
                                        currentModule = tab
                                    } else {
                                        handleShopClick()
                                    }
                                },
                                onToggleTheme = toggleTheme,
                                onLogoClick = openSettings
                            )
                        }
                        else -> {
                            NutritionScreen(
                                viewModel = nutritionViewModel,
                                onNavigateToWeight = { currentModule = "Peso" },
                                onNavigateToWorkout = { currentModule = "Entreno" },
                                onNavigateToSupplements = { currentModule = "Suplementos" },
                                onNavigateToShop = { handleShopClick() },
                                onToggleTheme = toggleTheme,
                                onLogoClick = openSettings
                            )
                        }
                    }

                    if (showSettingsSheet) {
                        SettingsBottomSheet(
                            sheetState = settingsSheetState,
                            preferences = preferences,
                            onWeightUnitChange = { userPreferencesRepository.setWeightUnit(it) },
                            onHeightUnitChange = { userPreferencesRepository.setHeightUnit(it) },
                            onToggleTheme = toggleTheme,
                            onSignOut = {
                                scope.launch {
                                    try {
                                        syncAllData()
                                    } catch (_: Exception) {
                                    }
                                    auth.signOut()
                                    nutritionRepository.clearUserData()
                                    workoutRepository.clearUserData()
                                    supplementRepository.clearUserData()
                                    showSettingsSheet = false
                                }
                            },
                            onDeleteAccount = {
                                scope.launch {
                                    try {
                                        val uid = auth.currentUser?.uid
                                        if (uid != null) {
                                            firebaseSyncRepository.deleteAllUserData(uid)
                                        }
                                        auth.currentUser?.delete()
                                    } catch (_: Exception) {
                                    }
                                    auth.signOut()
                                    nutritionRepository.clearUserData()
                                    workoutRepository.clearUserData()
                                    supplementRepository.clearUserData()
                                    showSettingsSheet = false
                                    Toast.makeText(context, "Cuenta y datos eliminados correctamente", Toast.LENGTH_LONG).show()
                                }
                            },
                            onDismiss = { showSettingsSheet = false },
                            onSyncAll = { syncAllData() },
                            syncRepository = firebaseSyncRepository
                        )
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
}