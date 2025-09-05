package com.samco.trackandgraph.data.database.sampling

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.IDataPoint
import org.threeten.bp.OffsetDateTime

/**
 * A sequence of data points in order from newest to oldest. When you are done iterating the
 * sample you must call [dispose] to release any resources being used.
 */
abstract class RawDataSample : Sequence<DataPoint> {
    companion object {
        /**
         * Return a DataSample from a sequence with the given properties and the given function
         * for returning the raw data used.
         */
        fun fromSequence(
            data: Sequence<DataPoint>,
            getRawDataPoints: () -> List<DataPoint>,
            onDispose: () -> Unit
        ): RawDataSample {
            return object : RawDataSample() {
                override fun getRawDataPoints() = getRawDataPoints()
                override fun iterator(): Iterator<DataPoint> = data.iterator()
                override fun dispose() = onDispose()
            }
        }
    }

    /**
     * Clean up any resources held onto by this data sample. For example a SQLite Cursor.
     * Attempting to use a DataSample after its dispose function has been called is un-defined
     * behaviour.
     */
    abstract fun dispose()

    /**
     * Get a list of all the raw data points that have been used so far to generate this
     * data sample. This will not contain any data points that have not yet been iterated in the
     * sequence.
     */
    abstract fun getRawDataPoints(): List<DataPoint>

    fun asDataSample(dataSampleProperties: DataSampleProperties? = null): DataSample =
        DataSample.fromSequence(
            data = this.map {
                object : IDataPoint() {
                    override val timestamp: OffsetDateTime = it.timestamp
                    override val value: Double = it.value
                    override val label: String = it.label
                }
            },
            dataSampleProperties = dataSampleProperties ?: DataSampleProperties(),
            onDispose = this::dispose
        )
}
