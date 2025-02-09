package com.samco.trackandgraph.base.database.sampling

import com.samco.trackandgraph.base.database.dto.DataPoint

/**
 * A sequence of data points in order from newest to oldest. When you are done iterating the
 * sample you must call [dispose] to release any resources being used.
 */
abstract class RawDataSample(
    val dataSampleProperties: DataSampleProperties = DataSampleProperties()
) : Sequence<DataPoint> {
    companion object {
        /**
         * Return a DataSample from a sequence with the given properties and the given function
         * for returning the raw data used.
         */
        fun fromSequence(
            data: Sequence<DataPoint>,
            dataSampleProperties: DataSampleProperties = DataSampleProperties(),
            getRawDataPoints: () -> List<DataPoint>,
            onDispose: () -> Unit
        ): RawDataSample {
            return object : RawDataSample(dataSampleProperties) {
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
}
