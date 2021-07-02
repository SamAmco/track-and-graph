package com.samco.trackandgraph.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.samco.trackandgraph.database.entity.CheckedDays
import com.samco.trackandgraph.database.entity.DiscreteValue
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.Exception

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminders_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `display_index` INTEGER NOT NULL, 
                `name` TEXT NOT NULL,
                `time` TEXT NOT NULL,
                `checked_days` TEXT NOT NULL
            )
            """.trimMargin()
        )
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
            val oldFeatures = oldFeaturesString.split("||")
            val newFeatures = oldFeatures.map { f ->
                val params = f.split("!!").toMutableList()
                params.add(5, "0")
                params.joinToString("!!")
            }
            val newFeaturesString = newFeatures.joinToString("||")
            updates.add(
                Pair(
                    "UPDATE line_graphs_table SET features=? WHERE id=?",
                    listOf(newFeaturesString, id.toString())
                )
            )
        }
        if (updates.size > 0) updates.forEach {
            database.execSQL(
                it.first,
                it.second.toTypedArray()
            )
        }
    }
}

val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val lineGraphsCursor = database.query("SELECT * FROM line_graphs_table")
        val updates = mutableListOf<Pair<String, List<String>>>()
        while (lineGraphsCursor.moveToNext()) {
            val id = lineGraphsCursor.getLong(0)
            val oldFeaturesString = lineGraphsCursor.getString(2)
            val oldFeatures = oldFeaturesString.split("||")
            val newFeatures = oldFeatures.map { f ->
                val params = f.split("!!").toMutableList()
                var plottingMode = params[4].toInt()
                if (plottingMode > 0) plottingMode += 1
                params[4] = plottingMode.toString()
                params.joinToString("!!")
            }
            val newFeaturesString = newFeatures.joinToString("||")
            updates.add(
                Pair(
                    "UPDATE line_graphs_table SET features=? WHERE id=?",
                    listOf(newFeaturesString, id.toString())
                )
            )
        }
        if (updates.size > 0) updates.forEach {
            database.execSQL(
                it.first,
                it.second.toTypedArray()
            )
        }
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
            val discreteValues = from + "||" + to
            val query = "UPDATE average_time_between_stat_table SET discrete_values=? WHERE id=?"
            val args = listOf(discreteValues, id.toString())
            updates.add(Pair(query, args))
        }
        val timeSinceCursor = database.query("SELECT * FROM time_since_last_stat_table")
        while (timeSinceCursor.moveToNext()) {
            val id = timeSinceCursor.getLong(0)
            val from = timeSinceCursor.getString(3)
            val to = timeSinceCursor.getString(4)
            val discreteValues = from + "||" + to
            val query = "UPDATE time_since_last_stat_table SET discrete_values=? WHERE id=?"
            val args = listOf(discreteValues, id.toString())
            updates.add(Pair(query, args))
        }
        if (updates.size > 0) updates.forEach {
            database.execSQL(
                it.first,
                it.second.toTypedArray()
            )
        }
    }
}

val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE features_table ADD feature_description TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE data_points_table ADD note TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE graphs_and_stats_table ADD end_date TEXT")
    }
}

val MIGRATION_38_39 = object : Migration(38, 39) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notes_table` (
                `timestamp` TEXT PRIMARY KEY NOT NULL, 
                `note` TEXT NOT NULL
            )
            """.trimMargin()
        )
    }
}

