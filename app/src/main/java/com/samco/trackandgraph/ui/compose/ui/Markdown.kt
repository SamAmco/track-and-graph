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
package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Composable
fun TnGMarkdown(
    modifier: Modifier = Modifier,
    content: String,
) {
    Markdown(
        modifier = modifier,
        content = content,
        imageTransformer = Coil3ImageTransformerImpl,
        colors = markdownColor(
            text = MaterialTheme.tngColors.onSurface,
            codeBackground = MaterialTheme.tngColors.selectorButtonColor,
            inlineCodeBackground = MaterialTheme.tngColors.selectorButtonColor,
            dividerColor = MaterialTheme.colorScheme.outline,
            tableBackground = MaterialTheme.tngColors.selectorButtonColor,
        ),
        typography = markdownTypography(
            h1 = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
            h2 = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
            h3 = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
            h4 = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            h5 = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
            h6 = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
            text = MaterialTheme.typography.bodyMedium,
            code = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            quote = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic
            ),
            paragraph = MaterialTheme.typography.bodyMedium,
            ordered = MaterialTheme.typography.bodyMedium,
            bullet = MaterialTheme.typography.bodyMedium,
            list = MaterialTheme.typography.bodyMedium,
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun TnGMarkdownPreview() {
    TnGComposeTheme {
        TnGMarkdown(
            content = """
                # Track & Graph Markdown Preview
                
                This is a **bold** text and this is *italic* text.
                
                ## Features
                
                - Bullet point 1
                - Bullet point 2
                - Bullet point 3
                
                ### Code Examples
                
                Here's some `inline code` and a code block:
                
                ```kotlin
                fun hello() {
                    println("Hello, World!")
                }
                ```
                
                ### Cute Cat Image
                
                ![Cute Cat](https://cataas.com/cat/cute?width=300&height=200)
                
                > This is a blockquote with some important information.
                
                ### Links
                
                Check out [Track & Graph on GitHub](https://github.com/SamAmco/track-and-graph)!
                
                | Feature | Status |
                |---------|--------|
                | Markdown | ✅ |
                | Images | ✅ |
                | Tables | ✅ |
            """.trimIndent()
        )
    }
}