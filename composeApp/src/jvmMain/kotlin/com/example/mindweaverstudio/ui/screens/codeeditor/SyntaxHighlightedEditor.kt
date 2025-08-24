package com.example.mindweaverstudio.ui.screens.codeeditor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clipToBounds
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.Theme
import org.fife.ui.rtextarea.RTextScrollPane
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

@Composable
fun SyntaxHighlightedEditor(
    initialContent: String,
    onContentChanged: (String) -> Unit,
    isKotlin: Boolean = true,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(initialContent) }

    val textArea = remember {
        RSyntaxTextArea().apply {
            syntaxEditingStyle = if (isKotlin) SyntaxConstants.SYNTAX_STYLE_JAVA else SyntaxConstants.SYNTAX_STYLE_GROOVY
            isCodeFoldingEnabled = true
            antiAliasingEnabled = true
            isBracketMatchingEnabled = true
            font = java.awt.Font("JetBrains Mono", java.awt.Font.PLAIN, 14)

            // Тема Atom One Dark (адаптирована под доступные token types)
            val theme = Theme(this).apply {
                bgColor = java.awt.Color.decode("#282c34") // Background
                caretColor = java.awt.Color.decode("#528bff") // Caret
                currentLineHighlightColor = java.awt.Color.decode("#2c323c")
                fadeCurrentLineHighlight = true

                // Цвета для токенов (расширенные для большего количества типов)
                baseFont = font // Monospace
//                baseStyle = Style().withForeground(java.awt.Color.decode("#abb2bf")) // Default text
//
//                // Keywords (val, fun ~ reserved words in Java)
//                keyword = Style().withForeground(java.awt.Color.decode("#c678dd")).withBold(true)
//
//                // Identifiers (variables)
//                identifier = Style().withForeground(java.awt.Color.decode("#abb2bf"))
//
//                // Data types/classes
//                dataType = Style().withForeground(java.awt.Color.decode("#e5c07b"))
//
//                // Functions (обрабатываются как identifiers, но можно override в custom если нужно)
//                function = Style().withForeground(java.awt.Color.decode("#61afef"))
//
//                // Strings
//                literalString = Style().withForeground(java.awt.Color.decode("#98c379"))
//
//                // Numbers/literals
//                literalNumber = Style().withForeground(java.awt.Color.decode("#d19a66"))
//
//                // Operators
//                operator = Style().withForeground(java.awt.Color.decode("#56b6c2"))
//
//                // Comments
//                comment = Style().withForeground(java.awt.Color.decode("#5c6370")).withItalic(true)
//                commentMultiline = Style().withForeground(java.awt.Color.decode("#5c6370")).withItalic(true)
//
//                // Annotations (@)
//                annotation = Style().withForeground(java.awt.Color.decode("#e5c07b"))
//
//                // Errors
//                errorIdentifier = Style().withForeground(java.awt.Color.decode("#e06c75")).withUnderline(true)

                // Brackets/marks
                matchedBracketBorderColor = java.awt.Color.decode("#3a3f4b")
                matchedBracketFG = java.awt.Color.decode("#abb2bf")
                matchedBracketBG = java.awt.Color.decode("#3a3f4b")
            }
            theme.apply(this)

            this.text = initialContent

            document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) { text = this@apply.text; onContentChanged(text) }
                override fun removeUpdate(e: DocumentEvent?) { text = this@apply.text; onContentChanged(text) }
                override fun changedUpdate(e: DocumentEvent?) { text = this@apply.text; onContentChanged(text) }
            })
        }
    }

    SwingPanel(
        factory = { RTextScrollPane(textArea) },
        modifier = modifier.fillMaxSize().clipToBounds()
    )

    DisposableEffect(initialContent) {
        if (textArea.text != initialContent) textArea.text = initialContent
        onDispose {}
    }
}