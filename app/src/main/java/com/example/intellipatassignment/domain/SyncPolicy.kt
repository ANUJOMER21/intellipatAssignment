package com.example.intellipatassignment.domain

object SyncPolicy {
    const val STALE_AFTER_MS = 5 * 60 * 1000L

    fun isStale(lastSyncedAt: Long?, now: Long): Boolean =
        lastSyncedAt == null || now - lastSyncedAt > STALE_AFTER_MS
}
