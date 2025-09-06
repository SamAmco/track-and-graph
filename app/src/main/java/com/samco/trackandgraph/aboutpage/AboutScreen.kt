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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.aboutpage

import android.content.Context
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.R
import com.samco.trackandgraph.remoteconfig.UrlNavigator
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.InputSpacingXLarge
import com.samco.trackandgraph.ui.compose.ui.TextBody1
import com.samco.trackandgraph.ui.compose.ui.TextLink
import com.samco.trackandgraph.ui.compose.ui.TextSubtitle2
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
data object AboutNavKey : NavKey

@Composable
fun AboutScreen(
    navArgs: AboutNavKey,
    urlNavigator: UrlNavigator
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    TopAppBarContent(navArgs)

    val versionText = remember(context) { getVersionText(context) }

    AboutPageView(
        versionText = versionText,
        onRepoLinkClicked = {
            scope.launch {
                urlNavigator.navigateTo(context, UrlNavigator.Location.GITHUB)
            }
        }
    )
}

@Composable
private fun TopAppBarContent(navArgs: AboutNavKey) {
    val topBarController = LocalTopBarController.current
    val title = stringResource(R.string.about)

    topBarController.Set(
        navArgs,
        AppBarConfig(
            title = title,
            appBarPinned = true,
        )
    )
}

private fun getVersionText(context: Context): String {
    return try {
        val pInfo = context.packageManager
            .getPackageInfo(context.packageName, 0)
        "v${pInfo.versionName}"
    } catch (e: Exception) {
        Timber.e("Could not get package version name: ${e.message}")
        ""
    }
}

@Composable
fun AboutPageView(
    versionText: String,
    onRepoLinkClicked: () -> Unit = {}
) = Column(
    modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(
            WindowInsets.safeDrawing
                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                .asPaddingValues()
        )
        .fillMaxSize()
        .then(Modifier.padding(dialogInputSpacing)),
) {
    VersionText(versionText)
    InputSpacingXLarge()
    TnGIcon(modifier = Modifier.align(Alignment.CenterHorizontally))
    InputSpacingXLarge()
    Headline()
    HalfDialogInputSpacing()
    RepoLink(onLinkClicked = onRepoLinkClicked)
    InputSpacingXLarge()
    AboutLibraries()
    InputSpacingLarge()
    LibraryTable()
}

@Composable
fun LibraryTable() = Row {
    Spacer(modifier = Modifier.weight(1f))
    LibraryNames()
    Spacer(modifier = Modifier.weight(1f))
    LibraryVersions()
    Spacer(modifier = Modifier.weight(1f))
}

@Composable
fun LibraryNames() = Column {
    val libraries = listOf(
        R.string.androidplot,
        R.string.apache_commons_csv,
        R.string.three_ten_android_backport,
        R.string.timber,
        R.string.moshi,
        R.string.luak,
    )

    for (library in libraries) {
        TextBody1(
            text = stringResource(id = library),
            maxLines = 1
        )
    }
}

@Composable
fun LibraryVersions() = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    val versions = listOf(
        R.string.apache_2_0,
        R.string.apache_2_0,
        R.string.apache_2_0,
        R.string.apache_2_0,
        R.string.apache_2_0,
        R.string.mit,
    )

    for (version in versions) {
        TextBody1(
            text = stringResource(id = version),
            maxLines = 1
        )
    }
}

@Composable
fun AboutLibraries() = TextBody1(
    modifier = Modifier.fillMaxWidth(),
    text = stringResource(id = R.string.about_libraries_message),
    textAlign = TextAlign.Center,
)

@Composable
fun RepoLink(
    onLinkClicked: () -> Unit = {}
) = TextLink(
    modifier = Modifier.fillMaxWidth(),
    text = stringResource(id = R.string.github_link_friendly),
    textAlign = TextAlign.Center,
    onClick = onLinkClicked
)

@Composable
fun Headline() = TextSubtitle2(
    modifier = Modifier.fillMaxWidth(),
    text = stringResource(id = R.string.about_description),
    textAlign = TextAlign.Center
)

@Composable
private fun TnGIcon(modifier: Modifier) = Card(
    modifier = modifier.size(80.dp),
    shape = MaterialTheme.shapes.medium,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ImageView(ctx).apply {
                setImageResource(R.drawable.app_icon)
            }
        }
    )
}

@Composable
private fun VersionText(versionText: String) = Box(
    modifier = Modifier.fillMaxWidth()
) {
    TextBody1(
        modifier = Modifier.align(Alignment.CenterEnd),
        versionText
    )
}

@Preview(showBackground = true)
@Composable
fun AboutPageViewPreview() {
    TnGComposeTheme {
        AboutPageView("v1.0.0")
    }
}