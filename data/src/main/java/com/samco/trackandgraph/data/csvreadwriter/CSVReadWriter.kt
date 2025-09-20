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

import com.samco.trackandgraph.data.database.dto.Feature
import com.samco.trackandgraph.data.sampling.DataSample
import java.io.InputStream
import java.io.OutputStream

sealed class ImportFeaturesException : Exception() {
    class Unknown : ImportFeaturesException()
    class InconsistentDataType(val lineNumber: Int) : ImportFeaturesException()
    class InconsistentRecord(val lineNumber: Int) : ImportFeaturesException()
    class BadTimestamp(val lineNumber: Int) : ImportFeaturesException()
    class BadHeaders(val requiredHeaders: String) : ImportFeaturesException()
}


interface CSVReadWriter {
    suspend fun writeFeaturesToCSV(outStream: OutputStream, features: Map<Feature, DataSample>)
    suspend fun readFeaturesFromCSV(inputStream: InputStream, trackGroupId: Long)
}