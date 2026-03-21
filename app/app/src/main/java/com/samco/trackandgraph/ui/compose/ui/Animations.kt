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
package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

private const val FAB_ANIMATION_DURATION_MS = 300

val fabEnterTransition: EnterTransition =
    scaleIn(animationSpec = tween(FAB_ANIMATION_DURATION_MS)) +
        fadeIn(animationSpec = tween(FAB_ANIMATION_DURATION_MS))

val fabExitTransition: ExitTransition =
    scaleOut(animationSpec = tween(FAB_ANIMATION_DURATION_MS)) +
        fadeOut(animationSpec = tween(FAB_ANIMATION_DURATION_MS))
