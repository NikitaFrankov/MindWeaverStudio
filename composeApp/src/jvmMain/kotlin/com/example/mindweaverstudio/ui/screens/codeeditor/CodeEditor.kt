package com.example.mindweaverstudio.ui.screens.codeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindweaverstudio.ui.theme.MindWeaverTheme
import kotlin.math.max

enum class Language { KOTLIN, JAVA, JSON, PLAIN }

@Composable
fun CodeEditor(
    modifier: Modifier = Modifier.fillMaxSize(),
    initialText: String = "",
    language: Language = Language.KOTLIN,
    lineNumberGutterWidth: Dp = 56.dp,
    onTextChange: (String) -> Unit = {}
) {
    var selectedLanguage by remember { mutableStateOf(language) }
    var searchQuery by remember { mutableStateOf("") }
    var wrapLines by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(14f) }

    Box(modifier.fillMaxSize().padding(8.dp)) {
        CodeEditor(
            text = initialText,
            onTextChange = {
                onTextChange(it)
            },
            language = selectedLanguage,
            searchQuery = searchQuery,
            wrapLines = wrapLines,
            fontSize = fontSize.sp,
            gutterWidth = lineNumberGutterWidth
        )
    }
}

@Composable
private fun LanguageDropdown(selected: Language, onSelect: (Language) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(selected.name) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Language.values().forEach { lang ->
                DropdownMenuItem(onClick = { onSelect(lang); expanded = false }) {
                    Text(lang.name)
                }
            }
        }
    }
}

@Composable
private fun CodeEditor(
    text: String,
    onTextChange: (String) -> Unit,
    language: Language,
    searchQuery: String,
    wrapLines: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    gutterWidth: Dp
) {
    val verticalScroll = rememberScrollState(0)
    val horizontalScroll = rememberScrollState(0)
    val focusRequester = remember { FocusRequester() }

    Row(modifier = Modifier.fillMaxSize().border(1.dp, Color.LightGray)) {
        // Gutter with line numbers
        val lines = text.split('\n')
        Column(
            modifier = Modifier
                .width(gutterWidth)
                .fillMaxHeight()
                .background(Color(0xFFFAFAFA))
                .verticalScroll(verticalScroll)
                .padding(6.dp)
        ) {
            for (i in 1..max(1, lines.size)) {
                Text(
                    text = i.toString(),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize * 0.9f,
                        color = MindWeaverTheme.colors.textPrimary
                    ),
                    color = Color.Gray
                )
            }
        }

        // Editor area
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .verticalScroll(verticalScroll)
            .horizontalScroll(horizontalScroll)
            .padding(8.dp)
            .clipToBounds()
        ) {
            // Highlighted (display) layer
            val highlighted = remember(text, language, searchQuery) {
                highlightSyntax(text, language, searchQuery)
            }

            Text(
                text = highlighted,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize
                ),
                color = MindWeaverTheme.colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
            )

            // Transparent BasicTextField on top to capture input
            BasicTextField(
                value = text,
                onValueChange = { onTextChange(it) },
                textStyle = TextStyle(
                    color = Color.Transparent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize
                ),
                modifier = Modifier
                    .matchParentSize()
                    .focusRequester(focusRequester)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusRequester.requestFocus() })
                    }
            )
        }
    }
}

// --- Simple syntax highlighting ---

private fun highlightSyntax(text: String, language: Language, searchQuery: String): AnnotatedString {
    return when (language) {
        Language.KOTLIN -> highlightWithRules(text, kotlinRules(), searchQuery)
        Language.JAVA -> highlightWithRules(text, javaRules(), searchQuery)
        Language.JSON -> highlightWithRules(text, jsonRules(), searchQuery)
        Language.PLAIN -> AnnotatedString(text)
    }
}

private fun highlightWithRules(text: String, rules: List<SyntaxRule>, searchQuery: String): AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder().apply {
        append(text)
    }

    // apply rules
    rules.forEach { rule ->
        val regex = rule.regex.toRegex()
        regex.findAll(text).forEach { match ->
            // addStyle доступен на AnnotatedString.Builder
            builder.addStyle(rule.style, match.range.first, match.range.last + 1)
        }
    }

    // highlight search matches with underline style
    if (searchQuery.isNotEmpty()) {
        val q = Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE)
        q.findAll(text).forEach { m ->
            builder.addStyle(
                SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                m.range.first,
                m.range.last + 1
            )
        }
    }

    return builder.toAnnotatedString()
}

private data class SyntaxRule(val regex: String, val style: SpanStyle)

private fun kotlinRules(): List<SyntaxRule> {
    val keywordStyle = SpanStyle(color = Color(0xFF7C4DFF))
    val typeStyle = SpanStyle(color = Color(0xFF1E88E5))
    val stringStyle = SpanStyle(color = Color(0xFF43A047))
    val commentStyle = SpanStyle(color = Color(0xFF9E9E9E))
    val numberStyle = SpanStyle(color = Color(0xFFFB8C00))

    val keywords = listOf(
        "fun", "val", "var", "if", "else", "for", "while", "return", "when", "is", "in", "object", "class", "interface", "sealed", "data", "package", "import", "override"
    ).joinToString("\\b|") { it }

    return listOf(
        SyntaxRule("//.*", commentStyle),
        SyntaxRule("/\\*(.|\\R)*?\\*/", commentStyle),
        SyntaxRule("\"([^\\\"\\\\]|\\\\.)*\"", stringStyle),
        SyntaxRule("'([^'\\\\]|\\\\.)*'", stringStyle),
        SyntaxRule("\\b($keywords)\\b", keywordStyle),
        SyntaxRule("\\b[0-9]+(\\.[0-9]+)?\\b", numberStyle),
        SyntaxRule("\\b[A-Z][A-Za-z0-9_]*\\b", typeStyle)
    )
}

private fun javaRules(): List<SyntaxRule> {
    val keywordStyle = SpanStyle(color = Color(0xFF7C4DFF))
    val stringStyle = SpanStyle(color = Color(0xFF43A047))
    val commentStyle = SpanStyle(color = Color(0xFF9E9E9E))

    val keywords = listOf("public", "private", "protected", "class", "static", "final", "void", "new", "if", "else", "for", "while", "return").joinToString("\\b|") { it }

    return listOf(
        SyntaxRule("//.*", commentStyle),
        SyntaxRule("/\\*(.|\\R)*?\\*/", commentStyle),
        SyntaxRule("\"([^\\\"\\\\]|\\\\.)*\"", stringStyle),
    SyntaxRule("\\b($keywords)\\b", keywordStyle)
    )
}

private fun jsonRules(): List<SyntaxRule> {
    val keyStyle = SpanStyle(color = Color(0xFF7C4DFF))
    val stringStyle = SpanStyle(color = Color(0xFF43A047))
    val numberStyle = SpanStyle(color = Color(0xFFFB8C00))

    return listOf(
        SyntaxRule("\"(.*?)\"(?=\\s*:)", keyStyle),
        SyntaxRule("\"([^\\\"\\\\]|\\\\.)*\"", stringStyle),
        SyntaxRule("\\b-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b", numberStyle)
    )
}

