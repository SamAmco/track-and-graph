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

val MIGRATION_56_57 = object : Migration(56, 57) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // Create functions_table
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `functions_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `feature_id` INTEGER NOT NULL, 
                `function_graph` TEXT NOT NULL, 
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimIndent()
        )

        // Create function_input_features_table
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `function_input_features_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `function_id` INTEGER NOT NULL, 
                `feature_id` INTEGER NOT NULL, 
                FOREIGN KEY(`function_id`) REFERENCES `functions_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimIndent()
        )

        // Create indexes for functions_table
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_functions_table_id` ON functions_table (`id`)
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_functions_table_feature_id` ON functions_table (`feature_id`)
            """.trimIndent()
        )

        // Create indexes for function_input_features_table
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_function_input_features_table_id` ON function_input_features_table (`id`)
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_function_input_features_table_function_id` ON function_input_features_table (`function_id`)
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_function_input_features_table_feature_id` ON function_input_features_table (`feature_id`)
            """.trimIndent()
        )
    }
}
