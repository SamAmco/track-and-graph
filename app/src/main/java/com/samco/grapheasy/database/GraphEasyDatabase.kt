package com.samco.grapheasy.database

import android.content.Context
import androidx.room.*
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

val databaseFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
val displayFormatter: DateTimeFormatter = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.SHORT)
    .withZone(ZoneId.systemDefault())

@Database(
    entities = [TrackGroup::class, Feature::class, FeatureTrackGroupJoin::class, DataPoint::class],
    version = 5,
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
    fun stringToListOfStrings(value: String): List<String> = value.split(",").toList()

    @TypeConverter
    fun listOfStringsToString(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun stringToListLong(value: String): List<Long> = value.split(",").map { f -> f.toLong() }.toList()

    @TypeConverter
    fun listLongToString(longs: List<Long>): String = longs.joinToString(",") { l -> l.toString() }

    @TypeConverter
    fun intToFeatureType(i: Int): FeatureType = FeatureType.values()[i]

    @TypeConverter
    fun featureTypeToInt(featureType: FeatureType): Int = featureType.index

    @TypeConverter
    fun stringToOffsetDateTime(value: String): OffsetDateTime = databaseFormatter.parse(value, OffsetDateTime::from)

    @TypeConverter
    fun offsetDateTimeToString(value: OffsetDateTime): String = databaseFormatter.format(value)
}
