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
package com.samco.trackandgraph.time

import com.samco.trackandgraph.data.time.TimeProvider
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

// Fake TimeProvider for controlling time in tests
class FakeTimeProvider : TimeProvider {
    var currentTime: ZonedDateTime = ZonedDateTime.now()
    var timeZone: ZoneId = ZoneId.systemDefault()

    override fun now(): ZonedDateTime = currentTime
    override fun defaultZone(): ZoneId = timeZone
}
