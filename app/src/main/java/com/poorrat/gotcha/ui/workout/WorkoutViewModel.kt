package com.poorrat.gotcha.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poorrat.gotcha.data.local.entity.RoutineDayEntity
import com.poorrat.gotcha.data.local.entity.RoutineExerciseEntity
import com.poorrat.gotcha.data.local.entity.WorkoutSessionLogEntity
import com.poorrat.gotcha.data.repository.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class WorkoutDatabaseData(
    val days: List<RoutineDayEntity>,
    val exercises: List<RoutineExerciseEntity>
)

private data class WorkoutUiInputData(
    val activeSheet: WorkoutActiveSheet,
    val isSessionRunning: Boolean,
    val sessionElapsedSeconds: Long,
    val sessionExerciseInputs: Map<Long, ExerciseSessionInput>,
    val detailSelectedDay: Int,
    val scheduleSelectedDay: Int,
    val scheduleRoutineName: String,
    val scheduleBodyPart: String,
    val scheduleExercises: List<RoutineExerciseEntity>,
    val newExName: String,
    val newExSets: String,
    val newExReps: String,
    val newExWeight: String
)

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val currentDayOfWeek = LocalDate.now().dayOfWeek.value // 1=Lunes .. 7=Domingo

    private val _activeSheet = MutableStateFlow(WorkoutActiveSheet.NONE)

    // Stopwatch & Session state
    private val _isSessionRunning = MutableStateFlow(false)
    private val _sessionElapsedSeconds = MutableStateFlow(0L)
    private val _sessionExerciseInputs = MutableStateFlow<Map<Long, ExerciseSessionInput>>(emptyMap())
    private var stopwatchJob: Job? = null

    // Detail Modal state
    private val _detailSelectedDay = MutableStateFlow(currentDayOfWeek)

    // Schedule Modal state
    private val _scheduleSelectedDay = MutableStateFlow(currentDayOfWeek)
    private val _scheduleRoutineName = MutableStateFlow("Upper de Fuerza")
    private val _scheduleBodyPart = MutableStateFlow("Upper")
    private val _scheduleExercises = MutableStateFlow<List<RoutineExerciseEntity>>(emptyList())
    private val _newExName = MutableStateFlow("")
    private val _newExSets = MutableStateFlow("4")
    private val _newExReps = MutableStateFlow("8-10")
    private val _newExWeight = MutableStateFlow("85")

    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    private val dbDataFlow = combine(
        repository.allRoutineDays,
        repository.allExercises
    ) { days, exercises ->
        WorkoutDatabaseData(days, exercises)
    }

    private val sessionStateFlow = combine(
        _activeSheet,
        _isSessionRunning,
        _sessionElapsedSeconds,
        _sessionExerciseInputs
    ) { sheet, running, elapsed, inputs ->
        Tuple4(sheet, running, elapsed, inputs)
    }

    private val detailAndScheduleStateFlow = combine(
        _detailSelectedDay,
        _scheduleSelectedDay,
        _scheduleRoutineName,
        _scheduleBodyPart,
        _scheduleExercises
    ) { detailDay, schedDay, name, part, exList ->
        Tuple5(detailDay, schedDay, name, part, exList)
    }

    private val newExInputsFlow = combine(
        _newExName,
        _newExSets,
        _newExReps,
        _newExWeight
    ) { name, sets, reps, weight ->
        Tuple4(name, sets, reps, weight)
    }

    private val inputsCombinedFlow = combine(
        sessionStateFlow,
        detailAndScheduleStateFlow,
        newExInputsFlow
    ) { s1, s2, s3 ->
        WorkoutUiInputData(
            activeSheet = s1.first,
            isSessionRunning = s1.second,
            sessionElapsedSeconds = s1.third,
            sessionExerciseInputs = s1.fourth,
            detailSelectedDay = s2.first,
            scheduleSelectedDay = s2.second,
            scheduleRoutineName = s2.third,
            scheduleBodyPart = s2.fourth,
            scheduleExercises = s2.fifth,
            newExName = s3.first,
            newExSets = s3.second,
            newExReps = s3.third,
            newExWeight = s3.fourth
        )
    }

    val uiState: StateFlow<WorkoutUiState> = combine(
        dbDataFlow,
        inputsCombinedFlow
    ) { db, inputs ->

        val todayDay = db.days.find { it.dayOfWeek == currentDayOfWeek }
            ?: RoutineDayEntity(currentDayOfWeek, getDayLabel(currentDayOfWeek), "Upper de fuerza", "Upper")

        val todayEx = db.exercises.filter { it.dayOfWeek == currentDayOfWeek }
            .sortedBy { it.exerciseOrder }

        val detailDay = db.days.find { it.dayOfWeek == inputs.detailSelectedDay }
            ?: RoutineDayEntity(inputs.detailSelectedDay, getDayLabel(inputs.detailSelectedDay), "", "Descanso")

        val detailEx = db.exercises.filter { it.dayOfWeek == inputs.detailSelectedDay }
            .sortedBy { it.exerciseOrder }

        WorkoutUiState(
            currentDayOfWeek = currentDayOfWeek,
            todayRoutineDay = todayDay,
            todayExercises = todayEx,
            allRoutineDays = db.days,
            allExercises = db.exercises,
            activeSheet = inputs.activeSheet,
            isSessionRunning = inputs.isSessionRunning,
            sessionElapsedSeconds = inputs.sessionElapsedSeconds,
            sessionExerciseInputs = inputs.sessionExerciseInputs,
            detailSelectedDayOfWeek = inputs.detailSelectedDay,
            detailRoutineDay = detailDay,
            detailExercises = detailEx,
            scheduleSelectedDayOfWeek = inputs.scheduleSelectedDay,
            scheduleRoutineName = inputs.scheduleRoutineName,
            scheduleBodyPart = inputs.scheduleBodyPart,
            scheduleExercises = inputs.scheduleExercises,
            newExerciseName = inputs.newExName,
            newExerciseSets = inputs.newExSets,
            newExerciseReps = inputs.newExReps,
            newExerciseWeight = inputs.newExWeight
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorkoutUiState(currentDayOfWeek = currentDayOfWeek)
    )

    // --- Modal Navigation ---
    fun openSheet(sheet: WorkoutActiveSheet) {
        _activeSheet.value = sheet
        if (sheet == WorkoutActiveSheet.ROUTINE_DETAIL) {
            _detailSelectedDay.value = currentDayOfWeek
        } else if (sheet == WorkoutActiveSheet.SCHEDULE_ROUTINE) {
            loadDayToSchedule(currentDayOfWeek)
        }
    }

    fun closeSheet() {
        _activeSheet.value = WorkoutActiveSheet.NONE
    }

    // --- Active Session & Stopwatch ---
    fun startOrResumeSession() {
        val today = uiState.value.todayRoutineDay
        if (today?.bodyPart == "Descanso") return // Disabled on rest days

        if (!_isSessionRunning.value) {
            _isSessionRunning.value = true
            startStopwatch()

            // Initialize inputs for today's exercises if not already initialized
            val currentMap = _sessionExerciseInputs.value.toMutableMap()
            val exercises = uiState.value.todayExercises
            for (ex in exercises) {
                if (!currentMap.containsKey(ex.id)) {
                    currentMap[ex.id] = ExerciseSessionInput(
                        isCompleted = false,
                        achievedWeightText = ex.targetWeightKg.toString().replace(".0", ""),
                        achievedRepsText = ex.targetReps
                    )
                }
            }
            _sessionExerciseInputs.value = currentMap
        }

        _activeSheet.value = WorkoutActiveSheet.PROGRESS_ACTIVE_SESSION
    }

    private fun startStopwatch() {
        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (isActive && _isSessionRunning.value) {
                delay(1000)
                _sessionElapsedSeconds.value += 1
            }
        }
    }

    fun updateExerciseCompleted(exerciseId: Long, completed: Boolean) {
        val current = _sessionExerciseInputs.value.toMutableMap()
        val existing = current[exerciseId] ?: ExerciseSessionInput()
        current[exerciseId] = existing.copy(isCompleted = completed)
        _sessionExerciseInputs.value = current
    }

    fun updateExerciseAchievedWeight(exerciseId: Long, weight: String) {
        val current = _sessionExerciseInputs.value.toMutableMap()
        val existing = current[exerciseId] ?: ExerciseSessionInput()
        current[exerciseId] = existing.copy(achievedWeightText = weight)
        _sessionExerciseInputs.value = current
    }

    fun updateExerciseAchievedReps(exerciseId: Long, reps: String) {
        val current = _sessionExerciseInputs.value.toMutableMap()
        val existing = current[exerciseId] ?: ExerciseSessionInput()
        current[exerciseId] = existing.copy(achievedRepsText = reps)
        _sessionExerciseInputs.value = current
    }

    fun finishAndSaveSession() {
        val elapsed = _sessionElapsedSeconds.value
        val inputs = _sessionExerciseInputs.value

        viewModelScope.launch {
            // Find exercises marked as completed
            val updates = mutableListOf<Triple<Long, Float, String>>()
            for ((id, input) in inputs) {
                if (input.isCompleted) {
                    val w = input.achievedWeightText.replace(',', '.').toFloatOrNull() ?: 0f
                    val r = input.achievedRepsText.ifBlank { "10" }
                    updates.add(Triple(id, w, r))
                }
            }

            val session = WorkoutSessionLogEntity(
                dayOfWeek = currentDayOfWeek,
                durationSeconds = elapsed,
                timestampMillis = System.currentTimeMillis()
            )

            repository.finishWorkoutSession(session, updates)

            // Reset session
            stopwatchJob?.cancel()
            _isSessionRunning.value = false
            _sessionElapsedSeconds.value = 0L
            _sessionExerciseInputs.value = emptyMap()
            closeSheet()
        }
    }

    // --- Detail Modal ---
    fun selectDetailDay(dayOfWeek: Int) {
        _detailSelectedDay.value = dayOfWeek
    }

    // --- Schedule Modal ---
    fun loadDayToSchedule(dayOfWeek: Int) {
        _scheduleSelectedDay.value = dayOfWeek
        val day = uiState.value.allRoutineDays.find { it.dayOfWeek == dayOfWeek }
        _scheduleRoutineName.value = day?.routineName ?: "Upper de Fuerza"
        _scheduleBodyPart.value = day?.bodyPart ?: "Upper"

        val ex = uiState.value.allExercises.filter { it.dayOfWeek == dayOfWeek }
            .sortedBy { it.exerciseOrder }
        _scheduleExercises.value = ex
    }

    fun updateScheduleRoutineName(name: String) {
        _scheduleRoutineName.value = name
    }

    fun updateScheduleBodyPart(bodyPart: String) {
        _scheduleBodyPart.value = bodyPart
        if (bodyPart == "Descanso") {
            _scheduleExercises.value = emptyList()
        }
    }

    fun updateNewExName(name: String) { _newExName.value = name }
    fun updateNewExSets(sets: String) { _newExSets.value = sets }
    fun updateNewExReps(reps: String) { _newExReps.value = reps }
    fun updateNewExWeight(weight: String) { _newExWeight.value = weight }

    fun addExerciseToSchedule() {
        val name = _newExName.value.trim()
        if (name.isBlank() || _scheduleBodyPart.value == "Descanso") return

        val sets = _newExSets.value.toIntOrNull() ?: 4
        val reps = _newExReps.value.trim().ifBlank { "8-10" }
        val weight = _newExWeight.value.replace(',', '.').toFloatOrNull() ?: 0f

        val currentList = _scheduleExercises.value.toMutableList()
        val newOrder = currentList.size + 1
        currentList.add(
            RoutineExerciseEntity(
                dayOfWeek = _scheduleSelectedDay.value,
                exerciseOrder = newOrder,
                name = name,
                targetSets = sets,
                targetReps = reps,
                targetWeightKg = weight
            )
        )
        _scheduleExercises.value = currentList
        _newExName.value = ""
    }

    fun removeExerciseFromSchedule(index: Int) {
        val currentList = _scheduleExercises.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            // Re-index orders
            val reordered = currentList.mapIndexed { idx, item ->
                item.copy(exerciseOrder = idx + 1)
            }
            _scheduleExercises.value = reordered
        }
    }

    fun saveSchedule() {
        val dayNum = _scheduleSelectedDay.value
        val name = _scheduleRoutineName.value.trim().ifBlank { "Rutina Día $dayNum" }
        val bodyPart = _scheduleBodyPart.value

        val dayEntity = RoutineDayEntity(
            dayOfWeek = dayNum,
            dayLabel = getDayLabel(dayNum),
            routineName = name,
            bodyPart = bodyPart
        )

        val exercisesToSave = if (bodyPart == "Descanso") {
            emptyList()
        } else {
            _scheduleExercises.value.mapIndexed { idx, ex ->
                ex.copy(dayOfWeek = dayNum, exerciseOrder = idx + 1)
            }
        }

        viewModelScope.launch {
            repository.saveDaySchedule(dayEntity, exercisesToSave)
            closeSheet()
        }
    }

    companion object {
        fun getDayLabel(day: Int): String {
            return when (day) {
                1 -> "Día 1 · Lunes"
                2 -> "Día 2 · Martes"
                3 -> "Día 3 · Miércoles"
                4 -> "Día 4 · Jueves"
                5 -> "Día 5 · Viernes"
                6 -> "Día 6 · Sábado"
                7 -> "Día 7 · Domingo"
                else -> "Día $day"
            }
        }
    }
}

// Helper tuples for clean combine
data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
data class Tuple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
