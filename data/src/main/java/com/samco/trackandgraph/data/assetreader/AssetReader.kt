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
package com.samco.trackandgraph.data.assetreader

interface AssetReader {
    fun readAssetToString(assetPath: String): String
    
    /**
     * Finds all files in the given asset directory path that match the given suffix
     * @param assetDirectoryPath the directory path in assets to search
     * @param suffix the file suffix to match (e.g., ".apispec.lua")
     * @return list of file paths relative to the asset directory
     */
    suspend fun findFilesWithSuffix(assetDirectoryPath: String, suffix: String): List<String>
}
