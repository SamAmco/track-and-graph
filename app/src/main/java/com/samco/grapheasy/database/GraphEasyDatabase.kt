package com.samco.grapheasy.database

import android.content.Context
import androidx.room.*
import com.samco.grapheasy.R
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

val databaseFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
val displayFeatureDateFormat: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yy  HH:mm")
    .withZone(ZoneId.systemDefault())


//this is a number coprime to the number of colours used to select them in a pseudo random order for greater contrast
val dataVisColorGenerator = 7
val dataVisColorList = listOf(
    R.color.visColor1,
    R.color.visColor2,
    R.color.visColor3,
    R.color.visColor4,
    R.color.visColor5,
    R.color.visColor6,
    R.color.visColor7,
    R.color.visColor8,
    R.color.visColor9,
    R.color.visColor10
)


@Database(
    entities = [TrackGroup::class, Feature::class, DataPoint::class, GraphOrStat::class, LineGraph::class,
        AverageTimeBetweenStat::class, PieChart::class, TimeSinceLastStat::class],
    version = 19,
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
    fun featureTypeToInt(featureType: FeatureType): Int = featureType.ordinal

    @TypeConverter
    fun intToGraphStatType(i: Int): GraphStatType = GraphStatType.values()[i]

    @TypeConverter
    fun graphStatTypeToInt(graphStat: GraphStatType): Int = graphStat.ordinal

    @TypeConverter
    fun stringToOffsetDateTime(value: String?): OffsetDateTime? = value?.let { odtFromString(value) }

    @TypeConverter
    fun offsetDateTimeToString(value: OffsetDateTime?): String = value?.let { stringFromOdt(it) } ?: ""

    @TypeConverter
    fun lineGraphFeaturesToString(value: List<LineGraphFeature>): String {
        return value.joinToString(","){ v -> "${v.featureId};${v.name};${v.colorId};${v.averagingMode.ordinal};${v.plottingMode.ordinal}${v.offset};${v.scale}" }
    }

    @TypeConverter
    fun stringToLineGraphFeatures(value: String): List<LineGraphFeature> {
        return value.split(",").map {
            val strs = it.split(';')
            LineGraphFeature(
                strs[0].toLong(),
                strs[1],
                strs[2].toInt(),
                LineGraphAveraginModes.values()[strs[3].toInt()],
                LineGraphPlottingModes.values()[strs[4].toInt()],
                strs[4].toDouble(),
                strs[5].toDouble()
            )
        }
    }

    @TypeConverter
    fun durationToString(value: Duration?): String = value?.let { value.toString() } ?: ""

    @TypeConverter
    fun stringToDuration(value: String): Duration? = if (value.isEmpty()) null else Duration.parse(value)
}


fun odtFromString(value: String): OffsetDateTime = databaseFormatter.parse(value, OffsetDateTime::from)
fun stringFromOdt(value: OffsetDateTime): String = databaseFormatter.format(value)

