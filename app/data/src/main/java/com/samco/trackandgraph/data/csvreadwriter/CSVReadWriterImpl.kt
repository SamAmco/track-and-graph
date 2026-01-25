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

package com.samco.trackandgraph.data.csvreadwriter

import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.Feature
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.data.database.odtFromString
import com.samco.trackandgraph.data.interactor.TrackerHelper
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.sampling.DataSampleProperties
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.sampling.RawDataSample
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVRecord
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import timber.log.Timber
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject
import com.samco.trackandgraph.data.database.entity.DataPoint as DataPointEntity

internal class CSVReadWriterImpl @Inject constructor(
    private val dao: TrackAndGraphDatabaseDao,
    private val trackerHelper: TrackerHelper,
    private val dataSampler: DataSampler,
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
        Label,
        Note
    }

    private data class WriteFeatureData(
        val feature: Feature,
        val dataPoints: List<DataPoint>,
        val dataSampleProperties: DataSampleProperties?
    )

    override suspend fun writeFeaturesToCSV(
        outStream: OutputStream,
        features: List<Feature>
    ) {
        withContext(io) {
            outStream.writer().use { outputStreamWriter ->
                val samples = features.map { getWriteFeatureData(it) }
                writeFeaturesToCSVImpl(samples, outputStreamWriter)
            }
        }
    }

    private suspend fun getWriteFeatureData(feature: Feature): WriteFeatureData {
        var rawDataSample: RawDataSample? = null
        return try {
            rawDataSample = dataSampler
                .getRawDataSampleForFeatureId(feature.featureId)
            val dataPoints = rawDataSample?.toList() ?: emptyList()
            val dataSampleProperties = dataSampler
                .getDataSamplePropertiesForFeatureId(feature.featureId)
            WriteFeatureData(feature, dataPoints, dataSampleProperties)
        } finally {
            rawDataSample?.dispose()
        }
    }

    private suspend fun writeFeaturesToCSVImpl(
        writeFeatureData: List<WriteFeatureData>,
        writer: OutputStreamWriter
    ) {
        val csvWriter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(Headers::class.java))
        try {
            for (wfd in writeFeatureData) {
                writeDataPointsToCSV(
                    csvWriter,
                    wfd.dataPoints,
                    wfd.feature.name,
                    wfd.dataSampleProperties?.isDuration ?: false,
                )
            }
        } catch (e: Throwable) {
            Timber.tag("CSVReadWriterImpl").e(e, "CSV writer threw exception")
            if (e is ImportFeaturesException) throw e
            else throw ImportFeaturesException.Unknown()
        } finally {
            csvWriter.close(true)
        }
    }

    private suspend fun writeDataPointsToCSV(
        csvPrinter: CSVPrinter,
        dataPoints: List<DataPoint>,
        featureName: String,
        isDuration: Boolean
    ) = dataPoints.forEach { dp ->
        val dataPointString = if (isDuration) {
            dp.value.toLong().let { seconds ->
                String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60))
            }
        } else dp.value.toString()

        csvPrinter.printRecord(
            featureName,
            dp.timestamp.toString(),
            dataPointString,
            dp.label,
            dp.note,
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
        } catch (e: Throwable) {
            Timber.tag("CSVReadWriterImpl").e(e, "CSV reader through exception")
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
                epochMilli = rec.timestamp.toInstant().toEpochMilli(),
                utcOffsetSec = rec.timestamp.offset.totalSeconds,
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
                epochMilli = rec.timestamp.toInstant().toEpochMilli(),
                utcOffsetSec = rec.timestamp.offset.totalSeconds,
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
        val label = if (record.isMapped(Headers.Label.name)) record.get(Headers.Label) else null
        //For legacy reasons (CSV's didn't used to contain notes)
        val note = if (record.isMapped(Headers.Note.name)) record.get(Headers.Note) else ""

        return parseToRecordData(
            trackerName = trackerName,
            timestamp = timestamp,
            csvLabel = label,
            valueString = valueString,
            note = note,
            lineNumber = lineNumber
        )
    }

    private fun parseToRecordData(
        trackerName: String,
        timestamp: String,
        csvLabel: String?,
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
            label = csvLabel ?: label,
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
        val ourHeaders = RequiredHeaders.entries.map { v -> v.name }
        RequiredHeaders.entries.forEach {
            if (!headerIndexes.containsKey(it.name)) {
                throw ImportFeaturesException.BadHeaders(ourHeaders.joinToString())
            }
        }
    }
}