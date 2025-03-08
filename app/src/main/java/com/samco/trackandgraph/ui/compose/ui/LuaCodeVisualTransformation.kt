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

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.samco.trackandgraph.R

@Composable
fun luaCodeVisualTransformation(): VisualTransformation {
    val highlights = getColorHighlightMap()
    return remember(highlights) {
        SyntaxHighlightingTransformation(patternHighlights = highlights)
    }
}

private class SyntaxHighlightingTransformation(
    private val patternHighlights: Map<String, Color>
) : VisualTransformation {
    private var lastText = ""
    private var lastResult = TransformedText(
        text = AnnotatedString(""),
        offsetMapping = OffsetMapping.Identity,
    )

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text == lastText) return lastResult

        val highlightedText = buildAnnotatedString {
            var lastIndex = 0
            val ranges = mutableListOf<IntRange>()
            val matches = mutableListOf<Pair<MatchResult, Color>>()

            for ((pattern, color) in patternHighlights) {
                for (match in Regex(pattern).findAll(text.text)) {
                    if (ranges.any { it.overlaps(match.range) }) continue
                    val insertionIndex = matches.binarySearch {
                        it.first.range.first.compareTo(match.range.first)
                    }
                    if (insertionIndex < 0) matches.add(match to color)
                    else matches.add(insertionIndex, match to color)
                    ranges.add(match.range)
                }
            }

            matches.sortBy { it.first.range.first }

            for ((match, color) in matches) {
                // Add normal text before the match
                if (match.range.first > lastIndex) {
                    append(text.text.substring(lastIndex, match.range.first))
                }

                // Apply color to the matched text
                withStyle(SpanStyle(color = color)) {
                    append(match.value)
                }

                lastIndex = match.range.last + 1
            }

            // Add remaining text after the last match
            if (lastIndex < text.text.length) {
                append(text.text.substring(lastIndex))
            }
        }

        lastResult = TransformedText(
            text = highlightedText,
            offsetMapping = OffsetMapping.Identity,
        )
        lastText = text.text
        return lastResult
    }
}

// Extension function to check for overlapping ranges
private fun IntRange.overlaps(other: IntRange): Boolean {
    return this.first <= other.last && other.first <= this.last
}

@Composable
private fun getColorHighlightMap(): Map<String, Color> {
    val midGray = colorResource(R.color.mid_gray)
    val orange = colorResource(R.color.orange)
    val darkBlue = colorResource(R.color.dark_blue)
    val darkOrange = colorResource(R.color.dark_orange)
    val blueBlack = colorResource(R.color.blue_black)
    val lightBlue = colorResource(R.color.light_blue)
    val fadedOrange = colorResource(R.color.faded_orange)
    val blueWhitePastel = colorResource(R.color.blue_white_pastel)

    val isLight = MaterialTheme.colors.isLight
    return remember(isLight) {
        if (isLight) {
            mapOf(
                "--.*" to midGray,
                "\".*?\"" to orange,
                "\\b(local|return|function|if|then|else|end|for|do|while|repeat|until|break|goto|and|or|not)\\b" to darkBlue,
                "\\b(nil|true|false)\\b" to darkOrange,
                "\\b(\\d+\\.?\\d*|\\.\\d+)\\b" to darkOrange,
                "\\b(\\w+)\\b" to blueBlack,
            )
        } else {
            mapOf(
                "--.*" to midGray,
                "\".*?\"" to orange,
                "\\b(local|return|function|if|then|else|end|for|do|while|repeat|until|break|goto|and|or|not)\\b" to lightBlue,
                "\\b(nil|true|false)\\b" to fadedOrange,
                "\\b(\\d+\\.?\\d*|\\.\\d+)\\b" to fadedOrange,
                "\\b(\\w+)\\b" to blueWhitePastel,
            )
        }
    }
}