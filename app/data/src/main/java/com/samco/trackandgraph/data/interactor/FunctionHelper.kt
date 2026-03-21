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
import com.samco.trackandgraph.data.database.dto.ComponentDeleteRequest
import com.samco.trackandgraph.data.database.dto.CreatedComponent
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
    suspend fun insertFunction(request: FunctionCreateRequest): CreatedComponent?

    suspend fun updateFunction(request: FunctionUpdateRequest)

    suspend fun deleteFunction(request: ComponentDeleteRequest)

    suspend fun getFunctionById(functionId: Long): Function?

    suspend fun tryGetFunctionByFeatureId(featureId: Long): Function?

    suspend fun getFunctionsForGroupSync(groupId: Long): List<Function>

    /**
     * Duplicates the function identified by the given GroupItem placement.
     * The duplicate is placed immediately after the original in the same group.
     *
     * @param groupItemId The GroupItem.id of the placement to duplicate.
     * @return The new component's ID and GroupItem placement, or null if the function was not found.
     */
    suspend fun duplicateFunction(groupItemId: Long): CreatedComponent?

    suspend fun hasAnyFunctions(): Boolean
}
