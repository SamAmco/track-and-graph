package com.samco.trackandgraph.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R

@Composable
internal fun SupportProjectMenuItem(
    onNavigateToBrowser: (DrawerMenuBrowserLocation) -> Unit,
) {
    MenuItem(
        title = stringResource(R.string.release_notes_support_development),
        icon = painterResource(R.drawable.bmc_logo)
    ) { onNavigateToBrowser(DrawerMenuBrowserLocation.SUPPORT_PROJECT) }
}
