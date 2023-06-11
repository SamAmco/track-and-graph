package com.samco.trackandgraph.base.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_52_53 = object : Migration(52, 53) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_data_points_table_timestamp` ON `data_points_table` (`timestamp`)")
    }
}
