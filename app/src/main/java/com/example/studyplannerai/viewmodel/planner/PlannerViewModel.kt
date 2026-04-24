package com.example.studyplannerai.viewmodel.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplannerai.data.model.StudyPlanItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.studyplannerai.core.util.Resource
import com.example.studyplannerai.domain.repository.AiRepository
import com.example.studyplannerai.domain.repository.StudyRepository
import com.example.studyplannerai.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class PlannerUiState(
    val isLoading: Boolean = false,
    val studyPlan: List<StudyPlanItem> = emptyList(),
    val temporaryPlan: List<StudyPlanItem> = emptyList(),
    val history: List<StudyPlanItem> = emptyList(),
    val topics: List<String> = emptyList(),
    val selectedTopics: List<String> = emptyList(),
    val errorMessage: String? = null,
    val progress: Float = 0f,
    val isPlanAccepted: Boolean = false
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val studyRepository: StudyRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        loadSavedPlan()
    }

    fun loadSavedPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = studyRepository.getSavedPlan()) {
                is Resource.Success -> updatePlanAndProgress(result.data ?: emptyList())
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun getTopics(subject: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = aiRepository.getTopicsForSubject(subject)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, topics = result.data ?: emptyList()) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun generateFinalSchedule(
        subject: String,
        selectedTopics: List<String>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isPlanAccepted = false, selectedTopics = selectedTopics) }
            when (val result = aiRepository.generateSchedule(selectedTopics, 4, "Flexible", 15)) {
                is Resource.Success -> {
                    val plan = (result.data ?: emptyList()).map { it.copy(planId = subject) }
                    _uiState.update { it.copy(isLoading = false, temporaryPlan = plan) }
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun acceptPlan() {
        viewModelScope.launch {
            val planToSave = _uiState.value.temporaryPlan
            if (planToSave.isEmpty()) return@launch
            
            _uiState.update { it.copy(isLoading = true) }
            when (val result = studyRepository.savePlan(planToSave)) {
                is Resource.Success -> {
                    updatePlanAndProgress(planToSave)
                    _uiState.update { it.copy(temporaryPlan = emptyList(), isPlanAccepted = true) }
                    
                    // Schedule reminders for each task
                    planToSave.forEach { item ->
                        // Note: In a real app, you'd convert day/time_slot to a Long timestamp.
                        // For this demo, we'll assume the scheduler handles it or we'll add a helper later.
                        // reminderScheduler.schedule(item.toTask()) 
                    }
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun regeneratePlan(subject: String) {
        generateFinalSchedule(subject, _uiState.value.selectedTopics)
    }

    fun toggleTaskCompletion(item: StudyPlanItem) {
        viewModelScope.launch {
            val isCompleted = !item.isCompleted
            when (val result = studyRepository.updateTaskStatus(item.id, isCompleted)) {
                is Resource.Success -> {
                    loadSavedPlan() // Refresh lists
                }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteSingleTask(taskId: String) {
        viewModelScope.launch {
            studyRepository.deleteTask(taskId)
            loadSavedPlan()
        }
    }

    fun addCustomTask(day: String, title: String, topic: String, duration: Int) {
        viewModelScope.launch {
            val newTask = StudyPlanItem(
                id = "${System.currentTimeMillis()}",
                day = day,
                time_slot = "Custom",
                duration_minutes = duration,
                topic = topic,
                task = title,
                status = "pending"
            )
            val currentPlan = _uiState.value.studyPlan.toMutableList()
            currentPlan.add(newTask)
            studyRepository.savePlan(listOf(newTask))
            updatePlanAndProgress(currentPlan)
        }
    }

    private fun updatePlanAndProgress(plan: List<StudyPlanItem>) {
        val activeTasks = plan.filter { !it.isCompleted && it.status != "completed" }
        val completedCount = plan.count { it.isCompleted || it.status == "completed" }
        val totalCount = plan.size
        val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
        
        _uiState.update { 
            it.copy(
                isLoading = false, 
                studyPlan = activeTasks.sortedBy { p -> p.day },
                progress = progress
            ) 
        }
    }
}
