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
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.DataType
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

//TODO really need to test this class
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
        var dataPointString = if (isDuration) {
            dp.value.toLong().let { seconds ->
                String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60))
            }
        } else dp.value.toString()

        if (dp.label.isNotBlank()) dataPointString = "${dataPointString}:${dp.label}"

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
                    rec
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
    ) {
        val newTracker = createNewTracker(rec, trackGroupId)
        insertTracker(existingTrackers, existingTrackersByName, newTracker)
        points.add(
            DataPointEntity(
                timestamp = rec.timestamp,
                featureId = newTracker.featureId,
                value = rec.value,
                label = rec.label,
                note = rec.note
            )
        )
    }

    private fun addDataPointToExistingFeature(
        existingTrackersByName: MutableMap<String, Tracker>,
        points: MutableList<DataPointEntity>,
        rec: RecordData,
        lineNumber: Int
    ) {
        val tracker = existingTrackersByName[rec.trackerName]!!
        validateDataType(tracker, rec.isDuration, lineNumber)
        points.add(
            DataPointEntity(
                timestamp = rec.timestamp,
                featureId = tracker.featureId,
                value = rec.value,
                label = rec.label,
                note = rec.note
            )
        )
    }

    private fun validateDataType(tracker: Tracker, isDuration: Boolean, lineNumber: Int) {
        if (isDuration && tracker.dataType != DataType.DURATION)
            throw ImportFeaturesException.InconsistentDataType(lineNumber)
        else if (!isDuration && tracker.dataType != DataType.CONTINUOUS)
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

    private suspend fun createNewTracker(
        rec: RecordData,
        trackGroupId: Long
    ): Tracker {
        val dataType = when (rec.isDuration) {
            true -> DataType.DURATION
            false -> DataType.CONTINUOUS
        }
        val newTracker = Tracker(
            id = 0,
            name = rec.trackerName,
            groupId = trackGroupId,
            featureId = 0L,
            displayIndex = 0,
            description = "",
            dataType = dataType,
            hasDefaultValue = false,
            defaultValue = 1.0,
            defaultLabel = ""
        )
        val trackerId = trackerHelper.insertTracker(newTracker)
        return trackerHelper.getTrackerById(trackerId)!!
    }

    private data class RecordData(
        val trackerName: String,
        val value: Double,
        val label: String,
        val isDuration: Boolean,
        val timestamp: OffsetDateTime,
        val note: String
    )

    private fun getValidRecordData(record: CSVRecord, lineNumber: Int): RecordData {
        val trackerName = record.get(Headers.FeatureName)
            ?: throw ImportFeaturesException.InconsistentRecord(lineNumber)
        val timestamp = record.get(Headers.Timestamp)
            ?: throw ImportFeaturesException.InconsistentRecord(lineNumber)
        val valueString = record.get(Headers.Value)
            ?: throw ImportFeaturesException.InconsistentRecord(lineNumber)
        //For legacy reasons (CSV's didn't used to contain notes)
        val note = if (record.isMapped(Headers.Note.name)) record.get(Headers.Note) else ""

        return parseToRecordData(
            trackerName = trackerName,
            timestamp = timestamp,
            valueString = valueString,
            note = note,
            lineNumber = lineNumber
        )
    }

    private fun parseToRecordData(
        trackerName: String,
        timestamp: String,
        valueString: String,
        note: String,
        lineNumber: Int
    ): RecordData {
        val parsedTimestamp = try {
            odtFromString(timestamp)!!
        } catch (_: Exception) {
            throw ImportFeaturesException.BadTimestamp(lineNumber)
        }

        val colons = valueString.count { it == ':' }
        val value: Double
        var label = ""
        var isDuration = false
        val durationRegex = Regex("^-?\\d*:-?\\d{2}:-?\\d{2}")

        when {
            colons == 0 -> {
                value = valueString.toDouble()
            }
            colons == 2 && valueString.matches(durationRegex) -> {
                value = parseDuration(valueString)
                isDuration = true
            }
            colons > 2 && valueString.contains(durationRegex) -> {
                value = parseDuration(valueString)
                val matchResult = durationRegex.find(valueString)
                if (valueString.length > matchResult!!.range.last + 2)
                    label = valueString.substring(matchResult.range.last + 2)
                isDuration = true
            }
            else -> {
                value = valueString.split(":").first().toDoubleOrNull()
                    ?: throw ImportFeaturesException.InconsistentRecord(lineNumber)
                label = valueString.dropWhile { it != ':' }.drop(1)
                isDuration = false
            }
        }

        return RecordData(
            trackerName = trackerName,
            value = value,
            label = label,
            isDuration = isDuration,
            timestamp = parsedTimestamp,
            note = note
        )
    }

    private fun parseDuration(duration: String): Double {
        val segments = duration.split(":")
        val hours = segments.getOrNull(0)?.toLong() ?: 0L
        val minutes = segments.getOrNull(1)?.toLong() ?: 0L
        val seconds = segments.getOrNull(2)?.toLong() ?: 0L
        return Duration.ZERO
            .plusHours(hours)
            .plusMinutes(minutes)
            .plusSeconds(seconds)
            .seconds
            .toDouble()
    }

    private fun validateHeaderMap(headerIndexes: Map<String, Int>) {
        val ourHeaders = RequiredHeaders.values().map { v -> v.name }
        val exception = ImportFeaturesException.BadHeaders(ourHeaders.joinToString())
        RequiredHeaders.values().forEach {
            if (!headerIndexes.containsKey(it.name)) throw exception
        }
    }
}