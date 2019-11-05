package com.samco.trackandgraph.util

import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import kotlinx.coroutines.yield
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVRecord
import org.threeten.bp.OffsetDateTime
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception

object CSVReadWriter {
    enum class HEADERS {
        FeatureName,
        Timestamp,
        Value
    }

    suspend fun writeFeaturesToCSV(features: List<Feature>, dataSource: TrackAndGraphDatabaseDao, outStream: OutputStream) {
        outStream.writer().use {
            val csvWriter = CSVPrinter(it, CSVFormat.DEFAULT.withHeader(HEADERS::class.java))
            for (feature in features) {
                val dataPoints = dataSource.getDataPointsForFeatureSync(feature.id)
                dataPoints.forEach { dp ->
                    var value = dp.value
                    if (feature.featureType == FeatureType.DISCRETE) {
                        value = DiscreteValue.fromDataPoint(dp).toString()
                    }
                    csvWriter.printRecord(feature.name, dp.timestamp.toString(), value)
                    yield()
                }
            }
            it.flush()
        }
    }

    suspend fun readFeaturesFromCSV(dataSource: TrackAndGraphDatabaseDao, inputStream: InputStream, trackGroupId: Long, validationCharacters: String) {
        try {
            inputStream.reader().use {
                val records = CSVFormat.DEFAULT
                    .withHeader(HEADERS::class.java)
                    .withFirstRecordAsHeader()
                    .parse(it)
                val headerMap = records.headerMap
                validateHeaderMap(headerMap)
                ingestRecords(dataSource, records, trackGroupId, validationCharacters)
            }
        } catch (e: Exception) {
            if (e is ImportFeaturesException) throw e
            else throw ImportFeaturesException(R.string.import_exception_unknown)
        }
    }

    private suspend fun ingestRecords(dataSource: TrackAndGraphDatabaseDao, records: Iterable<CSVRecord>, trackGroupId: Long, validationCharacters: String) {
        val existingFeatures = dataSource.getFeaturesForTrackGroupSync(trackGroupId).toMutableList()
        val existingFeaturesByName = existingFeatures.map { it.name to it }.toMap().toMutableMap()
        val newDataPoints = mutableListOf<DataPoint>()

        val insertFeature = {feature: Feature ->
            existingFeatures.add(feature)
            existingFeaturesByName[feature.name] = feature
        }

        val validateNotDiscreteValueConflict = { discreteValue: DiscreteValue, feature: Feature, lineNumber: Int ->
            if (feature.discreteValues.any { dv -> dv.index == discreteValue.index })
                throw ImportFeaturesException(R.string.import_exception_discrete_value_conflict, listOf(lineNumber.toString()))
        }

        val addDiscreteValueToFeature =  { feature: Feature, discreteValue: DiscreteValue, lineNumber: Int->
            validateNotDiscreteValueConflict(discreteValue, feature, lineNumber)
            existingFeatures.remove(feature)
            existingFeaturesByName.remove(feature.name)
            val newFeature = tryAddDiscreteValueToFeature(dataSource, feature, discreteValue, lineNumber)
            existingFeatures.add(newFeature)
            existingFeaturesByName[newFeature.name] = newFeature
        }

        val addContinuousDataPoint = { value: String, timestamp: OffsetDateTime, featureId: Long, lineNumber: Int ->
            validateValueContinuous(value, lineNumber)
            newDataPoints.add(DataPoint(timestamp, featureId, value, ""))
        }

        val addDiscreteDataPoint = { value: String, timestamp: OffsetDateTime, feature: Feature, lineNumber: Int ->
            val discreteValue = tryGetDiscreteValueFromString(value, lineNumber, validationCharacters)
            newDataPoints.add(DataPoint(timestamp, feature.id, discreteValue.index.toString(), discreteValue.label))
            discreteValue
        }

        records.forEachIndexed { recordNumber, record ->
            val lineNumber = recordNumber + 2 //one for index by zero and one for headers
            val rec = getValidRecordData(record, lineNumber)
            if (existingFeaturesByName.containsKey(rec.featureName)) {
                val feature = existingFeaturesByName[rec.featureName]!!
                if (getFeatureTypeForValue(rec.value) == FeatureType.DISCRETE) {
                    validateFeatureDiscrete(feature, lineNumber)
                    val discreteValue = addDiscreteDataPoint(rec.value, rec.timestamp, feature, lineNumber)
                    if (!feature.discreteValues.contains(discreteValue)) addDiscreteValueToFeature(feature, discreteValue, lineNumber)
                } else {
                    validateFeatureContinuous(feature, lineNumber)
                    addContinuousDataPoint(rec.value, rec.timestamp, feature.id, lineNumber)
                }
            } else {
                validateInputString(rec.featureName, validationCharacters)
                val newFeature = createNewFeature(dataSource, rec, trackGroupId, lineNumber, validationCharacters)
                insertFeature(newFeature)
                if (newFeature.featureType == FeatureType.DISCRETE) addDiscreteDataPoint(rec.value, rec.timestamp, newFeature, lineNumber)
                else addContinuousDataPoint(rec.value, rec.timestamp, newFeature.id, lineNumber)
            }
            if (lineNumber % 1000 == 0) {
                dataSource.insertDataPoints(newDataPoints)
                newDataPoints.clear()
            }
            yield()
        }
        if (newDataPoints.isNotEmpty()) dataSource.insertDataPoints(newDataPoints)
    }

