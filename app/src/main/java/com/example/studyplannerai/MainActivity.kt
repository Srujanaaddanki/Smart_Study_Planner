package com.example.studyplannerai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studyplannerai.reminder.NotificationHelper
import com.example.studyplannerai.ui.auth.AuthScreen
import com.example.studyplannerai.ui.navigation.AppShell
import com.example.studyplannerai.ui.splash.SplashScreen
import com.example.studyplannerai.ui.theme.StudyPlannerAiTheme
import com.example.studyplannerai.viewmodel.auth.AuthViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        enableEdgeToEdge()
        setContent {
            StudyPlannerAiTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val authViewModel: AuthViewModel = hiltViewModel()
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onTimeout = { showSplash = false })
    } else {
        if (authViewModel.isLoggedIn.value) {
            AppShell(authViewModel = authViewModel)
        } else {
            AuthScreen(
                viewModel = authViewModel,
                onNavigateHome = {}
            )
        }
    }
}

