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
package com.samco.trackandgraph.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import com.samco.trackandgraph.R
import com.samco.trackandgraph.group.GroupNavKey
import com.samco.trackandgraph.remoteconfig.UrlNavigator
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.ui.compose.appbar.TopBarController
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    urlNavigator: UrlNavigator,
    onNavigateToBrowser: (DrawerMenuBrowserLocation) -> Unit,
    currentTheme: State<ThemeSelection>,
    onThemeSelected: (ThemeSelection) -> Unit,
    currentDateFormat: State<Int>,
    onDateFormatSelected: (Int) -> Unit,
) = TnGComposeTheme {

    val backStack = rememberNavBackStack(GroupNavKey())
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val title = stringResource(R.string.app_name)
    val topBarController = remember(backStack, title) { TopBarController(backStack, AppBarConfig(title)) }

    CompositionLocalProvider(LocalTopBarController provides topBarController) {
        MainView(
            topBarController = topBarController,
            drawerState = drawerState,
            backStack = backStack,
            onNavigateToBrowser = onNavigateToBrowser,
            currentTheme = currentTheme,
            onThemeSelected = onThemeSelected,
            currentDateFormat = currentDateFormat,
            onDateFormatSelected = onDateFormatSelected,
        ) { contentPadding ->
            NavigationHost(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
                backStack = backStack,
                urlNavigator = urlNavigator,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainView(
    topBarController: TopBarController,
    drawerState: DrawerState,
    backStack: NavBackStack,
    onNavigateToBrowser: (DrawerMenuBrowserLocation) -> Unit,
    currentTheme: State<ThemeSelection>,
    onThemeSelected: (ThemeSelection) -> Unit,
    currentDateFormat: State<Int>,
    onDateFormatSelected: (Int) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    LaunchedEffect(drawerState, focusManager) {
        snapshotFlow { drawerState.isOpen }
            .filter { it }
            .collect { focusManager.clearFocus(force = true) }
    }

    val topBarConfig = topBarController.config
    val pinnedScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val enterAlwaysScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scrollBehavior = remember (topBarConfig.appBarPinned) {
        if (topBarConfig.appBarPinned) pinnedScrollBehavior
        else enterAlwaysScrollBehavior
    }
    val scope = rememberCoroutineScope()

    // Animate app bar back in when navigation occurs
    LaunchedEffect(backStack.size, topBarConfig, enterAlwaysScrollBehavior) {
        if (topBarConfig.visible && !topBarConfig.appBarPinned) {
            // Animate from collapsed (1.0) to expanded (0.0) to show the app bar sliding in
            val animatable = Animatable(enterAlwaysScrollBehavior.state.collapsedFraction)
            animatable.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            ) {
                // Update the scroll behavior's height offset to animate the app bar
                val heightOffsetLimit = enterAlwaysScrollBehavior.state.heightOffsetLimit
                enterAlwaysScrollBehavior.state.heightOffset = value * heightOffsetLimit
            }
        }
    }

    BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                MenuDrawerContent(
                    onNavigate = {
                        scope.launch { drawerState.close() }
                        backStack.clear()
                        backStack.add(GroupNavKey())
                        backStack.add(it)
                    },
                    onNavigateToBrowser = onNavigateToBrowser,
                    currentTheme = currentTheme,
                    onThemeSelected = onThemeSelected,
                    currentDateFormat = currentDateFormat,
                    onDateFormatSelected = onDateFormatSelected,
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier
                .let {
                    if (topBarConfig.nestedScrollConnection != null) {
                        it.nestedScroll(topBarConfig.nestedScrollConnection)
                    } else {
                        it
                    }
                }
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets(),
            topBar = {
                if (topBarConfig.visible) {
                    AppBar(
                        scope = scope,
                        config = topBarConfig,
                        drawerState = drawerState,
                        backStack = backStack,
                        scrollBehavior = scrollBehavior,
                    )
                }
            },
            content = content,
        )
    }
}

private enum class NavButtonStyle { UP, MENU }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    scope: CoroutineScope,
    config: AppBarConfig,
    drawerState: DrawerState,
    backStack: NavBackStack,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val density = LocalDensity.current
    val statusBarDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }

    // Make the app bar container taller by the status bar height so that area scrolls away too
    val totalHeight = TopAppBarDefaults.TopAppBarExpandedHeight + statusBarDp

    // We'll apply the status-bar inset INSIDE each slot
    val slotInset = Modifier
        .height(totalHeight)
        .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))

    TopAppBar(
        // No external insets -> nothing "pinned" behind status bar
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.tngColors.toolbarBackgroundColor,
            scrolledContainerColor = MaterialTheme.tngColors.toolbarBackgroundColor
        ),
        expandedHeight = totalHeight,
        scrollBehavior = scrollBehavior,

        navigationIcon = {
            Box(slotInset, contentAlignment = Alignment.CenterStart) {
                NavigationIcon(
                    navButtonStyle = if (config.backNavigationAction) NavButtonStyle.UP else NavButtonStyle.MENU,
                    onClick = {
                        if (config.backNavigationAction) backStack.removeLastOrNull()
                        else scope.launch { drawerState.open() }
                    }
                )
            }
        },

        title = {
            Column(slotInset, verticalArrangement = Arrangement.Center) {
                Text(
                    text = config.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                config.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },

        actions = {
            Row(slotInset, verticalAlignment = Alignment.CenterVertically, content = config.actions)
        }
    )
}

@Composable
private fun NavigationIcon(
    navButtonStyle: NavButtonStyle,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        when (navButtonStyle) {
            NavButtonStyle.UP -> Icon(
                modifier = Modifier.testTag("backButton"),
                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = null,
            )

            NavButtonStyle.MENU -> Icon(
                modifier = Modifier.testTag("burgerMenuButton"),
                imageVector = Icons.Filled.Menu,
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
private fun MainViewPreview() {
    TnGComposeTheme {
        val mockBackStack = rememberNavBackStack(GroupNavKey())
        MainView(
            topBarController = remember { TopBarController(mockBackStack, AppBarConfig("Track & Graph")) },
            drawerState = rememberDrawerState(
                initialValue = DrawerValue.Closed
            ),
            backStack = mockBackStack,
            onNavigateToBrowser = {},
            currentTheme = remember { mutableStateOf(ThemeSelection.SYSTEM) },
            onThemeSelected = {},
            currentDateFormat = remember { mutableIntStateOf(0) },
            onDateFormatSelected = {},
        ) { _ -> }
    }
}