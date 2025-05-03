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

import android.os.Build
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.samco.trackandgraph.ui.compose.ui.Divider
import com.samco.trackandgraph.ui.compose.ui.GradientDivider
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.Int

@Composable
fun MainScreen(
    activity: FragmentActivity,
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
    var isAtNavRoot = remember { mutableStateOf(false) }
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

    ModalDrawer(
        drawerState = drawerState,
        drawerShape = CutCornerShape(0.dp),
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            MenuDrawerContent(
                currentTheme = currentTheme,
                onThemeSelected = onThemeSelected,
                onNavigateFromMenu = {
                    scope.launch { drawerState.close() }
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.groupFragment, false)
                        .build()
                    navController?.navigate(it, null, navOptions)
                },
                currentDateFormat = currentDateFormat,
                onDateFormatSelected = onDateFormatSelected,
            )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuDrawerContent(
    currentTheme: State<ThemeSelection>,
    onNavigateFromMenu: (Int) -> Unit,
    onThemeSelected: (ThemeSelection) -> Unit,
    currentDateFormat: State<Int>,
    onDateFormatSelected: (Int) -> Unit
) = Column(
    modifier = Modifier
        .windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility)
) {
    Text(
        modifier = Modifier.padding(inputSpacingLarge),
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.h6,
    )

    GradientDivider(
        modifier = Modifier.padding(vertical = inputSpacingLarge / 2)
    )

    MenuItem(
        title = stringResource(R.string.home),
        icon = painterResource(R.drawable.home_menu_icon)
    ) { onNavigateFromMenu(R.id.groupFragment) }

    MenuItem(
        title = stringResource(R.string.reminders),
        icon = painterResource(R.drawable.reminders_icon)
    ) { onNavigateFromMenu(R.id.remindersFragment) }

    MenuItem(
        title = stringResource(R.string.notes),
        icon = painterResource(R.drawable.edit_icon)
    ) { onNavigateFromMenu(R.id.notesFragment) }

    MenuItem(
        title = stringResource(R.string.backup_and_restore),
        icon = painterResource(R.drawable.backup_restore_icon)
    ) { onNavigateFromMenu(R.id.backupAndRestoreFragment) }

    Divider(
        modifier = Modifier.padding(vertical = inputSpacingLarge / 2)
    )

    MenuItem(
        title = stringResource(R.string.faq),
        icon = painterResource(R.drawable.faq_icon)
    ) { onNavigateFromMenu(R.id.FAQFragment) }

    MenuItem(
        title = stringResource(R.string.rate_the_app),
        icon = painterResource(R.drawable.rate_icon)
    ) { onNavigateFromMenu(R.id.rateAppRedirectFragment) }

    MenuItem(
        title = stringResource(R.string.about),
        icon = painterResource(R.drawable.about_icon)
    ) { onNavigateFromMenu(R.id.aboutPageFragment) }

    Divider(
        modifier = Modifier.padding(top = inputSpacingLarge)
    )

    ThemeMenuSpinner(
        currentTheme = currentTheme,
        onThemeSelected = onThemeSelected
    )

    DateFormatSpinner(
        currentFormat = currentDateFormat,
        onFormatSelected = onDateFormatSelected
    )
}

@Composable
fun ThemeMenuSpinner(
    currentTheme: State<ThemeSelection>,
    onThemeSelected: (ThemeSelection) -> Unit
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(
            start = inputSpacingLarge,
            top = dialogInputSpacing,
            end = inputSpacingLarge
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    Text(
        stringResource(R.string.theme_colon),
        style = MaterialTheme.typography.h6,
    )

    val themeValues = arrayOf(
        ThemeSelection.SYSTEM,
        ThemeSelection.LIGHT,
        ThemeSelection.DARK,
    )

    val stringsResId = remember {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> R.array.theme_names_Q
            else -> R.array.theme_names_pre_Q
        }
    }
    val stringArray = stringArrayResource(stringsResId)

    TextMapSpinner(
        strings = themeValues.zip(stringArray).toMap(),
        selectedItem = currentTheme.value,
        textAlign = TextAlign.End,
        textStyle = MaterialTheme.typography.subtitle1,
        paddingValues = PaddingValues(all = 0.dp),
    ) { onThemeSelected(it) }
}

@Composable
fun DateFormatSpinner(
    currentFormat: State<Int>,
    onFormatSelected: (Int) -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(
            start = inputSpacingLarge,
            top = dialogInputSpacing,
            end = inputSpacingLarge
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    Text(
        stringResource(R.string.theme_colon),
        style = MaterialTheme.typography.h6,
    )

    val formatNames = stringArrayResource(R.array.date_formats)

    TextMapSpinner(
        strings = formatNames.indices.zip(formatNames).toMap(),
        selectedItem = currentFormat.value,
        textAlign = TextAlign.End,
        textStyle = MaterialTheme.typography.subtitle1,
        paddingValues = PaddingValues(all = 0.dp),
    ) { onFormatSelected(it) }
}

@Composable
fun MenuItem(
    icon: Painter,
    title: String,
    onClick: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(
            horizontal = inputSpacingLarge,
            vertical = inputSpacingLarge / 2f,
        ),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(
        modifier = Modifier.size(22.dp),
        painter = icon,
        contentDescription = title,
        tint = MaterialTheme.tngColors.onSurface,
    )
    InputSpacingLarge()
    Text(
        text = title,
        style = MaterialTheme.typography.h6,
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AppBar(
    scope: CoroutineScope,
    navBarConfig: State<NavBarConfig>,
    isAtNavRoot: State<Boolean>,
    onUpClicked: () -> Unit,
    drawerState: DrawerState,
    onAction: (AppBarViewModel.Action) -> Unit,
) = TopAppBar(
    windowInsets = WindowInsets.statusBarsIgnoringVisibility,
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
    title = {
        Column(verticalArrangement = Arrangement.Center) {
            navBarConfig.value.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.h6,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // TODO this can be clipped with very large font sizes. We should be able to fix
            // it in material3: https://issuetracker.google.com/issues/308540676
            navBarConfig.value.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    },
    backgroundColor = MaterialTheme.tngColors.toolbarBackgroundColor,
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
                    onClick = {
                        onAction(action)
                        expanded = false
                    }
                ) {
                    Text(
                        text = stringResource(action.titleId),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
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
fun MainViewPreview() {
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
                initialValue = DrawerValue.Open
            ),
            onAppBarAction = {},
            navController = null,
            isAtNavRoot = remember { mutableStateOf(true) },
            onUpClicked = {},
            currentTheme = remember { mutableStateOf(ThemeSelection.SYSTEM) },
            onThemeSelected = {},
            currentDateFormat = remember { mutableStateOf(0) },
            onDateFormatSelected = {},
        ) { _ -> }
    }
}