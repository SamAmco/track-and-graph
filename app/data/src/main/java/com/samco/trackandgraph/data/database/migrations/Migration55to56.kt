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

val MIGRATION_55_56 = object : Migration(55, 56) {
    override fun migrate(db: SupportSQLiteDatabase) {

        //Drop the function table
        db.execSQL(
            """
            DROP TABLE IF EXISTS `functions_table`
            """.trimIndent()
        )

        // Create lua_graphs_table
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `lua_graphs_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_id` INTEGER NOT NULL, 
                `script` TEXT NOT NULL, 
                FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimIndent()
        )

        // Create lua_graph_features_table
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `lua_graph_features_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `lua_graph_id` INTEGER NOT NULL, 
                `feature_id` INTEGER NOT NULL, 
                `name` TEXT NOT NULL, 
                FOREIGN KEY(`lua_graph_id`) REFERENCES `lua_graphs_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimIndent()
        )

        // Create indexes for lua_graphs_table
        
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_lua_graphs_table_id` ON lua_graphs_table (`id`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_lua_graphs_table_graph_stat_id` ON lua_graphs_table (`graph_stat_id`)
            """.trimIndent()
        )

        
        // Create indexes for lua_graph_features_table
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_lua_graph_features_table_id` ON lua_graph_features_table (`id`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_lua_graph_features_table_lua_graph_id` ON lua_graph_features_table (`lua_graph_id`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_lua_graph_features_table_feature_id` ON lua_graph_features_table (`feature_id`)
            """.trimIndent()
        )
    }
}

