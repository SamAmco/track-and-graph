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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.rememberNavBackStack
import com.jakewharton.threetenabp.AndroidThreeTen
import com.samco.trackandgraph.R
import com.samco.trackandgraph.group.GroupNavKey
import com.samco.trackandgraph.main.AppBar
import com.samco.trackandgraph.reminders.ui.RemindersNavKey
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.TimeZone

internal val PlayStoreStatusBarHeight = 38.dp
internal val PlayStoreNavigationBarHeight = 12.dp
internal val PlayStoreStatusBarTimeStartPadding = 56.dp
internal const val PlayStoreStatusBarLightAlpha = 0.65f
internal const val PlayStoreStatusBarDarkAlpha = 0.85f
internal const val PlayStoreNavigationBarLightAlpha = 0.35f
internal const val PlayStoreNavigationBarDarkAlpha = 0.55f

@Preview(name = "Play Store Status Bar", widthDp = 411, heightDp = 38)
@Composable
private fun PlayStoreStatusBarPreview() {
    TnGComposeTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            PlayStoreStatusBar()
        }
    }
}

@Composable
internal fun PlayStorePreviewEnvironment(content: @Composable () -> Unit) {
    val context = LocalContext.current
    remember(context) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        AndroidThreeTen.init(context)
        true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            PlayStoreStatusBar()
            PlayStoreNavigationBar(
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PlayStoreNavigationBar(
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val statusBarAlpha =
        if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
            PlayStoreNavigationBarDarkAlpha
        } else {
            PlayStoreNavigationBarLightAlpha
        }
    Box(
        modifier = modifier
            .height(PlayStoreNavigationBarHeight),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .height(3.dp)
                .alpha(statusBarAlpha),
            color = contentColor,
            shape = CircleShape,
        ) {}
    }
}


@Composable
private fun PlayStoreStatusBar() {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val statusBarAlpha =
        if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
            PlayStoreStatusBarDarkAlpha
        } else {
            PlayStoreStatusBarLightAlpha
        }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(statusBarAlpha)
            .height(PlayStoreStatusBarHeight)
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(top = 18.dp)
                .padding(start = PlayStoreStatusBarTimeStartPadding),
            text = "4:20",
            color = contentColor,
            fontSize = 18.sp,
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 3.dp, top = 16.dp, end = 24.dp)
                .height(PlayStoreStatusBarHeight),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.playstore_status_wifi),
                contentDescription = null,
                tint = contentColor,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.playstore_status_cell),
                contentDescription = null,
                tint = contentColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                modifier = Modifier
                    .scale(1.4f)
                    .padding(top = 1.dp),
                painter = painterResource(R.drawable.playstore_status_battery),
                contentDescription = null,
                tint = contentColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayStoreGroupFrame(
    title: String,
    backNavigationAction: Boolean = true,
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val backStack = rememberNavBackStack(GroupNavKey(), GroupNavKey(groupName = title))
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = remember { CoroutineScope(Dispatchers.Main.immediate) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBar(
            scope = scope,
            config = AppBarConfig(
                title = title,
                backNavigationAction = backNavigationAction,
                statusBarHeightOverride = PlayStoreStatusBarHeight,
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
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayStoreRemindersFrame(
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val backStack = rememberNavBackStack(RemindersNavKey)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = remember { CoroutineScope(Dispatchers.Main.immediate) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBar(
            scope = scope,
            config = AppBarConfig(
                title = stringResource(R.string.reminders),
                statusBarHeightOverride = PlayStoreStatusBarHeight,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(painterResource(R.drawable.add_icon), stringResource(R.string.add))
                    }
                }
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
