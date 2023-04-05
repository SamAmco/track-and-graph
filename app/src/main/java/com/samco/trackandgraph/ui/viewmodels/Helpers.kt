package com.samco.trackandgraph.ui.viewmodels

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

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

fun TextFieldValue.asValidatedInt() = this.copy(text = this.text.asValidatedInt())

fun TextFieldValue.asValidatedDouble() = this.copy(text = this.text.asValidatedDouble())

fun Double.asTextFieldValue() = TextFieldValue(this.toString(), TextRange(this.toString().length))

