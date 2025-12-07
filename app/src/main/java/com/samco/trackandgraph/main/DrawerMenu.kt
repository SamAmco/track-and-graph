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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.R
import com.samco.trackandgraph.aboutpage.AboutNavKey
import com.samco.trackandgraph.backupandrestore.BackupAndRestoreNavKey
import com.samco.trackandgraph.group.GroupNavKey
import com.samco.trackandgraph.notes.NotesNavKey
import com.samco.trackandgraph.reminders.ui.RemindersNavKey
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.Divider
import com.samco.trackandgraph.ui.compose.ui.GradientDivider
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge

enum class DrawerMenuBrowserLocation {
    FAQ,
    RATE_APP,
    SUPPORT_PROJECT,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuDrawerContent(
    onNavigate: (NavKey) -> Unit = {},
    onNavigateToBrowser: (DrawerMenuBrowserLocation) -> Unit = {},
    currentTheme: State<ThemeSelection>,
    onThemeSelected: (ThemeSelection) -> Unit = {},
    currentDateFormat: State<Int>,
    onDateFormatSelected: (Int) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility)
    ) {
        Text(
            modifier = Modifier.padding(inputSpacingLarge),
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )

        GradientDivider(
            modifier = Modifier.padding(vertical = inputSpacingLarge / 2)
        )

        MenuItem(
            title = stringResource(R.string.home),
            icon = painterResource(R.drawable.home_menu_icon)
        ) { onNavigate(GroupNavKey()) }

        MenuItem(
            modifier = Modifier.testTag("remindersMenuItem"),
            title = stringResource(R.string.reminders),
            icon = painterResource(R.drawable.reminders_icon)
        ) { onNavigate(RemindersNavKey) }

        MenuItem(
            title = stringResource(R.string.notes),
            icon = painterResource(R.drawable.edit_icon)
        ) { onNavigate(NotesNavKey) }

        MenuItem(
            title = stringResource(R.string.backup_and_restore),
            icon = painterResource(R.drawable.backup_restore_icon)
        ) { onNavigate(BackupAndRestoreNavKey) }

        Divider(
            modifier = Modifier.padding(vertical = inputSpacingLarge / 2)
        )

        MenuItem(
            title = stringResource(R.string.faq),
            icon = painterResource(R.drawable.faq_icon)
        ) { onNavigateToBrowser(DrawerMenuBrowserLocation.FAQ) }

        MenuItem(
            title = stringResource(R.string.rate_the_app),
            icon = painterResource(R.drawable.rate_icon)
        ) { onNavigateToBrowser(DrawerMenuBrowserLocation.RATE_APP) }

        MenuItem(
            title = stringResource(R.string.release_notes_support_development),
            icon = painterResource(R.drawable.bmc_logo)
        ) { onNavigateToBrowser(DrawerMenuBrowserLocation.SUPPORT_PROJECT) }

        MenuItem(
            title = stringResource(R.string.about),
            icon = painterResource(R.drawable.about_icon)
        ) { onNavigate(AboutNavKey) }

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
}

@Composable
private fun ThemeMenuSpinner(
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
        style = MaterialTheme.typography.titleSmall,
    )

    DialogInputSpacing()

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
    ) { onThemeSelected(it) }
}

@Composable
private fun DateFormatSpinner(
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
        stringResource(R.string.date_format_colon),
        style = MaterialTheme.typography.titleSmall,
    )

    DialogInputSpacing()

    val formatNames = stringArrayResource(R.array.date_formats)

    TextMapSpinner(
        strings = formatNames.indices.zip(formatNames).toMap(),
        selectedItem = currentFormat.value,
        textAlign = TextAlign.End,
    ) { onFormatSelected(it) }
}

@Composable
private fun MenuItem(
    modifier: Modifier = Modifier,
    icon: Painter,
    title: String,
    onClick: () -> Unit,
) = Row(
    modifier = modifier
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
        style = MaterialTheme.typography.titleSmall,
    )
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun MenuDrawerContentPreview() = TnGComposeTheme {
    MenuDrawerContent(
        currentTheme = remember { mutableStateOf(ThemeSelection.SYSTEM) },
        currentDateFormat = remember { mutableIntStateOf(0) },
    )
}