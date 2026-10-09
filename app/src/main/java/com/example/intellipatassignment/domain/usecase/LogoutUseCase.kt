package com.example.intellipatassignment.domain.usecase

import com.example.intellipatassignment.domain.repository.AuthRepository
import com.example.intellipatassignment.domain.repository.CourseRepository
import com.example.intellipatassignment.domain.repository.SyncScheduler

class LogoutUseCase(
    private val auth: AuthRepository,
    private val courses: CourseRepository,
    private val scheduler: SyncScheduler,
) {
    suspend operator fun invoke() {
        scheduler.cancelAll()
        auth.logout()
        courses.clearLocalData()
    }
}