    private fun createNewFeature(dataSource: TrackAndGraphDatabaseDao, rec: RecordData, trackGroupId: Long, lineNumber: Int, validationCharacters: String): Feature {
        if (rec.featureName.length > MAX_FEATURE_NAME_LENGTH) throw ImportFeaturesException(R.string.import_exception_feature_name_too_long, listOf(lineNumber.toString()))
        val featureType = getFeatureTypeForValue(rec.value)
        val discreteValues =
            if (featureType == FeatureType.DISCRETE) listOf(tryGetDiscreteValueFromString(rec.value, lineNumber, validationCharacters))
            else listOf()
        val newFeature = Feature(0, rec.featureName, trackGroupId, featureType, discreteValues, 0)
        val featureId = dataSource.insertFeature(newFeature)
        newFeature.id = featureId
        return newFeature
    }

    private fun validateValueContinuous(value: String, lineNumber: Int) {
        if (value.toDoubleOrNull() == null) throw ImportFeaturesException(R.string.import_exception_not_a_valid_number, listOf(lineNumber.toString()))
    }

    private fun tryGetDiscreteValueFromString(string: String, lineNumber: Int, validationCharacters: String): DiscreteValue {
        try {
            val discreteValue = DiscreteValue.fromString(string)
            validateInputString(discreteValue.label, validationCharacters)
            return discreteValue
        }
        catch (e: Exception) { throw ImportFeaturesException(R.string.import_exception_bad_discrete_value, listOf(lineNumber.toString())) }
    }

    private fun tryAddDiscreteValueToFeature(dataSource: TrackAndGraphDatabaseDao, feature: Feature, discreteValue: DiscreteValue, lineNumber: Int): Feature {
        val newDiscreteValues = feature.discreteValues.toMutableList()
        if (newDiscreteValues.size == MAX_DISCRETE_VALUES_PER_FEATURE)
            throw ImportFeaturesException(R.string.import_exception_max_discrete_values_reached, listOf(MAX_DISCRETE_VALUES_PER_FEATURE.toString()))
        if (newDiscreteValues.map { dv -> dv.index }.contains(discreteValue.index))
            throw ImportFeaturesException(R.string.import_exception_discrete_value_index_exists, listOf(lineNumber.toString(), discreteValue.index.toString()))
        if (newDiscreteValues.map { dv -> dv.label }.contains(discreteValue.label))
            throw ImportFeaturesException(R.string.import_exception_discrete_value_label_exists, listOf(lineNumber.toString(), discreteValue.label))
        newDiscreteValues.add(discreteValue)
        dataSource.updateFeature(feature.copy(discreteValues = newDiscreteValues))
        return feature.copy(discreteValues = newDiscreteValues)
    }

    private fun validateFeatureContinuous(feature: Feature, lineNumber: Int) {
        if (feature.featureType != FeatureType.CONTINUOUS)
            throw ImportFeaturesException(R.string.import_exception_inconsistent_data_type, listOf(lineNumber.toString()))
    }

    private fun validateFeatureDiscrete(feature: Feature, lineNumber: Int) {
        if (feature.featureType != FeatureType.DISCRETE)
            throw ImportFeaturesException(R.string.import_exception_inconsistent_data_type, listOf(lineNumber.toString()))
    }

    private fun getFeatureTypeForValue(value: String): FeatureType {
        return if (value.contains(':')) FeatureType.DISCRETE else FeatureType.CONTINUOUS
    }

    private fun validateInputString(input: String, validationCharacters: String) {
        val invalidChar = input.firstOrNull { c -> !validationCharacters.contains(c) }
        if (invalidChar != null) throw ImportFeaturesException(R.string.import_exception_invalid_character, listOf(invalidChar.toString()))
    }

    class ImportFeaturesException(val stringId: Int, val stringArgs: List<String>? = null) : Exception()

    private data class RecordData (
        val featureName: String,
        val value: String,
        val timestamp: OffsetDateTime
    )

    private fun getValidRecordData(record: CSVRecord, lineNumber: Int): RecordData {
        if (!record.isConsistent) throw ImportFeaturesException(R.string.import_exception_inconsistent_record, listOf(lineNumber.toString()))
        val featureName = record.get(HEADERS.FeatureName)
        val timestamp = record.get(HEADERS.Timestamp)
        val value = record.get(HEADERS.Value)
        lateinit var parsedTimestamp: OffsetDateTime
        try { parsedTimestamp = odtFromString(timestamp) }
        catch (_: Exception) { throw ImportFeaturesException(R.string.import_exception_bad_timestamp, listOf(lineNumber.toString())) }
        return RecordData(featureName, value, parsedTimestamp)
    }

    private fun validateHeaderMap(headerIndexes: Map<String, Int>) {
        val ourHeaders = HEADERS.values().map{ v -> v.name }
        val exception = ImportFeaturesException(R.string.import_exception_bad_headers, listOf(ourHeaders.joinToString()))
        if (headerIndexes.size != HEADERS.values().size) throw exception
        HEADERS.values().forEach {
            if (!headerIndexes.containsKey(it.name) || headerIndexes[it.name] != ourHeaders.indexOf(it.name)) throw exception
        }
    }
}
