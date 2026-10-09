package com.example.intellipatassignment

import android.app.Application
import com.example.intellipatassignment.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import android.util.Log

class IntellipatApp : Application() {
    private companion object {
        const val TAG = "IntellipatApp"
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@IntellipatApp)
            workManagerFactory()
            modules(appModules)
        }
        Log.d(TAG, "Application started (mock backend = ${BuildConfig.USE_MOCK_BACKEND})")
    }
}
