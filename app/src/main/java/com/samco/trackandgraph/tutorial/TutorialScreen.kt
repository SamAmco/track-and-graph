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
package com.samco.trackandgraph.tutorial

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme

@Composable
fun TutorialScreen(
    modifier: Modifier = Modifier,
    onGotItClicked: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    HorizontalPager(
        modifier = modifier.fillMaxSize(),
        state = pagerState,
    ) { page ->

        when (page) {
            0 -> TutorialPage1()
            1 -> TutorialPage2()
            2 -> TutorialPage3(onGotItClicked)
        }
    }
}

@Composable
private fun TutorialPage1() = TnGComposeTheme {
    TutorialPage(
        text = stringResource(id = R.string.tutorial_text_1),
        imageResId = R.drawable.tutorial_image_1,
        background = {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds // or .Fit depending on how your background should behave
            )
        },
        showGotIt = false
    )
}

@Composable
private fun TutorialPage2() = TnGComposeTheme {
    TutorialPage(
        text = stringResource(id = R.string.tutorial_text_2),
        imageResId = R.drawable.tutorial_image_2,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.secondary)
            )
        },
        showGotIt = false
    )
}

@Composable
private fun TutorialPage3(
    onGotItClicked: () -> Unit = {},
) = TnGComposeTheme {
    TutorialPage(
        text = stringResource(id = R.string.tutorial_text_3),
        imageResId = R.drawable.tutorial_image_3,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.primary)
            )
        },
        onGotItClicked = onGotItClicked,
        showGotIt = true
    )
}

@Composable
private fun TutorialPage(
    text: String,
    imageResId: Int,
    background: @Composable () -> Unit,
    showGotIt: Boolean = false,
    onGotItClicked: () -> Unit = {},
) = Box(modifier = Modifier.fillMaxSize()) {
    background()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, end = 40.dp, top = 80.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                text = text,
                color = MaterialTheme.colors.onSecondary,
                style = MaterialTheme.typography.h4,
                textAlign = TextAlign.Center,
            )

            Image(
                painter = painterResource(id = imageResId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        if (showGotIt) {
            Text(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 30.dp, bottom = 20.dp)
                    .clickable(
                        indication = ripple(bounded = false),
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onGotItClicked() },
                text = stringResource(id = R.string.got_it),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSecondary,
                style = MaterialTheme.typography.h5,
            )
        }
    }
}


@Composable
@Preview
private fun PreviewTutorialPage1() {
    TutorialPage1()
}

@Composable
@Preview
private fun PreviewTutorialPage2() {
    TutorialPage2()
}

@Composable
@Preview
private fun PreviewTutorialPage3() {
    TutorialPage3()
}
