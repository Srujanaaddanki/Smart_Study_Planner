package com.example.studyplannerai.viewmodel.permission

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplannerai.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PermissionUiState(
    val showNotificationRationale: Boolean = false,
    val showAlarmRationale: Boolean = false,
    val isPermissionRequested: Boolean = false, // From repository
    val snackbarMessage: String? = null
)

class PermissionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                _uiState.update { 
                    it.copy(isPermissionRequested = settings.isPermissionRequested)
                }
            }
        }
    }

    fun onNotificationRationaleDismissed() {
        _uiState.update { it.copy(showNotificationRationale = false) }
    }

    fun onAlarmRationaleDismissed() {
        _uiState.update { it.copy(showAlarmRationale = false) }
    }

    fun showNotificationRationale() {
        _uiState.update { it.copy(showNotificationRationale = true) }
    }

    fun showAlarmRationale() {
        _uiState.update { it.copy(showAlarmRationale = true) }
    }

    fun setPermissionRequested() {
        viewModelScope.launch {
            repository.updatePermissionRequested(true)
        }
    }

    fun onPermissionResult(isGranted: Boolean, permissionName: String) {
        android.util.Log.d("PermissionViewModel", "Permission result for $permissionName: Granted=$isGranted")
        val message = if (isGranted) {
            "$permissionName permission granted!"
        } else {
            "$permissionName permission denied. Reminders may not work."
        }
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
