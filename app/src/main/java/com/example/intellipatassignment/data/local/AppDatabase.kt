package com.example.intellipatassignment.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CourseEntity::class,
        LessonEntity::class,
        SyncStateEntity::class,
        RejectionEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao

    companion object {
        const val NAME = "learning.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sync_state` " +
                        "(`id` INTEGER NOT NULL, `lastSyncedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sync_rejections` " +
                        "(`lessonId` INTEGER NOT NULL, `title` TEXT NOT NULL, PRIMARY KEY(`lessonId`))",
                )
            }
        }
    }
}
