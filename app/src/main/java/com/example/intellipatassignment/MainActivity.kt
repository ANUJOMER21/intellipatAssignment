package com.example.intellipatassignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.intellipatassignment.domain.repository.AuthRepository
import com.example.intellipatassignment.ui.navigation.AppNavigation
import com.example.intellipatassignment.ui.theme.IntellipatAssignmentTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val auth: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isLoggedIn = auth.isLoggedIn
        setContent {
            IntellipatAssignmentTheme {
                AppNavigation(isLoggedIn = isLoggedIn)
            }
        }
    }
}
