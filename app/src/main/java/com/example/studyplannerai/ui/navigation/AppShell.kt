package com.example.studyplannerai.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyplannerai.ui.planner.PlannerScreen
import com.example.studyplannerai.ui.profile.ProfileScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.studyplannerai.viewmodel.planner.PlannerViewModel
import com.example.studyplannerai.viewmodel.auth.AuthViewModel
import com.example.studyplannerai.viewmodel.settings.SettingsViewModel
import com.example.studyplannerai.viewmodel.task.TaskViewModel
import com.example.studyplannerai.viewmodel.permission.PermissionViewModel
import com.example.studyplannerai.ui.progress.ProgressScreen
import com.example.studyplannerai.viewmodel.progress.ProgressViewModel

private enum class AppTab(val label: String) {
    Home("Home"),
    Progress("Progress"),
    Profile("Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val plannerViewModel: PlannerViewModel = hiltViewModel()
    val progressViewModel: ProgressViewModel = hiltViewModel()
    val taskViewModel: TaskViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val permissionViewModel: PermissionViewModel = hiltViewModel()

    val taskState by taskViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val permissionState by permissionViewModel.uiState.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }

    LaunchedEffect(taskState.message, taskState.errorMessage, settingsState.message) {
        taskState.message?.let { snackbarHostState.showSnackbar(it); taskViewModel.clearMessage() }
        taskState.errorMessage?.let { snackbarHostState.showSnackbar(it); taskViewModel.clearMessage() }
        settingsState.message?.let { snackbarHostState.showSnackbar(it); settingsViewModel.clearMessage() }
        permissionState.snackbarMessage?.let { snackbarHostState.showSnackbar(it); permissionViewModel.clearSnackbarMessage() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(selectedTab.label) },
                actions = {
                    IconButton(onClick = { authViewModel.logOut() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    AppTab.Home -> Icons.Filled.CalendarToday
                                    AppTab.Progress -> Icons.Filled.TrendingUp
                                    AppTab.Profile -> Icons.Filled.Person
                                },
                                contentDescription = tab.label
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            AppTab.Home -> PlannerScreen(
                plannerViewModel = plannerViewModel,
                innerPadding = paddingValues
            )
            AppTab.Progress -> ProgressScreen(
                viewModel = progressViewModel,
                innerPadding = paddingValues
            )
            AppTab.Profile -> ProfileScreen(
                authViewModel = authViewModel,
                taskViewModel = taskViewModel,
                innerPadding = paddingValues
            )
        }
    }
}
