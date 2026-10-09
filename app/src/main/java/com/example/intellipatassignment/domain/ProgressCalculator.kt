package com.example.intellipatassignment.domain

object ProgressCalculator {
    fun percent(completed: Int, total: Int): Int {
        if (total <= 0) return 0
        val safeCompleted = completed.coerceIn(0, total)
        val rounded = (safeCompleted * 100 + total / 2) / total
        return when {
            safeCompleted == total -> 100
            safeCompleted == 0 -> 0
            else -> rounded.coerceIn(1, 99)
        }
    }
}
