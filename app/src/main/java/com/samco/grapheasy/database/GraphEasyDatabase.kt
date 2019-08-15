package com.samco.grapheasy.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrackGroup::class], version = 1, exportSchema = false)
abstract class GraphEasyDatabase : RoomDatabase() {
    abstract val graphEasyDatabaseDao: GraphEasyDatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: GraphEasyDatabase? = null
        fun getInstance(context: Context): GraphEasyDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(context.applicationContext, GraphEasyDatabase::class.java, "graph_easy_database")
                        .fallbackToDestructiveMigration()//TODO consider migration
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}