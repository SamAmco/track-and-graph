package com.samco.trackandgraph.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `reminders_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `display_index` INTEGER NOT NULL, 
                `name` TEXT NOT NULL,
                `time` TEXT NOT NULL,
                `checked_days` TEXT NOT NULL
            )""".trimMargin())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_table_id` ON `reminders_table` (`id`)")
    }
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE line_graphs_table ADD y_range_type INTEGER NOT NULL DEFAULT  0")
        database.execSQL("ALTER TABLE line_graphs_table ADD y_from REAL NOT NULL DEFAULT  0")
        database.execSQL("ALTER TABLE line_graphs_table ADD y_to REAL NOT NULL DEFAULT  1")
    }
}

val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val lineGraphsCursor = database.query("SELECT * FROM line_graphs_table")
        val updates = mutableListOf<String>()
        while (lineGraphsCursor.moveToNext()) {
            val id = lineGraphsCursor.getLong(0)
            val oldFeaturesString = lineGraphsCursor.getString(2)
            val oldFeatures = oldFeaturesString.split(splitChars1)
            val newFeatures = oldFeatures.map { f ->
                val params = f.split(splitChars2).toMutableList()
                params.add(5, "0")
                params.joinToString(splitChars2)
            }
            val newFeaturesString = newFeatures.joinToString(splitChars1)
            updates.add("UPDATE line_graphs_table SET features='$newFeaturesString' WHERE id=$id")
        }
        if (updates.size > 0) updates.forEach { database.execSQL(it) }
    }
}
