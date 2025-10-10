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
package com.samco.trackandgraph.functions.service

/**
 * Data class containing the Lua functions file and its signature data
 */
class FunctionsCatalogData(
    val luaScriptBytes: ByteArray,
    val signatureData: SignatureData
)

/**
 * Service interface for fetching community functions and their signatures
 */
interface FunctionsService {
    /**
     * Fetches the community functions Lua file and its signature.
     * Both are retrieved together to ensure consistency.
     * 
     * @return FunctionsCatalogData containing the Lua script and signature
     * @throws Exception if the files cannot be retrieved
     */
    suspend fun fetchFunctionsCatalog(): FunctionsCatalogData
}
