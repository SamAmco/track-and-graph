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

import timber.log.Timber
import javax.inject.Inject

/**
 * Production implementation of FunctionsService that will fetch files from online sources.
 * Currently a stub - implementation to be added later.
 */
class ProductionFunctionsService @Inject constructor(
    // TODO: Add network dependencies (e.g., HTTP client, URL configuration)
) : FunctionsService {

    override suspend fun fetchFunctionsCatalog(): FunctionsCatalogData {
        // TODO: Implement online fetching of community functions and signature
        Timber.w("ProductionFunctionsService not yet implemented - returning empty data")
        
        // Stub implementation - return empty data for now
        return FunctionsCatalogData(
            luaScriptBytes = ByteArray(0),
            signatureData = SignatureData(
                keyId = "",
                algorithm = "",
                signature = ""
            )
        )
    }
}
