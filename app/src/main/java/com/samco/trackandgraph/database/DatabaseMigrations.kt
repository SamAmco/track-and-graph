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
        val updates = mutableListOf<Pair<String, List<String>>>()
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
            updates.add(Pair("UPDATE line_graphs_table SET features=? WHERE id=?",
                listOf(newFeaturesString, id.toString())))
        }
        if (updates.size > 0) updates.forEach { database.execSQL(it.first, it.second.toTypedArray()) }
    }
}

val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val lineGraphsCursor = database.query("SELECT * FROM line_graphs_table")
        val updates = mutableListOf<Pair<String, List<String>>>()
        while (lineGraphsCursor.moveToNext()) {
            val id = lineGraphsCursor.getLong(0)
            val oldFeaturesString = lineGraphsCursor.getString(2)
            val oldFeatures = oldFeaturesString.split(splitChars1)
            val newFeatures = oldFeatures.map { f ->
                val params = f.split(splitChars2).toMutableList()
                var plottingMode = params[4].toInt()
                if (plottingMode > 0) plottingMode += 1
                params[4] = plottingMode.toString()
                params.joinToString(splitChars2)
            }
            val newFeaturesString = newFeatures.joinToString(splitChars1)
            updates.add(Pair("UPDATE line_graphs_table SET features=? WHERE id=?",
                listOf(newFeaturesString, id.toString())))
        }
        if (updates.size > 0) updates.forEach { database.execSQL(it.first, it.second.toTypedArray()) }
    }
}

val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE features_table ADD has_default_value INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE features_table ADD default_value REAL NOT NULL DEFAULT 0")
        database.execSQL("UPDATE features_table SET type=1, has_default_value=1, default_value=1.0 WHERE type=2")
    }
}

val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE average_time_between_stat_table ADD discrete_values TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE time_since_last_stat_table ADD discrete_values TEXT NOT NULL DEFAULT ''")
        val updates = mutableListOf<Pair<String, List<String>>>()
        val avTimeCursor = database.query("SELECT * FROM average_time_between_stat_table")
        while (avTimeCursor.moveToNext()) {
            val id = avTimeCursor.getLong(0)
            val from = avTimeCursor.getString(3)
            val to = avTimeCursor.getString(4)
            val discreteValues = from + splitChars1 + to
            val query = "UPDATE average_time_between_stat_table SET discrete_values=? WHERE id=?"
            val args = listOf(discreteValues, id.toString())
            updates.add(Pair(query, args))
        }
        val timeSinceCursor = database.query("SELECT * FROM time_since_last_stat_table")
        while (timeSinceCursor.moveToNext()) {
            val id = timeSinceCursor.getLong(0)
            val from = timeSinceCursor.getString(3)
            val to = timeSinceCursor.getString(4)
            val discreteValues = from + splitChars1 + to
            val query = "UPDATE time_since_last_stat_table SET discrete_values=? WHERE id=?"
            val args = listOf(discreteValues, id.toString())
            updates.add(Pair(query, args))
        }
        if (updates.size > 0) updates.forEach { database.execSQL(it.first, it.second.toTypedArray()) }
    }
}

val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE features_table ADD feature_description TEXT NOT NULL DEFAULT ''")
    }
}

val allMigrations = arrayOf(
    MIGRATION_29_30,
    MIGRATION_30_31,
    MIGRATION_31_32,
    MIGRATION_32_33,
    MIGRATION_33_34,
    MIGRATION_34_35,
    MIGRATION_35_36
)
