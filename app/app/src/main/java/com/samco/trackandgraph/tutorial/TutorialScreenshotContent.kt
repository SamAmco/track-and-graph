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
package com.samco.trackandgraph.tutorial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.rememberNavBackStack
import com.jakewharton.threetenabp.AndroidThreeTen
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.group.CalculatedGraphViewData
import com.samco.trackandgraph.group.FunctionClickListeners
import com.samco.trackandgraph.group.GraphStatClickListeners
import com.samco.trackandgraph.group.GroupChild
import com.samco.trackandgraph.group.GroupClickListeners
import com.samco.trackandgraph.group.GroupNavKey
import com.samco.trackandgraph.group.GroupScreenView
import com.samco.trackandgraph.group.TrackerClickListeners
import com.samco.trackandgraph.main.AppBar
import com.samco.trackandgraph.playstore.PREVIEW_END_TIME
import com.samco.trackandgraph.playstore.PreviewLine
import com.samco.trackandgraph.playstore.PreviewSinTransform
import com.samco.trackandgraph.playstore.createPreviewWaveData
import com.samco.trackandgraph.playstore.lineGraphViewData
import com.samco.trackandgraph.playstore.sampledMovingAverage
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.threeten.bp.Duration
import java.util.TimeZone

private const val TUTORIAL_SCREENSHOT_DEVICE = "spec:width=1080px,height=1920px,dpi=420"

@Preview(name = "Tutorial Screenshot 1", device = TUTORIAL_SCREENSHOT_DEVICE)
@Composable
internal fun TutorialScreenshot1Preview() {
    TutorialScreenshot01Content()
}

@Preview(name = "Tutorial Screenshot 2", device = TUTORIAL_SCREENSHOT_DEVICE)
@Composable
internal fun TutorialScreenshot2Preview() {
    TutorialScreenshot02Content()
}

@Preview(name = "Tutorial Screenshot 3", device = TUTORIAL_SCREENSHOT_DEVICE)
@Composable
internal fun TutorialScreenshot3Preview() {
    TutorialScreenshot03Content()
}

@Composable
internal fun TutorialScreenshot01Content() {
    TutorialScreenshotTheme {
        TutorialGroupFrame {
            TutorialGroupScreen(tutorialTrackerChildren())
        }
    }
}

@Composable
internal fun TutorialScreenshot02Content() {
    TutorialScreenshotTheme {
        TutorialGroupFrame {
            TutorialGroupScreen(tutorialTrackerChildren() + tutorialGraphChild())
        }
    }
}

@Composable
internal fun TutorialScreenshot03Content() {
    TutorialScreenshotTheme {
        TutorialGroupFrame {
            TutorialGroupScreen(
                tutorialTrackerChildren() +
                    tutorialGraphChild() +
                    tutorialHealthGroupChild()
            )
        }
    }
}

@Composable
private fun TutorialScreenshotTheme(content: @Composable () -> Unit) {
    TnGComposeTheme {
        val context = LocalContext.current
        remember(context) {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            AndroidThreeTen.init(context)
            true
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TutorialGroupFrame(content: @Composable () -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val backStack = rememberNavBackStack(GroupNavKey(), GroupNavKey(groupName = "Track & Graph"))
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = remember { CoroutineScope(Dispatchers.Main.immediate) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBar(
            scope = scope,
            config = AppBarConfig(
                title = "Track & Graph",
                backNavigationAction = true,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            painterResource(R.drawable.swap_vert_icon),
                            stringResource(R.string.import_export)
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            painterResource(R.drawable.search_icon),
                            stringResource(R.string.search)
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(painterResource(R.drawable.add_icon), stringResource(R.string.add))
                    }
                },
            ),
            drawerState = drawerState,
            backStack = backStack,
            scrollBehavior = scrollBehavior,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            content()
        }
    }
}

@Composable
private fun TutorialGroupScreen(children: List<GroupChild>) {
    GroupScreenView(
        lazyGridState = rememberLazyGridState(),
        isLoading = false,
        showEmptyText = false,
        showFab = false,
        showReleaseNotesButton = false,
        allChildren = children,
        trackerClickListeners = TrackerClickListeners(),
        graphStatClickListeners = GraphStatClickListeners(),
        groupClickListeners = GroupClickListeners(),
        functionClickListeners = FunctionClickListeners(),
    )
}

private fun tutorialTrackerChildren(): List<GroupChild> {
    val trackedAt = PREVIEW_END_TIME.minusHours(6)
    val trackerSpecs = listOf(
        TutorialTrackerSpec(
            name = "Relaxation",
            description = "",
        ),
        TutorialTrackerSpec(
            name = "Stress",
            description = "Track daily stress level (0-10)",
        ),
    )

    return trackerSpecs.mapIndexed { index, spec ->
        val id = index + 1L
        GroupChild.ChildTracker(
            groupItemId = id,
            id = id,
            displayTracker = DisplayTracker(
                id = id,
                featureId = 100L + id,
                name = spec.name,
                dataType = DataType.CONTINUOUS,
                hasDefaultValue = false,
                defaultValue = 0.0,
                defaultLabel = "",
                timestamp = trackedAt.minusHours(index.toLong()),
                description = spec.description,
                timerStartInstant = null,
                unique = true,
            )
        )
    }
}

private fun tutorialGraphChild(): GroupChild.ChildGraph {
    val relaxation = tutorialRelaxationPoints()
    val stress = tutorialStressPoints()

    return GroupChild.ChildGraph(
        groupItemId = 3L,
        id = 3L,
        graph = CalculatedGraphViewData(
            time = 0L,
            viewData = lineGraphViewData(
                id = 3L,
                name = "Relaxation Vs Stress (Monthly moving averages)",
                lines = listOf(
                    PreviewLine(
                        name = "Relaxation",
                        colorIndex = 7,
                        pointStyle = LineGraphPointStyle.NONE,
                        values = sampledMovingAverage(
                            points = relaxation,
                            windowDays = 30,
                            numberOfWeeks = 26,
                        )
                    ),
                    PreviewLine(
                        name = "Stress",
                        colorIndex = 1,
                        pointStyle = LineGraphPointStyle.NONE,
                        values = sampledMovingAverage(
                            points = stress,
                            windowDays = 30,
                            numberOfWeeks = 26,
                        )
                    ),
                ),
                yFrom = 0.0,
                yTo = 10.0,
            )
        )
    )
}

private fun tutorialHealthGroupChild() = GroupChild.ChildGroup(
    groupItemId = 4L,
    id = 4L,
    group = Group(
        id = 4L,
        name = "Health",
        colorIndex = 0,
        unique = true,
    )
)

private fun tutorialRelaxationPoints() = createPreviewWaveData(
    sinTransform = PreviewSinTransform(amplitude = 1.0, wavelength = 210.0, yOffset = -20.0),
    randomSeed = 123,
    randomOffsetScalar = 40.0,
    numDataPoints = 500,
    spacing = Duration.ofDays(1),
    spacingRandomisationHours = 6,
    endPoint = PREVIEW_END_TIME.minusHours(6),
)

private fun tutorialStressPoints() = createPreviewWaveData(
    sinTransform = PreviewSinTransform(
        amplitude = 1.0,
        wavelength = 210.0,
        yOffset = -20.0,
        xOffset = 100.0,
    ),
    randomSeed = 456,
    randomOffsetScalar = 40.0,
    numDataPoints = 500,
    spacing = Duration.ofDays(1),
    spacingRandomisationHours = 6,
    endPoint = PREVIEW_END_TIME,
)

private data class TutorialTrackerSpec(
    val name: String,
    val description: String,
)
