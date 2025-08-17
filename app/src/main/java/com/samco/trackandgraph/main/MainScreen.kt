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

import android.support.v4.media.session.MediaSessionCompat.Token.fromToken
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.fragment.findNavController
import com.samco.trackandgraph.R
import com.samco.trackandgraph.main.AppBarViewModel.NavBarConfig
import com.samco.trackandgraph.main.AppBarViewModel.NavButtonStyle
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.Int

@Composable
fun MainScreen(
    activity: FragmentActivity,
    onNavigateToBrowser: (DrawerMenuBrowserLocation) -> Unit,
    currentTheme: State<ThemeSelection>,
    onThemeSelected: (ThemeSelection) -> Unit,
    currentDateFormat: State<Int>,
    onDateFormatSelected: (Int) -> Unit,
) = TnGComposeTheme {

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val appBarViewModel = viewModel<AppBarViewModel>(
        viewModelStoreOwner = activity
    )

    var navController by remember { mutableStateOf<NavController?>(null) }
    val currentBackStackEntry = navController?.currentBackStackEntryAsState()?.value
    val isAtNavRoot = remember { mutableStateOf(false) }
    LaunchedEffect(navController, currentBackStackEntry) {
        isAtNavRoot.value = navController?.previousBackStackEntry == null
    }
    BackHandler(!isAtNavRoot.value) { navController?.popBackStack() }

    MainView(
        navBarConfig = appBarViewModel.navBarConfigState,
        drawerState = drawerState,
        isAtNavRoot = isAtNavRoot,
        onUpClicked = { navController?.popBackStack() },
        onAppBarAction = appBarViewModel::onAction,
        navController = navController,
        onNavigateToBrowser = onNavigateToBrowser,
        currentTheme = currentTheme,
        onThemeSelected = onThemeSelected,
        currentDateFormat = currentDateFormat,
        onDateFormatSelected = onDateFormatSelected,
    ) { contentPadding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            factory = { context ->
                View.inflate(context, R.layout.nav_host_fragment, null)
            },
            update = {
                if (navController == null) {
                    navController = activity
                        .supportFragmentManager
                        .findFragmentById(R.id.nav_fragment)
                        ?.findNavController()
                    if (navController == null) {
                        Timber.e("Could not find NavController")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainView(
    navBarConfig: State<NavBarConfig>,
    drawerState: DrawerState,
    isAtNavRoot: State<Boolean>,
    onUpClicked: () -> Unit,
    onAppBarAction: (AppBarViewModel.Action) -> Unit,
    navController: NavController?,
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

    val scope = rememberCoroutineScope()

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
                    onNavigateFromMenu = {
                        scope.launch { drawerState.close() }
                        // I don't love this, it assumes we're already at the root
                        // (which should always be true). But it works for now.
                        // Another improvement to make when transitioning to compose navigation
                        val root = navController?.currentDestination?.id
                        val navOptions = NavOptions.Builder()
                            .setPopUpTo(root ?: R.id.groupFragment, true)
                            .build()
                        navController?.navigate(it, null, navOptions)
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
            contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
            topBar = {
                if (navBarConfig.value.visible) {
                    AppBar(
                        scope = scope,
                        navBarConfig = navBarConfig,
                        isAtNavRoot = isAtNavRoot,
                        onUpClicked = onUpClicked,
                        drawerState = drawerState,
                        onAction = onAppBarAction
                    )
                }
            },
            content = content,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
fun AppBar(
    scope: CoroutineScope,
    navBarConfig: State<NavBarConfig>,
    isAtNavRoot: State<Boolean>,
    onUpClicked: () -> Unit,
    drawerState: DrawerState,
    onAction: (AppBarViewModel.Action) -> Unit,
) = TopAppBar(
    windowInsets = WindowInsets.statusBarsIgnoringVisibility,
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.tngColors.toolbarBackgroundColor
    ),
    navigationIcon = {
        NavigationIcon(
            navButtonStyle = if (isAtNavRoot.value) NavButtonStyle.MENU else NavButtonStyle.UP,
            onClick = {
                if (!isAtNavRoot.value) onUpClicked()
                else scope.launch { drawerState.open() }
            }
        )
    },
    actions = {
        AppBarActions(
            actions = navBarConfig.value.actions,
            onAction = onAction,
        )
        AppBarOverflowActions(
            collapsedActions = navBarConfig.value.collapsedActions,
            onAction = onAction,
        )
    },
    expandedHeight = TopAppBarDefaults.TopAppBarExpandedHeight * LocalConfiguration.current.fontScale,
    title = {
        Column(verticalArrangement = Arrangement.Center) {
            navBarConfig.value.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            navBarConfig.value.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    },
)

@Composable
private fun AppBarActions(
    actions: List<AppBarViewModel.Action>,
    onAction: (AppBarViewModel.Action) -> Unit,
) = Row {
    for (action in actions) {
        IconButton(onClick = { onAction(action) }) {
            Icon(
                painter = painterResource(action.iconId),
                contentDescription = stringResource(action.titleId),
                tint = MaterialTheme.tngColors.onSurface,
            )
        }
    }
}

@Composable
private fun AppBarOverflowActions(
    collapsedActions: AppBarViewModel.CollapsedActions?,
    onAction: (AppBarViewModel.Action) -> Unit,
) {
    if (collapsedActions == null) return

    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = collapsedActions.overflowIconId),
                contentDescription = null,
                tint = MaterialTheme.tngColors.onSurface,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            for (action in collapsedActions.actions) {
                DropdownMenuItem(
                    text = { Text(stringResource(action.titleId)) },
                    onClick = {
                        onAction(action)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun NavigationIcon(
    navButtonStyle: NavButtonStyle,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        when (navButtonStyle) {
            NavButtonStyle.UP -> Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = null,
            )

            NavButtonStyle.MENU -> Icon(
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
        MainView(
            navBarConfig = remember {
                mutableStateOf(
                    NavBarConfig(
                        title = "Track & Graph",
                        subtitle = null,
                    )
                )
            },
            drawerState = rememberDrawerState(
                initialValue = DrawerValue.Closed
            ),
            onAppBarAction = {},
            navController = null,
            isAtNavRoot = remember { mutableStateOf(true) },
            onUpClicked = {},
            onNavigateToBrowser = {},
            currentTheme = remember { mutableStateOf(ThemeSelection.SYSTEM) },
            onThemeSelected = {},
            currentDateFormat = remember { mutableIntStateOf(0) },
            onDateFormatSelected = {},
        ) { _ -> }
    }
}