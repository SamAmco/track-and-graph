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

package com.samco.trackandgraph.data.interactor

import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionCreateRequest
import com.samco.trackandgraph.data.database.dto.FunctionDeleteRequest
import com.samco.trackandgraph.data.database.dto.FunctionUpdateRequest

/**
 * An interface for managing functions. Do not use this interface directly, it is implemented by
 * the DataInteractor interface.
 *
 * The implementation of FunctionHelper will manage the complete lifecycle of functions including
 * their steps and input features. It will perform all changes inside a transaction and
 * throw an exception if anything goes wrong.
 */
interface FunctionHelper {
    suspend fun insertFunction(request: FunctionCreateRequest): Long?

    suspend fun updateFunction(request: FunctionUpdateRequest)

    suspend fun deleteFunction(request: FunctionDeleteRequest)

    suspend fun getFunctionById(functionId: Long): Function?

    suspend fun tryGetFunctionByFeatureId(featureId: Long): Function?

    suspend fun getAllFunctionsSync(): List<Function>

    suspend fun getFunctionsForGroupSync(groupId: Long): List<Function>

    suspend fun duplicateFunction(function: Function): Long?

    suspend fun hasAnyFunctions(): Boolean
}
