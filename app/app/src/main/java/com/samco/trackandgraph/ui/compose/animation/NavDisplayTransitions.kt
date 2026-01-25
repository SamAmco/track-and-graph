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
package com.samco.trackandgraph.ui.compose.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

const val NAV_ANIM_DURATION_MILLIS = 280

fun <T : Any> popTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    return {
        slideInHorizontally(
            animationSpec = tween(NAV_ANIM_DURATION_MILLIS, easing = FastOutSlowInEasing),
            initialOffsetX = { -(it / 3) } // parallax in from left
        ) + fadeIn() togetherWith
                slideOutHorizontally(
                    animationSpec = tween(NAV_ANIM_DURATION_MILLIS, easing = FastOutSlowInEasing),
                    targetOffsetX = { it } // exit to right
                ) + fadeOut()
    }
}

fun <T : Any> predictivePopTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform {
    return {
        slideInHorizontally(
            animationSpec = tween(NAV_ANIM_DURATION_MILLIS, easing = FastOutSlowInEasing),
            initialOffsetX = { -(it / 3) } // parallax in from left
        ) + fadeIn() togetherWith
                slideOutHorizontally(
                    animationSpec = tween(NAV_ANIM_DURATION_MILLIS, easing = FastOutSlowInEasing),
                    targetOffsetX = { it } // exit to right
                ) + fadeOut()

    }
}

fun <T : Any> transitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    return {
        slideInHorizontally(
            animationSpec = tween(NAV_ANIM_DURATION_MILLIS, easing = FastOutSlowInEasing),
            initialOffsetX = { it } // start just off the right edge
        ) + fadeIn() togetherWith
                slideOutHorizontally(
                    animationSpec = tween(NAV_ANIM_DURATION_MILLIS, easing = FastOutSlowInEasing),
                    targetOffsetX = { -(it / 3) } // slight parallax
                ) + fadeOut()
    }
}
