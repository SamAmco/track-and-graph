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

sealed class DataUpdateType {
    /** Something changed, but we don't know what, e.g. due to external data import **/
    data object Unknown : DataUpdateType()

    /**Data point(s) were created/updated/deleted for a given feature**/
    data class DataPoint(val featureId: Long) : DataUpdateType()

    data object GroupCreated : DataUpdateType()
    data object GroupUpdated : DataUpdateType()
    data object GroupDeleted : DataUpdateType()

    interface FeatureUpdate {
        val featureId: Long
    }

    data object TrackerCreated : DataUpdateType()
    data object TrackerUpdated : DataUpdateType()
    data object TrackerDeleted : DataUpdateType()

    data class GraphOrStatCreated(val graphStatId: Long) : DataUpdateType()
    data class GraphOrStatUpdated(val graphStatId: Long) : DataUpdateType()
    data object GraphOrStatDeleted : DataUpdateType()

    /**A reminder was created/updated/deleted **/
    data object Reminder : DataUpdateType()

    /**A global note was created/updated/deleted **/
    data object GlobalNote : DataUpdateType()

    /**Display indices were updated **/
    data object DisplayIndex : DataUpdateType()

    data class FunctionCreated(override val featureId: Long) : DataUpdateType(), FeatureUpdate
    data class FunctionUpdated(override val featureId: Long) : DataUpdateType(), FeatureUpdate
    data class FunctionDeleted(override val featureId: Long) : DataUpdateType(), FeatureUpdate
}