val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `line_graphs_table2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                 `graph_stat_id` INTEGER NOT NULL, 
                `duration` TEXT, 
                `y_range_type` INTEGER NOT NULL, 
                `y_from` REAL NOT NULL, 
                `y_to` REAL NOT NULL, 
                FOREIGN KEY(`graph_stat_id`) 
                REFERENCES `graphs_and_stats_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimMargin()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graphs_table2_id` ON `line_graphs_table2` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graphs_table2_graph_stat_id` ON `line_graphs_table2` (`graph_stat_id`)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `line_graph_features_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `line_graph_id` INTEGER NOT NULL,
                `feature_id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `color_index` INTEGER NOT NULL,
                `averaging_mode` INTEGER NOT NULL,
                 `plotting_mode` INTEGER NOT NULL, 
                `point_style` INTEGER NOT NULL, 
                `offset` REAL NOT NULL, 
                `scale` REAL NOT NULL, 
                `duration_plotting_mode` INTEGER NOT NULL, 
                FOREIGN KEY(`line_graph_id`) REFERENCES `line_graphs_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimMargin()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graph_features_table_id` ON `line_graph_features_table` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graph_features_table_line_graph_id` ON `line_graph_features_table` (`line_graph_id`)")

        database.execSQL("INSERT INTO line_graphs_table2 SELECT id, graph_stat_id, duration, y_range_type, y_from, y_to FROM line_graphs_table")
        val lineGraphsCursor = database.query("SELECT * FROM line_graphs_table")
        val inserts = mutableListOf<Pair<String, List<String>>>()
        var index = 0L
        val lineGraphFeatureInsertStatement =
            """
                INSERT INTO line_graph_features_table(
                    id, line_graph_id, feature_id, name,
                    color_index, averaging_mode, plotting_mode, point_style,
                    offset, scale, duration_plotting_mode
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent()
        while (lineGraphsCursor.moveToNext()) {
            val id = lineGraphsCursor.getString(0)
            val features = lineGraphsCursor.getString(2).split("||")
            for (featureString in features) {
                val featureProperties = featureString.split("!!")
                if (featureProperties.size >= 8) {
                    val params = mutableListOf(index++.toString(), id)
                    params.addAll(featureProperties)
                    params.add(0.toString())
                    inserts.add(Pair(lineGraphFeatureInsertStatement, params))
                }
            }
        }
        if (inserts.size > 0) inserts.forEach {
            database.execSQL(
                it.first,
                it.second.toTypedArray()
            )
        }
        database.execSQL("DROP TABLE IF EXISTS `line_graphs_table`")
    }
}

val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(database: SupportSQLiteDatabase) {
        //UPDATE graphs_and_stats_table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `graphs_and_stats_table2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_group_id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `graph_stat_type` INTEGER NOT NULL,
                `display_index` INTEGER NOT NULL,
                FOREIGN KEY(`graph_stat_group_id`) REFERENCES `graph_stat_groups_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimMargin()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_graphs_and_stats_table2_id` ON `graphs_and_stats_table2` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_graphs_and_stats_table2_graph_stat_group_id` ON `graphs_and_stats_table2` (`graph_stat_group_id`)")
        database.execSQL("INSERT INTO graphs_and_stats_table2 SELECT id, graph_stat_group_id, name, graph_stat_type, display_index FROM graphs_and_stats_table")

        //UPDATE time_since_last_stat_table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `time_since_last_stat_table2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_id` INTEGER NOT NULL,
                `feature_id` INTEGER NOT NULL, 
                `from_value` TEXT NOT NULL, 
                `to_value` TEXT NOT NULL, 
                `discrete_values` TEXT NOT NULL, 
                FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_time_since_last_stat_table2_id` ON `time_since_last_stat_table2` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_time_since_last_stat_table2_graph_stat_id` ON `time_since_last_stat_table2` (`graph_stat_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_time_since_last_stat_table2_feature_id` ON `time_since_last_stat_table2` (`feature_id`)")
        database.execSQL("INSERT INTO time_since_last_stat_table2 SELECT id, graph_stat_id, feature_id, from_value, to_value, discrete_values FROM time_since_last_stat_table")

        //UPDATE pie_charts_table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pie_charts_table2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_id` INTEGER NOT NULL,
                `feature_id` INTEGER NOT NULL, 
                `duration` TEXT, 
                `end_date` TEXT, 
                FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_pie_charts_table2_id` ON `pie_charts_table2` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_pie_charts_table2_graph_stat_id` ON `pie_charts_table2` (`graph_stat_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_pie_charts_table2_feature_id` ON `pie_charts_table2` (`feature_id`)")
        database.execSQL("INSERT INTO pie_charts_table2 SELECT id, graph_stat_id, feature_id, duration, NULL FROM pie_chart_table")
        database.execSQL(
            """UPDATE pie_charts_table2 SET end_date= (
                SELECT end_date FROM graphs_and_stats_table 
                WHERE id=pie_charts_table2.graph_stat_id
            )""".trimMargin()
        )

        //UPDATE line_graphs_table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `line_graphs_table3` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_id` INTEGER NOT NULL,
                `duration` TEXT,
                `y_range_type` INTEGER NOT NULL,
                `y_from` REAL NOT NULL,
                `y_to` REAL NOT NULL,
                `end_date` TEXT,
                FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graphs_table3_id` ON `line_graphs_table3` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graphs_table3_graph_stat_id` ON `line_graphs_table3` (`graph_stat_id`)")
        database.execSQL("INSERT INTO line_graphs_table3 SELECT id, graph_stat_id, duration, y_range_type, y_from, y_to, NULL FROM line_graphs_table2")
        database.execSQL(
            """UPDATE line_graphs_table3 SET end_date= (
                SELECT end_date FROM graphs_and_stats_table 
                WHERE id=line_graphs_table3.graph_stat_id
            )""".trimMargin()
        )

        //UPDATE average_time_between_stat_table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `average_time_between_stat_table2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_id` INTEGER NOT NULL,
                `feature_id` INTEGER NOT NULL, 
                `from_value` TEXT NOT NULL,
                `to_value` TEXT NOT NULL,
                `duration` TEXT,
                `discrete_values` TEXT NOT NULL,
                `end_date` TEXT,
                FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
        )""".trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_average_time_between_stat_table2_id` ON `average_time_between_stat_table2` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_average_time_between_stat_table2_graph_stat_id` ON `average_time_between_stat_table2` (`graph_stat_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_average_time_between_stat_table2_feature_id` ON `average_time_between_stat_table2` (`feature_id`)")
        database.execSQL("INSERT INTO average_time_between_stat_table2 SELECT id, graph_stat_id, feature_id, from_value, to_value, duration, discrete_values, NULL FROM average_time_between_stat_table")
        database.execSQL(
            """UPDATE average_time_between_stat_table2 SET end_date= (
                SELECT end_date FROM graphs_and_stats_table 
                WHERE id=average_time_between_stat_table2.graph_stat_id
            )""".trimMargin()
        )

        //UPDATE line_graph_features_table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `line_graph_features_table2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `line_graph_id` INTEGER NOT NULL, 
                `feature_id` INTEGER NOT NULL, 
                `name` TEXT NOT NULL, 
                `color_index` INTEGER NOT NULL, 
                `averaging_mode` INTEGER NOT NULL, 
                `plotting_mode` INTEGER NOT NULL, 
                `point_style` INTEGER NOT NULL, 
                `offset` REAL NOT NULL, 
                `scale` REAL NOT NULL, 
                `duration_plotting_mode` INTEGER NOT NULL, 
                FOREIGN KEY(`line_graph_id`) REFERENCES `line_graphs_table3`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graph_features_table2_id` ON `line_graph_features_table2` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graph_features_table2_line_graph_id` ON `line_graph_features_table2` (`line_graph_id`)")
        database.execSQL("INSERT INTO line_graph_features_table2 SELECT id, line_graph_id, feature_id, name, color_index, averaging_mode, plotting_mode, point_style, offset, scale, duration_plotting_mode FROM line_graph_features_table")


        //DROP UNUSED TABLES
        database.execSQL("DROP TABLE IF EXISTS `graphs_and_stats_table`")
        database.execSQL("DROP TABLE IF EXISTS `time_since_last_stat_table`")
        database.execSQL("DROP TABLE IF EXISTS `pie_chart_table`")
        database.execSQL("DROP TABLE IF EXISTS `line_graphs_table2`")
        database.execSQL("DROP TABLE IF EXISTS `average_time_between_stat_table`")
        database.execSQL("DROP TABLE IF EXISTS `line_graph_features_table`")
    }
}

val MIGRATION_41_42 = object : Migration(41, 42) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `time_histograms_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_id` INTEGER NOT NULL,
                `feature_id` INTEGER NOT NULL, 
                `duration` TEXT, 
                `window` INTEGER NOT NULL, 
                `sum_by_count` INTEGER NOT NULL, 
                `end_date` TEXT, 
                FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimMargin()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_time_histograms_table_id` ON `time_histograms_table` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_time_histograms_table_graph_stat_id` ON `time_histograms_table` (`graph_stat_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_time_histograms_table_feature_id` ON `time_histograms_table` (`feature_id`)")
    }
}

val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val moshi = Moshi.Builder().build()
        updateDiscreteValues(database, moshi)
        updateCheckedDays(database, moshi)
    }

    private fun updateCheckedDays(database: SupportSQLiteDatabase, moshi: Moshi) {
        val remindersCursor = database.query("SELECT * FROM reminders_table")
        val updates = mutableListOf<List<String>>()
        while (remindersCursor.moveToNext()) {
            val id = remindersCursor.getString(0)
            val checkedDays = CheckedDays.fromList(
                remindersCursor.getString(4)
                    .split("||")
                    .map { it.toBoolean() }
            )

            val jsonString = moshi.adapter(CheckedDays::class.java).toJson(checkedDays) ?: ""
            updates.add(listOf(jsonString, id))
        }
        if (updates.size > 0) updates.forEach {
            val featureUpdateStatement =
                """UPDATE reminders_table 
                    SET checked_days = ?
                    WHERE id = ?
            """.trimIndent()
            database.execSQL(featureUpdateStatement, it.toTypedArray())
        }
    }

    private fun updateDiscreteValues(database: SupportSQLiteDatabase, moshi: Moshi) {
        val featureCursor = database.query("SELECT * FROM features_table WHERE type = 0")
        val updates = mutableListOf<List<String>>()
        val deletes = mutableListOf<String>()
        while (featureCursor.moveToNext()) {
            val id = featureCursor.getString(0)
            val discreteValuesString = featureCursor.getString(4)
            val discreteValues = try {
                discreteValuesString
                    .split("||")
                    .map { DiscreteValue.fromString(it) }
            } catch (e: Exception) {
                deletes.add(id)
                continue
            }
            val listType = Types.newParameterizedType(List::class.java, DiscreteValue::class.java)
            val jsonString =
                moshi.adapter<List<DiscreteValue>>(listType).toJson(discreteValues) ?: ""
            updates.add(listOf(jsonString, id))
        }
        if (deletes.size > 0) deletes.forEach {
            database.execSQL("""DELETE FROM features_table WHERE id = ?""", arrayOf(it))
        }
        if (updates.size > 0) updates.forEach {
            val featureUpdateStatement =
                """UPDATE features_table 
                    SET discrete_values = ?
                    WHERE id = ?
            """.trimIndent()
            database.execSQL(featureUpdateStatement, it.toTypedArray())
        }
    }
}

val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(database: SupportSQLiteDatabase) {
        TODO("Not yet implemented")
    }
}

val allMigrations = arrayOf(
    MIGRATION_29_30,
    MIGRATION_30_31,
    MIGRATION_31_32,
    MIGRATION_32_33,
    MIGRATION_33_34,
    MIGRATION_34_35,
    MIGRATION_35_36,
    MIGRATION_36_37,
    MIGRATION_37_38,
    MIGRATION_38_39,
    MIGRATION_39_40,
    MIGRATION_40_41,
    MIGRATION_41_42,
    MIGRATION_42_43,
    MIGRATION_43_44
)
