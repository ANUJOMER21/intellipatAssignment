package com.example.intellipatassignment.domain.repository

interface SyncScheduler {
    fun syncWhenOnline()

    fun schedulePeriodicSync()

    fun cancelAll()
}
