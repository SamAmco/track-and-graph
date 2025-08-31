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

package com.samco.trackandgraph.data.model

import com.samco.trackandgraph.data.database.dto.Function
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FunctionHelperImpl @Inject constructor(
    // TODO: Add required dependencies (database, dao, etc.)
) : FunctionHelper {

    override suspend fun insertFunction(function: Function): Long {
        TODO("Not yet implemented")
    }

    override suspend fun updateFunction(function: Function) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFunction(functionId: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun getFunctionById(functionId: Long): Function? {
        TODO("Not yet implemented")
    }

    override suspend fun getFunctionByFeatureId(featureId: Long): Function? {
        TODO("Not yet implemented")
    }

    override suspend fun getAllFunctionsSync(): List<Function> {
        TODO("Not yet implemented")
    }

    override suspend fun getFunctionsForGroupSync(groupId: Long): List<Function> {
        TODO("Not yet implemented")
    }

    override suspend fun duplicateFunction(function: Function): Long? {
        TODO("Not yet implemented")
    }

    override suspend fun hasAnyFunctions(): Boolean {
        TODO("Not yet implemented")
    }
}
