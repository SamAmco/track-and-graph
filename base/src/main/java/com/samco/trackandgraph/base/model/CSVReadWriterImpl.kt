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
import androidx.room.withTransaction
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.DiscreteValue
import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.entity.*
import com.samco.trackandgraph.base.database.odtFromString
import com.samco.trackandgraph.base.database.sampling.DataSample
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

import com.samco.trackandgraph.base.database.dto.Feature as FeatureDto

internal class CSVReadWriterImpl(
    private val database: TrackAndGraphDatabase,
    private val dao: TrackAndGraphDatabaseDao,
    private val io: CoroutineDispatcher
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
        features: Map<FeatureDto, DataSample>
    ) {
        var throwable: Throwable? = null
        withContext(io) {
            outStream.writer().use { writer ->
                database.withTransaction {
                    throwable = writeFeaturesToCSVImpl(features, writer).exceptionOrNull()
                }
            }
        }
        throwable?.let { throw it }
    }

    private suspend fun writeFeaturesToCSVImpl(
        features: Map<FeatureDto, DataSample>,
        writer: OutputStreamWriter
    ) = runCatching {
        val csvWriter = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(Headers::class.java))
        for (kvp in features) {
            val feature = kvp.key
            val dataPoints = kvp.value.toList()
            val notes = kvp.value.getRawDataPoints().associate { it.timestamp to it.note }

            when (feature.featureType) {
                DataType.DISCRETE -> writeDiscreteDataPointsToCSV(
                    csvWriter,
                    dataPoints,
                    notes,
                    feature.name
                )
                DataType.CONTINUOUS -> writeContinuousDataPointsToCSV(
                    csvWriter,
                    dataPoints,
                    notes,
                    feature.name
                )
                DataType.DURATION -> writeDurationDataPointsToCSV(
                    csvWriter,
                    dataPoints,
                    notes,
                    feature.name
                )
            }
        }
        writer.flush()
    }

    private suspend fun writeDurationDataPointsToCSV(
        csvPrinter: CSVPrinter,
        dataPoints: List<IDataPoint>,
        notes: Map<OffsetDateTime, String>,
        featureName: String
    ) = dataPoints.forEach { dp ->
        csvPrinter.printRecord(
            featureName,
            dp.timestamp.toString(),
            dp.value.toLong().let { seconds ->
                val absSecs = kotlin.math.abs(seconds)
                String.format("%d:%02d:%02d", seconds / 3600, (absSecs % 3600) / 60, (absSecs % 60))
            },
            notes[dp.timestamp] ?: ""
        )
        yield()
    }

    private suspend fun writeContinuousDataPointsToCSV(
        csvPrinter: CSVPrinter,
        dataPoints: List<IDataPoint>,
        notes: Map<OffsetDateTime, String>,
        featureName: String
    ) = dataPoints.forEach { dp ->
        csvPrinter.printRecord(
            featureName,
            dp.timestamp.toString(),
            dp.value.toString(),
            notes[dp.timestamp] ?: ""
        )
        yield()
    }

    private suspend fun writeDiscreteDataPointsToCSV(
        csvPrinter: CSVPrinter,
        dataPoints: List<IDataPoint>,
        notes: Map<OffsetDateTime, String>,
        featureName: String
    ) = dataPoints.forEach { dp ->
        csvPrinter.printRecord(
            featureName,
            dp.timestamp.toString(),
            "${dp.value.toInt()}:${dp.label}",
            notes[dp.timestamp] ?: ""
        )
        yield()
    }

    override suspend fun readFeaturesFromCSV(inputStream: InputStream, trackGroupId: Long) {
        var throwable: Throwable? = null
        withContext(io) {
            database.withTransaction {
                inputStream.reader().use {
                    throwable = readFeaturesFromCSVImpl(trackGroupId, it).exceptionOrNull()
                }
            }
        }
        throwable?.let { throw it }
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
        val existingFeatures = dao.getFeaturesForGroupSync(trackGroupId).toMutableList()
        val existingFeaturesByName = existingFeatures.associateBy { it.name }.toMutableMap()
        val newDataPoints = mutableListOf<DataPoint>()

        records.forEachIndexed { recordNumber, record ->
            val lineNumber = recordNumber + 2 //one for index by zero and one for headers
            val rec = getValidRecordData(record, lineNumber)
            if (existingFeaturesByName.containsKey(rec.featureName)) {
                addDataPointToExistingFeature(
                    existingFeatures,
                    existingFeaturesByName,
                    newDataPoints,
                    rec,
                    lineNumber
                )
            } else {
                addDataPointToNewFeature(
                    existingFeatures,
                    existingFeaturesByName,
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

    private fun addDataPointToNewFeature(
        existingFeatures: MutableList<Feature>,
        existingFeaturesByName: MutableMap<String, Feature>,
        points: MutableList<DataPoint>,
        trackGroupId: Long,
        rec: RecordData,
        lineNumber: Int
    ) {
        val newFeature = createNewFeature(rec, trackGroupId, lineNumber)
        insertFeature(existingFeatures, existingFeaturesByName, newFeature)
        when (newFeature.featureType) {
            DataType.DISCRETE -> addDiscreteDataPoint(
                points,
                rec.value,
                rec.timestamp,
                newFeature,
                rec.note,
                lineNumber
            )
            DataType.CONTINUOUS -> addContinuousDataPoint(
                points,
                rec.value.toDoubleOrNull(),
                rec.timestamp,
                newFeature.id,
                rec.note
            )
            DataType.DURATION -> addDurationDataPoint(
                points,
                rec.value,
                rec.timestamp,
                newFeature.id,
                rec.note
            )
        }
    }

    private fun addDataPointToExistingFeature(
        existingFeatures: MutableList<Feature>,
        existingFeaturesByName: MutableMap<String, Feature>,
        points: MutableList<DataPoint>,
        rec: RecordData,
        lineNumber: Int
    ) {
        val feature = existingFeaturesByName[rec.featureName]!!
        when (getFeatureTypeForValue(rec.value)) {
            DataType.DISCRETE -> {
                validateFeatureType(feature, DataType.DISCRETE, lineNumber)
                val discreteValue =
                    addDiscreteDataPoint(
                        points,
                        rec.value,
                        rec.timestamp,
                        feature,
                        rec.note,
                        lineNumber
                    )
                if (!feature.discreteValues.contains(discreteValue)) {
                    addDiscreteValueToFeature(
                        existingFeatures,
                        existingFeaturesByName,
                        feature,
                        discreteValue,
                        lineNumber
                    )
                }
            }
            DataType.CONTINUOUS -> {
                validateFeatureType(feature, DataType.CONTINUOUS, lineNumber)
                addContinuousDataPoint(
                    points,
                    rec.value.toDoubleOrNull(),
                    rec.timestamp,
                    feature.id,
                    rec.note
                )
            }
            DataType.DURATION -> {
                validateFeatureType(feature, DataType.DURATION, lineNumber)
                addDurationDataPoint(
                    points,
                    rec.value,
                    rec.timestamp,
                    feature.id,
                    rec.note
                )
            }
        }
    }

    private fun insertFeature(
        existingFeatures: MutableList<Feature>,
        existingFeaturesByName: MutableMap<String, Feature>,
        feature: Feature
    ) {
        existingFeatures.add(feature)
        existingFeaturesByName[feature.name] = feature
    }

    private fun validateNotDiscreteValueConflict(
        discreteValue: DiscreteValue,
        feature: Feature,
        lineNumber: Int
    ) {
        if (feature.discreteValues.any { dv -> dv.index == discreteValue.index }) {
            throw ImportFeaturesException.DiscreteValueConflict(lineNumber)
        }
    }

    private fun addDiscreteValueToFeature(
        existingFeatures: MutableList<Feature>,
        existingFeaturesByName: MutableMap<String, Feature>,
        feature: Feature,
        discreteValue: DiscreteValue,
        lineNumber: Int
    ) {
        validateNotDiscreteValueConflict(discreteValue, feature, lineNumber)
        existingFeatures.remove(feature)
        existingFeaturesByName.remove(feature.name)
        val newFeature =
            tryAddDiscreteValueToFeature(feature, discreteValue, lineNumber)
        existingFeatures.add(newFeature)
        existingFeaturesByName[newFeature.name] = newFeature
    }

    private fun addDiscreteDataPoint(
        points: MutableList<DataPoint>,
        value: String,
        timestamp: OffsetDateTime,
        feature: Feature,
        note: String,
        lineNumber: Int
    ): DiscreteValue {
        val discreteValue = tryGetDiscreteValueFromString(value, lineNumber)
        points.add(
            DataPoint(
                timestamp,
                feature.id,
                discreteValue.index.toDouble(),
                discreteValue.label,
                note
            )
        )
        return discreteValue
    }

    private fun addContinuousDataPoint(
        points: MutableList<DataPoint>,
        value: Double?,
        timestamp: OffsetDateTime,
        featureId: Long,
        note: String
    ) {
        points.add(
            DataPoint(
                timestamp,
                featureId,
                value ?: 1.0,
                "",
                note
            )
        )
    }

    private fun addDurationDataPoint(
        points: MutableList<DataPoint>,
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
            DataPoint(
                timestamp,
                featureId,
                totalSeconds,
                "",
                note
            )
        )
    }

    private fun createNewFeature(
        rec: RecordData,
        trackGroupId: Long,
        lineNumber: Int
    ): Feature {
        val featureType = getFeatureTypeForValue(rec.value)
        val discreteValues =
            if (featureType == DataType.DISCRETE) listOf(
                tryGetDiscreteValueFromString(
                    rec.value,
                    lineNumber
                )
            )
            else listOf()
        val newFeature = Feature(
            0, rec.featureName, trackGroupId, featureType,
            discreteValues, 0, false, 1.0, ""
        )
        val featureId = dao.insertFeature(newFeature)
        return newFeature.copy(id = featureId)
    }

    private fun tryGetDiscreteValueFromString(string: String, lineNumber: Int): DiscreteValue {
        try {
            return DiscreteValue.fromString(string)
        } catch (e: Exception) {
            throw ImportFeaturesException.BadDiscreteValue(lineNumber)
        }
    }

    private fun tryAddDiscreteValueToFeature(
        feature: Feature,
        discreteValue: DiscreteValue,
        lineNumber: Int
    ): Feature {
        val newDiscreteValues = feature.discreteValues.toMutableList()
        if (newDiscreteValues.map { dv -> dv.index }.contains(discreteValue.index))
            throw ImportFeaturesException.DiscreteValueIndexExists(lineNumber, discreteValue.index)
        if (newDiscreteValues.map { dv -> dv.label }.contains(discreteValue.label))
            throw ImportFeaturesException.DiscreteValueLabelExists(lineNumber, discreteValue.label)
        newDiscreteValues.add(discreteValue)
        dao.updateFeature(feature.copy(discreteValues = newDiscreteValues))
        return feature.copy(discreteValues = newDiscreteValues)
    }

    private fun validateFeatureType(feature: Feature, expectedType: DataType, lineNumber: Int) {
        if (feature.featureType != expectedType)
            throw ImportFeaturesException.InconsistentDataType(lineNumber)
    }

    private fun getFeatureTypeForValue(value: String): DataType {
        val durationRegex = Regex("\\d*:\\d{2}:\\d{2}")
        return when {
            value.matches(durationRegex) -> DataType.DURATION
            value.contains(":") -> DataType.DISCRETE
            else -> DataType.CONTINUOUS
        }
    }

    private data class RecordData(
        val featureName: String,
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