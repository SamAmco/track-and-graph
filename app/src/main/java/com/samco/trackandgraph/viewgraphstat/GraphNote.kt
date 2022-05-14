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

package com.samco.trackandgraph.viewgraphstat

import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.GlobalNote
import com.samco.trackandgraph.base.database.dto.NoteType
import org.threeten.bp.OffsetDateTime

class GraphNote {
    val noteType: NoteType
    val dataPoint: DataPoint?
    val globalNote: GlobalNote?
    val timestamp: OffsetDateTime

    constructor(dataPoint: DataPoint) {
        this.noteType = NoteType.DATA_POINT
        this.dataPoint = dataPoint
        this.globalNote = null
        this.timestamp = dataPoint.timestamp
    }

    constructor(globalNote: GlobalNote) {
        this.noteType = NoteType.GLOBAL_NOTE
        this.globalNote = globalNote
        this.dataPoint = null
        this.timestamp = globalNote.timestamp
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphNote

        if (noteType != other.noteType) return false
        if (dataPoint != other.dataPoint) return false
        if (globalNote != other.globalNote) return false

        return true
    }

    override fun hashCode(): Int {
        var result = noteType.hashCode()
        result = 31 * result + (dataPoint?.hashCode() ?: 0)
        result = 31 * result + (globalNote?.hashCode() ?: 0)
        return result
    }
}
