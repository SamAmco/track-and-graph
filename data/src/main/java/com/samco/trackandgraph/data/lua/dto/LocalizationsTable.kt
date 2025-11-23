package com.samco.trackandgraph.data.lua.dto

import com.samco.trackandgraph.data.localisation.TranslatedString

/**
 * A table of localization strings, mapping translation keys to their translated values.
 * Translation keys are typically prefixed with underscore (e.g., "_hours", "_days")
 * by convention to distinguish them from literal strings.
 */
typealias LocalizationsTable = Map<String, TranslatedString>
