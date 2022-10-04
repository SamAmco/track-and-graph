package com.samco.trackandgraph.ui.compose.viewmodels

fun String.asValidatedInt() =
    this.take(1).filter { it.isDigit() || it == '-' } + this.drop(1).takeWhile { it.isDigit() }

fun String.asValidatedDouble(): String {
    val firstChar = this.take(1).filter { it.isDigit() || it == '-' || it == '.' }
    val remainingChars = this.drop(1).filter { it.isDigit() || it == '.' }
    if (remainingChars.count { it == '.' } > 1) {
        val remainingCharsFiltered = remainingChars
            .takeWhile { it != '.' } + '.' + remainingChars
            .dropWhile { it != '.' }
            .filter { it != '.' }
        return firstChar + remainingCharsFiltered
    }
    return firstChar + remainingChars
}

