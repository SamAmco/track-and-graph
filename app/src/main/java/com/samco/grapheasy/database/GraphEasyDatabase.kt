package com.samco.grapheasy.database

import android.content.Context
import androidx.room.*

@Database(
    entities = [TrackGroup::class, Feature::class, FeatureTrackGroupJoin::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
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

class Converters {
    @TypeConverter
    fun stringToListLong(value: String): List<Long> = value.split(",").map { f -> f.toLong() }.toList()

    @TypeConverter
    fun listLongToString(longs: List<Long>): String = longs.joinToString(",") { l -> l.toString() }

    @TypeConverter
    fun intToFeatureType(i: Int): FeatureType = FeatureType.values()[i]

    @TypeConverter
    fun featureTypeToInt(featureType: FeatureType): Int = featureType.index
}