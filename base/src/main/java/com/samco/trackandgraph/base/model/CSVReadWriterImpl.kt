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

package com.samco.trackandgraph.base.model

import android.util.Log
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.DiscreteValue
import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.database.odtFromString
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.base.model.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVRecord
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject
import com.samco.trackandgraph.base.database.entity.DataPoint as DataPointEntity

//TODO right now the import feature of this implementation assumes that trackers may only have one
// data type and that that data type is either continuous, discrete or duration.
// In future I would like to blur the lines between continuous and discrete such that a tracker
// will require a numeric value (to be interpreted as either a number or a duration) but may optionally
// have any label or no label. The concept of a discrete value will be removed and this class will need
// to be updated accordingly.
internal class CSVReadWriterImpl @Inject constructor(
    private val dao: TrackAndGraphDatabaseDao,
    private val trackerHelper: TrackerHelper,
    @IODispatcher private val io: CoroutineDispatcher
) : CSVReadWriter {
    private enum class RequiredHeaders {
        FeatureName,
        Timestamp,
        Value
    }

    private enum class Headers {
        FeatureName,
        Timestamp,
        Value,
        Note
    }

    override suspend fun writeFeaturesToCSV(
        outStream: OutputStream,
        features: Map<Feature, DataSample>
    ) {
        withContext(io) {
            outStream.writer().use { writer ->
                writeFeaturesToCSVImpl(features, writer).getOrThrow()
            }
        }
    }

    private suspend fun writeFeaturesToCSVImpl(
        features: Map<Feature, DataSample>,
        writer: OutputStreamWriter
    ) = runCatching {
        val csvWriter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(Headers::class.java))
        for (kvp in features) {
            val feature = kvp.key
            val dataPoints = kvp.value.toList()
            val notes = kvp.value.getRawDataPoints().associate { it.timestamp to it.note }

            writeDataPointsToCSV(
                csvWriter,
                dataPoints,
                notes,
                feature.name,
                kvp.value.dataSampleProperties.isDuration
            )
        }
        writer.flush()
    }

    private suspend fun writeDataPointsToCSV(
        csvPrinter: CSVPrinter,
        dataPoints: List<IDataPoint>,
        notes: Map<OffsetDateTime, String>,
        featureName: String,
        isDuration: Boolean
    ) = dataPoints.forEach { dp ->
        val dataPointString = if (isDuration) {
            dp.value.toLong().let { seconds ->
                val absSecs = kotlin.math.abs(seconds)
                String.format("%d:%02d:%02d", seconds / 3600, (absSecs % 3600) / 60, (absSecs % 60))
            }
        } else {
            if (dp.label.isNotBlank()) "${dp.value}:${dp.label}"
            else dp.value.toString()
        }
        csvPrinter.printRecord(
            featureName,
            dp.timestamp.toString(),
            dataPointString,
            notes[dp.timestamp] ?: ""
        )
        yield()
    }

    override suspend fun readFeaturesFromCSV(inputStream: InputStream, trackGroupId: Long) {
        inputStream.reader().use {
            readFeaturesFromCSVImpl(trackGroupId, it).getOrThrow()
        }
    }

    private suspend fun readFeaturesFromCSVImpl(
        trackGroupId: Long,
        inputStreamReader: InputStreamReader
    ) = runCatching {
        try {
            val records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(inputStreamReader)
            val headerMap = records.headerMap
            validateHeaderMap(headerMap)
            ingestRecords(records, trackGroupId)
        } catch (e: Exception) {
            Log.e("CSVReadWriterImpl", "CSV reader through exception", e)
            if (e is ImportFeaturesException) throw e
            else throw ImportFeaturesException.Unknown()
        }
    }

    private suspend fun ingestRecords(
        records: Iterable<CSVRecord>,
        trackGroupId: Long
    ) {
        val existingTrackers = trackerHelper.getTrackersForGroupSync(trackGroupId).toMutableList()
        val existingTrackersByName = existingTrackers.associateBy { it.name }.toMutableMap()
        val newDataPoints = mutableListOf<DataPointEntity>()

        records.forEachIndexed { recordNumber, record ->
            val lineNumber = recordNumber + 2 //add one for index by zero and one for headers
            val rec = getValidRecordData(record, lineNumber)
            if (existingTrackersByName.containsKey(rec.trackerName)) {
                addDataPointToExistingFeature(
                    existingTrackers,
                    existingTrackersByName,
                    newDataPoints,
                    rec,
                    lineNumber
                )
            } else {
                addDataPointToNewFeature(
                    existingTrackers,
                    existingTrackersByName,
                    newDataPoints,
                    trackGroupId,
                    rec,
                    lineNumber
                )
            }
            if (lineNumber % 1000 == 0) {
                dao.insertDataPoints(newDataPoints)
                newDataPoints.clear()
            }
            yield()
        }
        if (newDataPoints.isNotEmpty()) dao.insertDataPoints(newDataPoints)
    }

    private suspend fun addDataPointToNewFeature(
        existingTrackers: MutableList<Tracker>,
        existingTrackersByName: MutableMap<String, Tracker>,
        points: MutableList<DataPointEntity>,
        trackGroupId: Long,
        rec: RecordData,
        lineNumber: Int
    ) {
        val newTracker = createNewTracker(rec, trackGroupId, lineNumber)
        insertTracker(existingTrackers, existingTrackersByName, newTracker)
        when (newTracker.dataType) {
            DataType.DISCRETE -> addDiscreteDataPoint(
                points,
                rec.value,
                rec.timestamp,
                newTracker,
                rec.note,
                lineNumber
            )
            DataType.CONTINUOUS -> addContinuousDataPoint(
                points,
                rec.value.toDoubleOrNull(),
                rec.timestamp,
                newTracker.featureId,
                rec.note
            )
            DataType.DURATION -> addDurationDataPoint(
                points,
                rec.value,
                rec.timestamp,
                newTracker.featureId,
                rec.note
            )
        }
    }

    private suspend fun addDataPointToExistingFeature(
        existingTrackers: MutableList<Tracker>,
        existingTrackersByName: MutableMap<String, Tracker>,
        points: MutableList<DataPointEntity>,
        rec: RecordData,
        lineNumber: Int
    ) {
        val tracker = existingTrackersByName[rec.trackerName]!!
        when (getDataType(rec.value)) {
            DataType.DISCRETE -> {
                validateDataType(tracker, DataType.DISCRETE, lineNumber)
                val discreteValue =
                    addDiscreteDataPoint(
                        points,
                        rec.value,
                        rec.timestamp,
                        tracker,
                        rec.note,
                        lineNumber
                    )
                if (!tracker.discreteValues.contains(discreteValue)) {
                    addDiscreteValueToFeature(
                        existingTrackers,
                        existingTrackersByName,
                        tracker,
                        discreteValue,
                        lineNumber
                    )
                }
            }
            DataType.CONTINUOUS -> {
                validateDataType(tracker, DataType.CONTINUOUS, lineNumber)
                addContinuousDataPoint(
                    points,
                    rec.value.toDoubleOrNull(),
                    rec.timestamp,
                    tracker.featureId,
                    rec.note
                )
            }
            DataType.DURATION -> {
                validateDataType(tracker, DataType.DURATION, lineNumber)
                addDurationDataPoint(
                    points,
                    rec.value,
                    rec.timestamp,
                    tracker.featureId,
                    rec.note
                )
            }
        }
    }

    private fun validateDataType(tracker: Tracker, expectedType: DataType, lineNumber: Int) {
        if (tracker.dataType != expectedType)
            throw ImportFeaturesException.InconsistentDataType(lineNumber)
    }


    private fun insertTracker(
        existingTrackers: MutableList<Tracker>,
        existingTrackersByName: MutableMap<String, Tracker>,
        tracker: Tracker
    ) {
        existingTrackers.add(tracker)
        existingTrackersByName[tracker.name] = tracker
    }

    private fun validateNotDiscreteValueConflict(
        discreteValue: DiscreteValue,
        tracker: Tracker,
        lineNumber: Int
    ) {
        if (tracker.discreteValues.any { dv -> dv.index == discreteValue.index }) {
            throw ImportFeaturesException.DiscreteValueConflict(lineNumber)
        }
    }

    private suspend fun addDiscreteValueToFeature(
        existingTrackers: MutableList<Tracker>,
        existingTrackersByName: MutableMap<String, Tracker>,
        tracker: Tracker,
        discreteValue: DiscreteValue,
        lineNumber: Int
    ) {
        validateNotDiscreteValueConflict(discreteValue, tracker, lineNumber)
        existingTrackers.remove(tracker)
        existingTrackersByName.remove(tracker.name)
        val newTracker =
            tryAddDiscreteValueToTracker(tracker, discreteValue, lineNumber)
        existingTrackers.add(newTracker)
        existingTrackersByName[newTracker.name] = newTracker
    }

    private fun addDiscreteDataPoint(
        points: MutableList<DataPointEntity>,
        value: String,
        timestamp: OffsetDateTime,
        tracker: Tracker,
        note: String,
        lineNumber: Int
    ): DiscreteValue {
        val discreteValue = tryGetDiscreteValueFromString(value, lineNumber)
        points.add(
            DataPointEntity(
                timestamp,
                tracker.featureId,
                discreteValue.index.toDouble(),
                discreteValue.label,
                note
            )
        )
        return discreteValue
    }

    private fun addContinuousDataPoint(
        points: MutableList<DataPointEntity>,
        value: Double?,
        timestamp: OffsetDateTime,
        featureId: Long,
        note: String
    ) {
        points.add(
            DataPointEntity(
                timestamp,
                featureId,
                value ?: 1.0,
                "",
                note
            )
        )
    }

    private fun addDurationDataPoint(
        points: MutableList<DataPointEntity>,
        value: String,
        timestamp: OffsetDateTime,
        featureId: Long,
        note: String
    ) {
        val segments = value.split(":")
        val hours = segments.getOrNull(0)?.toLong() ?: 0L
        val minutes = segments.getOrNull(1)?.toLong() ?: 0L
        val seconds = segments.getOrNull(2)?.toLong() ?: 0L
        val totalSeconds = Duration.ZERO
            .plusHours(hours)
            .plusMinutes(minutes)
            .plusSeconds(seconds)
            .seconds
            .toDouble()
        points.add(
            DataPointEntity(
                timestamp,
                featureId,
                totalSeconds,
                "",
                note
            )
        )
    }

    private suspend fun createNewTracker(
        rec: RecordData,
        trackGroupId: Long,
        lineNumber: Int
    ): Tracker {
        val dataType = getDataType(rec.value)
        val discreteValues = if (dataType == DataType.DISCRETE) listOf(
            tryGetDiscreteValueFromString(
                rec.value,
                lineNumber
            )
        ) else listOf()
        val newTracker = Tracker(
            id = 0,
            name = rec.trackerName,
            groupId = trackGroupId,
            featureId = 0L,
            displayIndex = 0,
            description = "",
            dataType = dataType,
            discreteValues = discreteValues,
            hasDefaultValue = false,
            1.0
        )
        val trackerId = trackerHelper.insertTracker(newTracker)
        return trackerHelper.getTrackerById(trackerId)!!
    }

    private fun tryGetDiscreteValueFromString(string: String, lineNumber: Int): DiscreteValue {
        try {
            return DiscreteValue.fromString(string)
        } catch (e: Exception) {
            throw ImportFeaturesException.BadDiscreteValue(lineNumber)
        }
    }

    private suspend fun tryAddDiscreteValueToTracker(
        tracker: Tracker,
        discreteValue: DiscreteValue,
        lineNumber: Int
    ): Tracker {
        val newDiscreteValues = tracker.discreteValues.toMutableList()
        if (newDiscreteValues.map { dv -> dv.index }.contains(discreteValue.index))
            throw ImportFeaturesException.DiscreteValueIndexExists(lineNumber, discreteValue.index)
        if (newDiscreteValues.map { dv -> dv.label }.contains(discreteValue.label))
            throw ImportFeaturesException.DiscreteValueLabelExists(lineNumber, discreteValue.label)
        newDiscreteValues.add(discreteValue)
        trackerHelper.updateTracker(tracker.copy(discreteValues = newDiscreteValues))
        return tracker.copy(discreteValues = newDiscreteValues)
    }

    private fun getDataType(value: String): DataType {
        val durationRegex = Regex("\\d*:\\d{2}:\\d{2}")
        return when {
            value.matches(durationRegex) -> DataType.DURATION
            value.contains(":") -> DataType.DISCRETE
            else -> DataType.CONTINUOUS
        }
    }

    private data class RecordData(
        val trackerName: String,
        val value: String,
        val timestamp: OffsetDateTime,
        val note: String
    )

    private fun getValidRecordData(record: CSVRecord, lineNumber: Int): RecordData {
        val featureName = record.get(Headers.FeatureName)
            ?: throw ImportFeaturesException.InconsistentRecord(lineNumber)
        val timestamp = record.get(Headers.Timestamp)
            ?: throw ImportFeaturesException.InconsistentRecord(lineNumber)
        val value = record.get(Headers.Value)
            ?: throw ImportFeaturesException.InconsistentRecord(lineNumber)
        //For legacy reasons (CSV's didn't used to contain notes)
        val note = if (record.isMapped(Headers.Note.name)) record.get(Headers.Note) else ""
        lateinit var parsedTimestamp: OffsetDateTime
        try {
            parsedTimestamp = odtFromString(timestamp)!!
        } catch (_: Exception) {
            throw ImportFeaturesException.BadTimestamp(lineNumber)
        }
        return RecordData(featureName, value, parsedTimestamp, note)
    }

    private fun validateHeaderMap(headerIndexes: Map<String, Int>) {
        val ourHeaders = RequiredHeaders.values().map { v -> v.name }
        val exception = ImportFeaturesException.BadHeaders(ourHeaders.joinToString())
        RequiredHeaders.values().forEach {
            if (!headerIndexes.containsKey(it.name)) throw exception
        }
    }
}