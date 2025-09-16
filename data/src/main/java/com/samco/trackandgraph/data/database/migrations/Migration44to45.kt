/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.data.database.migrations


import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


val MIGRATION_44_45 = object : Migration(44, 45) {
    private val helper = MigrationJsonHelper.getMigrationJsonHelper()

    override fun migrate(database: SupportSQLiteDatabase) {
        createTimeSinceLastTable(database)
        createAverageTimeBetweenTable(database)

        copyTimeSinceLastData(database)
        copyAverageTimeBetweenData(database)
        database.execSQL("DROP TABLE IF EXISTS `time_since_last_stat_table2`")
        database.execSQL("DROP TABLE IF EXISTS `average_time_between_stat_table2`")
    }

    private fun getDiscreteValues(value: String): List<MigrationJsonHelper.DiscreteValue> {
        return helper.stringToListOfDiscreteValues(value)
    }

    private fun encodeListOfLabels(labels: List<String>): String {
        return helper.toJson(labels)
    }

    private fun copyTimeSinceLastData(database: SupportSQLiteDatabase) {
        val inserts = mutableListOf<NTuple6<Long, Long, Long, Double, Double, String>>()
        val graphsCursor = database.query("SELECT * FROM time_since_last_stat_table2")
        while (graphsCursor.moveToNext()) {
            try {
                val id = graphsCursor.getLong(0)
                val graphStatId = graphsCursor.getLong(1)
                val featureId = graphsCursor.getLong(2)
                val fromValue = graphsCursor.getString(3).toDouble()
                val toValue = graphsCursor.getString(4).toDouble()
                val discreteIds = graphsCursor.getString(5)

                val discreteIdInts = discreteIds.split("||").mapNotNull { it.toIntOrNull() }
                val featureCursor =
                    database.query("SELECT discrete_values FROM features_table WHERE id = $featureId")
                val discreteValuesString = try {
                    if (featureCursor.moveToNext()) featureCursor.getString(0) else ""
                } catch (t: Throwable) {
                    ""
                }
                val discreteValues = getDiscreteValues(discreteValuesString)

                val labels = discreteValues.filter { it.index in discreteIdInts }.map { it.label }
                val labelsString = encodeListOfLabels(labels)
                inserts.add(NTuple6(id, graphStatId, featureId, fromValue, toValue, labelsString))
            } catch (throwable: Throwable) {
                continue
            }
        }
        val query =
            """
                INSERT INTO time_since_last_stat_table3(
                    id, graph_stat_id, feature_id, from_value, to_value, labels
                ) VALUES (?,?,?,?,?,?)
            """.trimIndent()
        if (inserts.size > 0) inserts.forEach { args ->
            database.execSQL(query, args.toList().map { it.toString() }.toTypedArray())
        }
    }

    private fun copyAverageTimeBetweenData(database: SupportSQLiteDatabase) {
        val inserts =
            mutableListOf<NTuple8<Long, Long, Long, Double, Double, String, String, String>>()
        val graphsCursor = database.query("SELECT * FROM average_time_between_stat_table2")
        while (graphsCursor.moveToNext()) {
            try {
                val id = graphsCursor.getLong(0)
                val graphStatId = graphsCursor.getLong(1)
                val featureId = graphsCursor.getLong(2)
                val fromValue = graphsCursor.getString(3).toDouble()
                val toValue = graphsCursor.getString(4).toDouble()
                val duration = graphsCursor.getString(5)
                val discreteIds = graphsCursor.getString(6)
                val endDate = graphsCursor.getString(7)

                val discreteIdInts = discreteIds.split("||").mapNotNull { it.toIntOrNull() }

                val featureCursor =
                    database.query("SELECT discrete_values FROM features_table WHERE id = $featureId")
                val discreteValuesString = try {
                    if (featureCursor.moveToNext()) featureCursor.getString(0) else ""
                } catch (t: Throwable) {
                    ""
                }
                val discreteValues = getDiscreteValues(discreteValuesString)
                val labels = discreteValues.filter { it.index in discreteIdInts }.map { it.label }
                val labelsString = encodeListOfLabels(labels)

                inserts.add(
                    NTuple8(
                        id,
                        graphStatId,
                        featureId,
                        fromValue,
                        toValue,
                        duration,
                        labelsString,
                        endDate
                    )
                )
            } catch (throwable: Throwable) {
                continue
            }
        }
        val query =
            """
                INSERT INTO average_time_between_stat_table3(
                    id, graph_stat_id, feature_id, from_value, to_value, duration, labels, end_date
                ) VALUES (?,?,?,?,?,?,?,?)
            """.trimIndent()
        if (inserts.size > 0) inserts.forEach { args ->
            database.execSQL(query, args.toList().map { it.toString() }.toTypedArray())
        }
    }

    private fun createTimeSinceLastTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `time_since_last_stat_table3` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `graph_stat_id` INTEGER NOT NULL, 
                    `feature_id` INTEGER NOT NULL, 
                    `from_value` REAL NOT NULL, 
                    `to_value` REAL NOT NULL, 
                    `labels` TEXT NOT NULL, 
                    FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                    FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimMargin()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_time_since_last_stat_table3_id` ON `time_since_last_stat_table3` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_time_since_last_stat_table3_graph_stat_id` ON `time_since_last_stat_table3` (`graph_stat_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_time_since_last_stat_table3_feature_id` ON `time_since_last_stat_table3` (`feature_id`)")
    }

    private fun createAverageTimeBetweenTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS average_time_between_stat_table3 (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `graph_stat_id` INTEGER NOT NULL, 
                    `feature_id` INTEGER NOT NULL, 
                    `from_value` REAL NOT NULL, 
                    `to_value` REAL NOT NULL, 
                    `duration` TEXT, 
                    `labels` TEXT NOT NULL, 
                    `end_date` TEXT, 
                    FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimMargin()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_average_time_between_stat_table3_id` ON average_time_between_stat_table3 (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_average_time_between_stat_table3_graph_stat_id` ON average_time_between_stat_table3 (`graph_stat_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_average_time_between_stat_table3_feature_id` ON average_time_between_stat_table3 (`feature_id`)")
    }
}
