package com.samco.trackandgraph.antlr

import com.blacksquircle.ui.language.base.Language
import com.blacksquircle.ui.language.base.exception.ParseException
import com.blacksquircle.ui.language.base.model.ParseResult
import com.blacksquircle.ui.language.base.model.Suggestion
import com.blacksquircle.ui.language.base.model.SyntaxScheme
import com.blacksquircle.ui.language.base.parser.LanguageParser
import com.blacksquircle.ui.language.base.provider.SuggestionProvider
import com.blacksquircle.ui.language.base.span.SyntaxHighlightSpan
import com.blacksquircle.ui.language.base.styler.LanguageStyler
import com.blacksquircle.ui.language.base.utils.StylingResult
import com.blacksquircle.ui.language.base.utils.StylingTask
import com.blacksquircle.ui.language.base.utils.WordsManager

class TnGLanguage: Language {

    override fun getName(): String {
        return "Track and Graph Custom Language"
    }

    override fun getParser(): LanguageParser {
        return CustomParser()
    }

    override fun getProvider(): SuggestionProvider {
        return CustomProvider()
    }

    override fun getStyler(): LanguageStyler {
        return CustomStyler()
    }
}

class CustomParser : LanguageParser {

    override fun execute(name: String, source: String): ParseResult {
        // TODO Implement parser
        // DO NOT DO THINGS ON MAIN THREAD!!
        val lineNumber = 0
        val columnNumber = 0
        val parseException = ParseException("describe exception here", lineNumber, columnNumber)
        return ParseResult(parseException)
    }
}

class CustomProvider : SuggestionProvider {

    // You can use WordsManager
    // if you don't want to write the language-specific implementation
    private val wordsManager = WordsManager()

    override fun getAll(): Set<Suggestion> {
        return wordsManager.getWords()
    }

    override fun processLine(lineNumber: Int, text: String) {
        wordsManager.processLine(lineNumber, text)
    }

    override fun deleteLine(lineNumber: Int) {
        wordsManager.deleteLine(lineNumber)
    }

    override fun clearLines() {
        wordsManager.clearLines()
    }
}

class CustomStyler : LanguageStyler {

    private var task: StylingTask? = null

    override fun execute(sourceCode: String, syntaxScheme: SyntaxScheme): List<SyntaxHighlightSpan> {
        val syntaxHighlightSpans = mutableListOf<SyntaxHighlightSpan>()

        // TODO Implement syntax highlighting

        return syntaxHighlightSpans
    }

    // StylingResult it's just a callback (List<SyntaxHighlightSpan>) -> Unit
    override fun enqueue(sourceCode: String, syntaxScheme: SyntaxScheme, stylingResult: StylingResult) {
        task?.cancelTask()
        task = StylingTask(
            doAsync = { execute(sourceCode, syntaxScheme) },
            onSuccess = stylingResult
        )
        task?.executeTask()
    }

    override fun cancel() {
        task?.cancelTask()
        task = null
    }
}


