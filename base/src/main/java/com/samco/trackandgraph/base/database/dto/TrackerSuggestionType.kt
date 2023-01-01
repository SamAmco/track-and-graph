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

import com.samco.trackandgraph.base.database.entity.TrackerSuggestionType

enum class TrackerSuggestionType {
    VALUE_AND_LABEL,
    VALUE_ONLY,
    LABEL_ONLY;

    companion object {

        internal fun fromEntity(entity: TrackerSuggestionType) =
            when (entity) {
                TrackerSuggestionType.VALUE_AND_LABEL -> VALUE_AND_LABEL
                TrackerSuggestionType.VALUE_ONLY -> VALUE_ONLY
                TrackerSuggestionType.LABEL_ONLY -> LABEL_ONLY
            }
    }

    internal fun toEntity(): TrackerSuggestionType {
        return when (this) {
            VALUE_AND_LABEL -> TrackerSuggestionType.VALUE_AND_LABEL
            VALUE_ONLY -> TrackerSuggestionType.VALUE_ONLY
            LABEL_ONLY -> TrackerSuggestionType.LABEL_ONLY
        }
    }
}
