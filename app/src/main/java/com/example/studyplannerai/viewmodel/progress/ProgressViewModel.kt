package com.example.studyplannerai.viewmodel.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplannerai.core.util.Resource
import com.example.studyplannerai.data.model.StudyPlanItem
import com.example.studyplannerai.domain.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ProgressUiState(
    val isLoading: Boolean = false,
    val dailyProgress: Float = 0f,
    val weeklyProgress: Float = 0f,
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val streakCount: Int = 0,
    val hoursStudied: Float = 0f,
    val topicsCovered: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        fetchAnalytics()
    }

    fun fetchAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = studyRepository.getAllTasks()) {
                is Resource.Success -> {
                    val tasks = result.data ?: emptyList()
                    calculateStats(tasks)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun calculateStats(tasks: List<StudyPlanItem>) {
        if (tasks.isEmpty()) {
            _uiState.update { ProgressUiState(isLoading = false) }
            return
        }

        val today = LocalDate.now().toString()
        val startOfWeek = LocalDate.now().minusDays(7).toString()

        val todayTasks = tasks.filter { it.day == today }
        val weeklyTasks = tasks.filter { it.day >= startOfWeek }
        
        val dailyProgress = if (todayTasks.isNotEmpty()) {
            todayTasks.count { it.isCompleted }.toFloat() / todayTasks.size
        } else 0f

        val weeklyProgress = if (weeklyTasks.isNotEmpty()) {
            weeklyTasks.count { it.isCompleted }.toFloat() / weeklyTasks.size
        } else 0f

        val completedCount = tasks.count { it.isCompleted }
        val topicsCovered = tasks.filter { it.isCompleted }.map { it.topic }.distinct().size
        
        // Simplified streak logic
        val streak = calculateStreak(tasks)

        _uiState.update {
            it.copy(
                isLoading = false,
                dailyProgress = dailyProgress,
                weeklyProgress = weeklyProgress,
                completedTasks = completedCount,
                totalTasks = tasks.size,
                streakCount = streak,
                topicsCovered = topicsCovered,
                hoursStudied = completedCount * 1.0f // Assuming 1 hour per task for demo
            )
        }
    }

    private fun calculateStreak(tasks: List<StudyPlanItem>): Int {
        val completedDays = tasks.filter { it.isCompleted }
            .mapNotNull { 
                runCatching { LocalDate.parse(it.day) }.getOrNull()
            }
            .distinct()
            .sortedDescending()

        if (completedDays.isEmpty()) return 0

        var streak = 0
        var currentDay = LocalDate.now()

        // Check if today or yesterday was the last completed day to continue streak
        if (completedDays.first() < currentDay.minusDays(1)) return 0

        for (day in completedDays) {
            if (day == currentDay || day == currentDay.minusDays(1)) {
                streak++
                currentDay = day
            } else {
                break
            }
        }
        return streak
    }
}
