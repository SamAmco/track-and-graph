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

import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.InputSpacingXLarge
import com.samco.trackandgraph.ui.compose.ui.TextBody1
import com.samco.trackandgraph.ui.compose.ui.TextLink
import com.samco.trackandgraph.ui.compose.ui.TextSubtitle2
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing

@Composable
fun AboutPageView(
    versionText: String,
    onRepoLinkClicked: () -> Unit = {}
) = Column(
    modifier = Modifier
        .padding(dialogInputSpacing)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
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
        R.string.moshi
    )

    for (library in libraries) {
        TextBody1(
            text = stringResource(id = library),
            maxLines = 1
        )
    }
}

@Composable
fun LibraryVersions() = Column {
    val versions = listOf(
        R.string.apache_2_0,
        R.string.apache_2_0,
        R.string.apache_2_0,
        R.string.apache_2_0,
        R.string.apache_2_0,
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