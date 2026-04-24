package com.example.studyplannerai

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.i("MyApp", "Firebase initialized. App Check disabled for Free tier.")
    }

    private companion object {
        const val TAG = "MyApp"
    }
}
