package com.samco.grapheasy.database

import android.content.Context
import androidx.room.*
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

val databaseFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
val displayFeatureDateFormat: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/YY  HH:mm")
    .withZone(ZoneId.systemDefault())

@Database(
    entities = [TrackGroup::class, Feature::class, DataPoint::class, DataSamplerSpec::class],
    version = 13,
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
    fun stringToListOfDiscreteValues(value: String): List<DiscreteValue> {
        return if (value.isEmpty()) listOf()
        else value.split(",").map { s -> DiscreteValue.fromString(s) }.toList()
    }

    @TypeConverter
    fun listOfDiscreteValuesToString(values: List<DiscreteValue>): String = values.joinToString(",") { v -> v.toString() }

    @TypeConverter
    fun intToFeatureType(i: Int): FeatureType = FeatureType.values()[i]

    @TypeConverter
    fun featureTypeToInt(featureType: FeatureType): Int = featureType.index

    @TypeConverter
    fun stringToOffsetDateTime(value: String?): OffsetDateTime? = value?.let { odtFromString(value) }

    @TypeConverter
    fun offsetDateTimeToString(value: OffsetDateTime?): String = value?.let { stringFromOdt(it) } ?: ""
}

fun odtFromString(value: String): OffsetDateTime = databaseFormatter.parse(value, OffsetDateTime::from)
fun stringFromOdt(value: OffsetDateTime): String = databaseFormatter.format(value)
