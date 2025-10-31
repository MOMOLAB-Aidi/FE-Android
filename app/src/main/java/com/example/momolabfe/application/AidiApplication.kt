package com.example.momolabfe.application

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AidiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}