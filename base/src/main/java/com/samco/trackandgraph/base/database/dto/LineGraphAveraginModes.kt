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

enum class LineGraphAveraginModes {
    NO_AVERAGING,
    DAILY_MOVING_AVERAGE,
    THREE_DAY_MOVING_AVERAGE,
    WEEKLY_MOVING_AVERAGE,
    MONTHLY_MOVING_AVERAGE,
    THREE_MONTH_MOVING_AVERAGE,
    SIX_MONTH_MOVING_AVERAGE,
    YEARLY_MOVING_AVERAGE
}