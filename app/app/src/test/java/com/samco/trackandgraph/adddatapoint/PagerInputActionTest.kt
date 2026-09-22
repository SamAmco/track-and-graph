/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph. If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.adddatapoint

import org.junit.Assert.assertEquals
import org.junit.Test

class PagerInputActionTest {

    @Test
    fun `unknown keyboard intent waits for suggestions`() {
        assertEquals(
            PagerInputAction.None,
            planAction(destinationKeyboardIntent = KeyboardIntent.Unknown),
        )
    }

    @Test
    fun `show waits until destination focus targets are available`() {
        assertEquals(
            PagerInputAction.None,
            planAction(
                destinationKeyboardIntent = KeyboardIntent.Show,
                availableTargets = null,
            ),
        )
    }

    @Test
    fun `hidden keyboard focuses destination and starts showing during scroll`() {
        assertEquals(
            PagerInputAction.FocusDestination(
                page = 1,
                field = TrackerPageFocusTarget.Value,
                showKeyboard = true,
            ),
            planAction(
                destinationKeyboardIntent = KeyboardIntent.Show,
                isScrollInProgress = true,
                lastAppliedKeyboardIntent = KeyboardIntent.Hide,
            ),
        )
    }

    @Test
    fun `visible keyboard keeps value focus on source until scroll settles`() {
        assertEquals(
            PagerInputAction.None,
            planAction(
                destinationKeyboardIntent = KeyboardIntent.Show,
                isScrollInProgress = true,
                lastAppliedKeyboardIntent = KeyboardIntent.Show,
            ),
        )
    }

    @Test
    fun `value focus transfers to destination after scroll settles`() {
        assertEquals(
            PagerInputAction.FocusDestination(
                page = 1,
                field = TrackerPageFocusTarget.Value,
                showKeyboard = false,
            ),
            planAction(
                destinationKeyboardIntent = KeyboardIntent.Show,
                isScrollInProgress = false,
                lastAppliedKeyboardIntent = KeyboardIntent.Show,
            ),
        )
    }

    @Test
    fun `supplemental focus transfers to destination during scroll`() {
        assertEquals(
            PagerInputAction.FocusDestination(
                page = 1,
                field = TrackerPageFocusTarget.Label,
                showKeyboard = false,
            ),
            planAction(
                destinationKeyboardIntent = KeyboardIntent.Show,
                isScrollInProgress = true,
                activeField = TrackerPageFocusTarget.Label,
                lastAppliedKeyboardIntent = KeyboardIntent.Show,
            ),
        )
    }

    @Test
    fun `missing supplemental field falls back to value`() {
        assertEquals(
            PagerInputAction.FocusDestination(
                page = 1,
                field = TrackerPageFocusTarget.Value,
                showKeyboard = false,
            ),
            planAction(
                destinationKeyboardIntent = KeyboardIntent.Show,
                isScrollInProgress = true,
                availableTargets = setOf(TrackerPageFocusTarget.Value),
                activeField = TrackerPageFocusTarget.Note,
                lastAppliedKeyboardIntent = KeyboardIntent.Show,
            ),
        )
    }

    @Test
    fun `hide starts during scroll but preserves focus`() {
        assertEquals(
            PagerInputAction.Hide(
                hideKeyboard = true,
                clearFocus = false,
            ),
            planAction(
                destinationKeyboardIntent = KeyboardIntent.Hide,
                isScrollInProgress = true,
                lastAppliedKeyboardIntent = KeyboardIntent.Show,
            ),
        )
    }

    @Test
    fun `hide clears focus after destination settles`() {
        assertEquals(
            PagerInputAction.Hide(
                hideKeyboard = false,
                clearFocus = true,
            ),
            planAction(
                destinationKeyboardIntent = KeyboardIntent.Hide,
                isScrollInProgress = false,
                lastAppliedKeyboardIntent = KeyboardIntent.Hide,
            ),
        )
    }

    private fun planAction(
        destinationKeyboardIntent: KeyboardIntent,
        isScrollInProgress: Boolean = false,
        availableTargets: Set<TrackerPageFocusTarget>? =
            setOf(
                TrackerPageFocusTarget.Value,
                TrackerPageFocusTarget.Label,
                TrackerPageFocusTarget.Note,
            ),
        activeField: TrackerPageFocusTarget = TrackerPageFocusTarget.Value,
        lastAppliedKeyboardIntent: KeyboardIntent? = null,
    ) = planPagerInputAction(
        destinationPage = 1,
        destinationKeyboardIntent = destinationKeyboardIntent,
        isScrollInProgress = isScrollInProgress,
        availableTargets = availableTargets,
        activeField = activeField,
        lastAppliedKeyboardIntent = lastAppliedKeyboardIntent,
        focusOwnerPage = 0,
    )
}
