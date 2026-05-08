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
package com.samco.trackandgraph.playstore

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

private const val PLAY_STORE_DEVICE = "spec:width=1080px,height=2340px,dpi=420"

@PreviewTest
@Preview(name = "Play Store Screenshot 1", device = PLAY_STORE_DEVICE)
@Composable
fun PlayStoreScreenshot01() {
    PlayStoreDailyGroupScreenshotContent()
}

@PreviewTest
@Preview(name = "Play Store Screenshot 2", device = PLAY_STORE_DEVICE)
@Composable
fun PlayStoreScreenshot02() {
    PlayStoreExerciseScreenshotContent()
}

@PreviewTest
@Preview(name = "Play Store Screenshot 3", device = PLAY_STORE_DEVICE)
@Composable
fun PlayStoreScreenshot03() {
    PlayStoreDailyAddStressScreenshotContent()
}

@PreviewTest
@Preview(name = "Play Store Screenshot 4", device = PLAY_STORE_DEVICE)
@Composable
fun PlayStoreScreenshot04() {
    PlayStoreDailyDarkScreenshotContent()
}

@PreviewTest
@Preview(name = "Play Store Screenshot 5", device = PLAY_STORE_DEVICE)
@Composable
fun PlayStoreScreenshot05() {
    PlayStoreRestDayStatisticsScreenshotContent()
}

@PreviewTest
@Preview(name = "Play Store Screenshot 6", device = PLAY_STORE_DEVICE)
@Composable
fun PlayStoreScreenshot06() {
    PlayStoreGroupsListScreenshotContent()
}

@PreviewTest
@Preview(name = "Play Store Screenshot 7", device = PLAY_STORE_DEVICE)
@Composable
fun PlayStoreScreenshot07() {
    PlayStoreRemindersScreenshotContent()
}

@PreviewTest
@Preview(name = "Play Store Screenshot 8", device = PLAY_STORE_DEVICE)
@Composable
fun PlayStoreScreenshot08() {
    PlayStoreFunctionEditorScreenshotContent()
}
