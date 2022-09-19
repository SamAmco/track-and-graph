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

package com.samco.trackandgraph.base.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_47_48 = object : Migration(47, 48) {
    override fun migrate(database: SupportSQLiteDatabase) {
        createNewFeaturesTable(database)
        createNewTrackersTable(database)
        createNewDataPointsTable(database)
        copyOldFeaturesToNewFeaturesTable(database)
        copyOldFeaturesToNewTrackersTable(database)
        copyOldDataPointsToNewDataPointsTable(database)
        removeOldFeaturesTable(database)
        removeOldDataPointsTable(database)
        createNewFeaturesIndex(database)
        createNewDataPointsIndex(database)
        createFunctionsTable(database)
    }

    private fun createNewDataPointsIndex(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_data_points_table_feature_id` ON `data_points_table` (`feature_id`)")
    }

    private fun createNewFeaturesIndex(database: SupportSQLiteDatabase) {
        val tableName = "features_table"
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_features_table_id` ON `${tableName}` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_features_table_group_id` ON `${tableName}` (`group_id`)")
    }

    private fun removeOldDataPointsTable(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS `data_points_table_old`")
    }

    private fun copyOldDataPointsToNewDataPointsTable(database: SupportSQLiteDatabase) {
        database.execSQL("INSERT INTO data_points_table SELECT * FROM data_points_table_old")
    }

    private fun createNewDataPointsTable(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE data_points_table RENAME TO data_points_table_old")

        val tableName = "data_points_table"
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `${tableName}` (
                    `timestamp` TEXT NOT NULL,
                    `feature_id` INTEGER NOT NULL, 
                    `value` REAL NOT NULL,
                    `label` TEXT NOT NULL, 
                    `note` TEXT NOT NULL, 
                    PRIMARY KEY(`timestamp`, `feature_id`), 
                    FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimMargin()
        )
    }

    private fun createFunctionsTable(database: SupportSQLiteDatabase) {
        val tableName = "functions_table"
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `${tableName}` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `feature_id` INTEGER NOT NULL, 
                    `data_sources` TEXT NOT NULL, 
                    `script` TEXT NOT NULL, 
                    FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimMargin()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_functions_table_id` ON `${tableName}` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_functions_table_feature_id` ON `${tableName}` (`feature_id`)")
    }

    private fun removeOldFeaturesTable(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS `features_table_old`")
    }

    private fun copyOldFeaturesToNewTrackersTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
                INSERT INTO trackers_table(feature_id, type, discrete_values, has_default_value, default_value)
                SELECT id as feature_id, type, discrete_values, has_default_value, default_value
                FROM features_table_old
            """.trimIndent()
        )
    }

    private fun copyOldFeaturesToNewFeaturesTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
                INSERT INTO features_table
                SELECT id, name, group_id, display_index, feature_description
                FROM features_table_old
            """.trimIndent()
        )
    }

    private fun createNewTrackersTable(database: SupportSQLiteDatabase) {
        val tableName = "trackers_table"
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `${tableName}` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `feature_id` INTEGER NOT NULL, 
                    `type` INTEGER NOT NULL, 
                    `discrete_values` TEXT NOT NULL, 
                    `has_default_value` INTEGER NOT NULL, 
                    `default_value` REAL NOT NULL, 
                    FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimMargin()
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_trackers_table_id` ON `${tableName}` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_trackers_table_feature_id` ON `${tableName}` (`feature_id`)")
    }

    private fun createNewFeaturesTable(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE features_table RENAME TO features_table_old")
        val tableName = "features_table"
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `${tableName}` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `group_id` INTEGER NOT NULL, 
                    `display_index` INTEGER NOT NULL, 
                    `feature_description` TEXT NOT NULL, 
                    FOREIGN KEY(`group_id`) REFERENCES `groups_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimMargin()
        )
    }
}
