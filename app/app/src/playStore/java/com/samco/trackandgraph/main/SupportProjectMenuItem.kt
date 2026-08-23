package com.samco.trackandgraph.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.support.SupportDeveloperDialog

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun SupportProjectMenuItem(
    onNavigateToBrowser: (DrawerMenuBrowserLocation) -> Unit,
) {
    var showSupportDialog by remember { mutableStateOf(false) }

    MenuItem(
        title = stringResource(R.string.support_developer),
        icon = painterResource(R.drawable.support_developer_icon),
        onClick = { showSupportDialog = true },
    )

    if (showSupportDialog) {
        SupportDeveloperDialog(onDismissRequest = { showSupportDialog = false })
    }
}
