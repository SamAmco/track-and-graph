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
package com.samco.trackandgraph.base.database.dto

import com.samco.trackandgraph.base.database.entity.TrackerSuggestionOrder

enum class TrackerSuggestionOrder {
    VALUE_ASCENDING,
    VALUE_DESCENDING,
    LABEL_ASCENDING,
    LABEL_DESCENDING,
    LATEST,
    OLDEST;

    companion object {
        internal fun fromEntity(entity: TrackerSuggestionOrder) =
            when (entity) {
                TrackerSuggestionOrder.VALUE_ASCENDING -> VALUE_ASCENDING
                TrackerSuggestionOrder.VALUE_DESCENDING -> VALUE_DESCENDING
                TrackerSuggestionOrder.LABEL_ASCENDING -> LABEL_ASCENDING
                TrackerSuggestionOrder.LABEL_DESCENDING -> LABEL_DESCENDING
                TrackerSuggestionOrder.LATEST -> LATEST
                TrackerSuggestionOrder.OLDEST -> OLDEST
            }
    }

    internal fun toEntity(): TrackerSuggestionOrder {
        return when (this) {
            VALUE_ASCENDING -> TrackerSuggestionOrder.VALUE_ASCENDING
            VALUE_DESCENDING -> TrackerSuggestionOrder.VALUE_DESCENDING
            LABEL_ASCENDING -> TrackerSuggestionOrder.LABEL_ASCENDING
            LABEL_DESCENDING -> TrackerSuggestionOrder.LABEL_DESCENDING
            LATEST -> TrackerSuggestionOrder.LATEST
            OLDEST -> TrackerSuggestionOrder.OLDEST
        }
    }
}