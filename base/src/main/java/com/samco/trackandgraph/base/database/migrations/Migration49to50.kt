package com.samco.trackandgraph.base.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_49_50 = object : Migration(49, 50) {
    override fun migrate(database: SupportSQLiteDatabase) {
        //Add column of type TEXT to trackers_table for suggestion_type
        database.execSQL("ALTER TABLE trackers_table ADD COLUMN suggestion_type TEXT NOT NULL DEFAULT 'VALUE_AND_LABEL'")
        //Add column of type TEXT to trackers_table for suggestion_order
        database.execSQL("ALTER TABLE trackers_table ADD COLUMN suggestion_order TEXT NOT NULL DEFAULT 'VALUE_ASCENDING'")
    }
}
