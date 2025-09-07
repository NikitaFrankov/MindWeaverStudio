package com.example.mindweaverstudio.data.utils.ragchunking.logic

import com.example.mindweaverstudio.data.utils.ragchunking.models.ChunkType
import com.example.mindweaverstudio.data.utils.ragchunking.models.CodeElement
import com.example.mindweaverstudio.data.utils.ragchunking.models.FileAnalysis
import java.io.File
import kotlin.text.iterator

class KotlinCodeParser {

    fun parseFile(file: File): FileAnalysis {
        val lines = file.readLines()
        val content = lines.joinToString("\n")

        return FileAnalysis(
            filePath = file.absolutePath,
            packageDeclaration = extractPackageDeclaration(lines),
            imports = extractImports(lines),
            elements = parseCodeElements(lines),
            totalLines = lines.size
        )
    }

    private fun extractPackageDeclaration(lines: List<String>): String? {
        return lines.find { it.trim().startsWith("package ") }
            ?.substringAfter("package ")
            ?.substringBefore("//")
            ?.trim()
    }

    private fun extractImports(lines: List<String>): List<String> {
        return lines.filter { it.trim().startsWith("import ") }
            .map {
                it.substringAfter("import ")
                    .substringBefore("//")
                    .trim()
            }
    }

    private fun parseCodeElements(lines: List<String>): List<CodeElement> {
        val elements = mutableListOf<CodeElement>()
        var currentDoc: String? = null
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()

            when {
                isDocumentationComment(line) -> {
                    val docEnd = findDocumentationEnd(lines, i)
                    currentDoc = extractDocumentation(lines, i, docEnd)
                    i = docEnd
                }

                isClassDeclaration(line) -> {
                    val element = parseClass(lines, i, currentDoc)
                    elements.add(element)
                    currentDoc = null
                    i = element.endLine
                }

                isInterfaceDeclaration(line) -> {
                    val element = parseInterface(lines, i, currentDoc)
                    elements.add(element)
                    currentDoc = null
                    i = element.endLine
                }

                isEnumDeclaration(line) -> {
                    val element = parseEnum(lines, i, currentDoc)
                    elements.add(element)
                    currentDoc = null
                    i = element.endLine
                }

                isFunctionDeclaration(line) -> {
                    val element = parseFunction(lines, i, currentDoc)
                    elements.add(element)
                    currentDoc = null
                    i = element.endLine
                }

                isPropertyDeclaration(line) -> {
                    val element = parseProperty(lines, i, currentDoc)
                    elements.add(element)
                    currentDoc = null
                    i = element.endLine
                }

                else -> i++
            }
        }

        return elements
    }

    private fun isDocumentationComment(line: String): Boolean {
        return line.startsWith("/**") || line.startsWith("/*")
    }

    private fun findDocumentationEnd(lines: List<String>, start: Int): Int {
        for (i in start until lines.size) {
            if (lines[i].trim().endsWith("*/")) {
                return i + 1
            }
        }
        return start + 1
    }

    private fun extractDocumentation(lines: List<String>, start: Int, end: Int): String {
        return lines.subList(start, end).joinToString("\n")
    }

    private fun isClassDeclaration(line: String): Boolean {
        val cleanLine = line.removeAnnotations().trim()
        return Regex("""(public|private|internal|protected)?\s*(abstract|sealed|open|final)?\s*class\s+\w+""")
            .find(cleanLine) != null
    }

    private fun isInterfaceDeclaration(line: String): Boolean {
        val cleanLine = line.removeAnnotations().trim()
        return Regex("""(public|private|internal|protected)?\s*interface\s+\w+""")
            .find(cleanLine) != null
    }

    private fun isEnumDeclaration(line: String): Boolean {
        val cleanLine = line.removeAnnotations().trim()
        return Regex("""(public|private|internal|protected)?\s*enum\s+class\s+\w+""")
            .find(cleanLine) != null
    }

    private fun isFunctionDeclaration(line: String): Boolean {
        val cleanLine = line.removeAnnotations().trim()
        return Regex("""(public|private|internal|protected)?\s*(override|suspend|inline|infix)?\s*fun\s+\w+""")
            .find(cleanLine) != null
    }

    private fun isPropertyDeclaration(line: String): Boolean {
        val cleanLine = line.removeAnnotations().trim()
        return (Regex("""(public|private|internal|protected)?\s*(val|var)\s+\w+""").find(cleanLine) != null) &&
                hasComplexInitialization(cleanLine)
    }

    private fun hasComplexInitialization(line: String): Boolean {
        return line.contains("{") || line.contains("=") && line.length > 50
    }

    private fun String.removeAnnotations(): String {
        return this.replace(Regex("""@\w+(\([^)]*\))?\s*"""), "")
    }

    private fun parseClass(lines: List<String>, startIndex: Int, documentation: String?): CodeElement {
        val startLine = startIndex + 1 // Convert to 1-based indexing
        val endLine = findBlockEnd(lines, startIndex)
        val signature = buildClassSignature(lines, startIndex)
        val name = extractClassName(lines[startIndex])
        val modifiers = extractModifiers(lines[startIndex])

        return CodeElement(
            type = ChunkType.CLASS,
            name = name,
            startLine = startLine,
            endLine = endLine,
            signature = signature,
            modifiers = modifiers,
            isPrivate = modifiers.contains("private"),
            documentation = documentation
        )
    }

    private fun parseInterface(lines: List<String>, startIndex: Int, documentation: String?): CodeElement {
        val startLine = startIndex + 1
        val endLine = findBlockEnd(lines, startIndex)
        val signature = buildInterfaceSignature(lines, startIndex)
        val name = extractInterfaceName(lines[startIndex])
        val modifiers = extractModifiers(lines[startIndex])

        return CodeElement(
            type = ChunkType.INTERFACE,
            name = name,
            startLine = startLine,
            endLine = endLine,
            signature = signature,
            modifiers = modifiers,
            isPrivate = modifiers.contains("private"),
            documentation = documentation
        )
    }

    private fun parseEnum(lines: List<String>, startIndex: Int, documentation: String?): CodeElement {
        val startLine = startIndex + 1
        val endLine = findBlockEnd(lines, startIndex)
        val signature = buildEnumSignature(lines, startIndex)
        val name = extractEnumName(lines[startIndex])
        val modifiers = extractModifiers(lines[startIndex])

        return CodeElement(
            type = ChunkType.ENUM,
            name = name,
            startLine = startLine,
            endLine = endLine,
            signature = signature,
            modifiers = modifiers,
            isPrivate = modifiers.contains("private"),
            documentation = documentation
        )
    }

    private fun parseFunction(lines: List<String>, startIndex: Int, documentation: String?): CodeElement {
        val startLine = startIndex + 1
        val endLine = findFunctionEnd(lines, startIndex)
        val signature = buildFunctionSignature(lines, startIndex, endLine - 1)
        val name = extractFunctionName(lines[startIndex])
        val modifiers = extractModifiers(lines[startIndex])

        return CodeElement(
            type = ChunkType.FUNCTION,
            name = name,
            startLine = startLine,
            endLine = endLine,
            signature = signature,
            modifiers = modifiers,
            isPrivate = modifiers.contains("private"),
            documentation = documentation
        )
    }

    private fun parseProperty(lines: List<String>, startIndex: Int, documentation: String?): CodeElement {
        val startLine = startIndex + 1
        val endLine = findPropertyEnd(lines, startIndex)
        val signature = buildPropertySignature(lines, startIndex, endLine - 1)
        val name = extractPropertyName(lines[startIndex])
        val modifiers = extractModifiers(lines[startIndex])

        return CodeElement(
            type = ChunkType.PROPERTY,
            name = name,
            startLine = startLine,
            endLine = endLine,
            signature = signature,
            modifiers = modifiers,
            isPrivate = modifiers.contains("private"),
            documentation = documentation
        )
    }

    private fun findBlockEnd(lines: List<String>, startIndex: Int): Int {
        var braceCount = 0
        var foundFirstBrace = false

        for (i in startIndex until lines.size) {
            val line = lines[i]
            for (char in line) {
                when (char) {
                    '{' -> {
                        braceCount++
                        foundFirstBrace = true
                    }
                    '}' -> {
                        braceCount--
                        if (foundFirstBrace && braceCount == 0) {
                            return i + 1 // Convert to 1-based indexing
                        }
                    }
                }
            }
        }
        return lines.size
    }

    private fun findFunctionEnd(lines: List<String>, startIndex: Int): Int {
        val line = lines[startIndex]
        return if (line.contains("{")) {
            findBlockEnd(lines, startIndex)
        } else {
            // Single expression function or abstract function
            startIndex + 1
        }
    }

    private fun findPropertyEnd(lines: List<String>, startIndex: Int): Int {
        val line = lines[startIndex]
        return if (line.contains("{")) {
            findBlockEnd(lines, startIndex)
        } else {
            startIndex + 1
        }
    }

    private fun buildClassSignature(lines: List<String>, startIndex: Int): String {
        val endIndex = findSignatureEnd(lines, startIndex)
        return lines.subList(startIndex, endIndex + 1).joinToString("\n").trim()
    }

    private fun buildInterfaceSignature(lines: List<String>, startIndex: Int): String {
        val endIndex = findSignatureEnd(lines, startIndex)
        return lines.subList(startIndex, endIndex + 1).joinToString("\n").trim()
    }

    private fun buildEnumSignature(lines: List<String>, startIndex: Int): String {
        val endIndex = findSignatureEnd(lines, startIndex)
        return lines.subList(startIndex, endIndex + 1).joinToString("\n").trim()
    }

    private fun buildFunctionSignature(lines: List<String>, startIndex: Int, endIndex: Int): String {
        val signatureEndIndex = findSignatureEnd(lines, startIndex)
        return lines.subList(startIndex, minOf(signatureEndIndex + 1, endIndex + 1)).joinToString("\n").trim()
    }

    private fun buildPropertySignature(lines: List<String>, startIndex: Int, endIndex: Int): String {
        val signatureEndIndex = findSignatureEnd(lines, startIndex)
        return lines.subList(startIndex, minOf(signatureEndIndex + 1, endIndex + 1)).joinToString("\n").trim()
    }

    private fun findSignatureEnd(lines: List<String>, startIndex: Int): Int {
        for (i in startIndex until lines.size) {
            val line = lines[i]
            if (line.contains("{") || line.contains("=")) {
                return i
            }
        }
        return startIndex
    }

    private fun extractClassName(line: String): String {
        return Regex("""class\s+(\w+)""").find(line)?.groupValues?.get(1) ?: "UnknownClass"
    }

    private fun extractInterfaceName(line: String): String {
        return Regex("""interface\s+(\w+)""").find(line)?.groupValues?.get(1) ?: "UnknownInterface"
    }

    private fun extractEnumName(line: String): String {
        return Regex("""enum\s+class\s+(\w+)""").find(line)?.groupValues?.get(1) ?: "UnknownEnum"
    }

    private fun extractFunctionName(line: String): String {
        return Regex("""fun\s+(\w+)""").find(line)?.groupValues?.get(1) ?: "UnknownFunction"
    }

    private fun extractPropertyName(line: String): String {
        return Regex("""(val|var)\s+(\w+)""").find(line)?.groupValues?.get(2) ?: "UnknownProperty"
    }

    private fun extractModifiers(line: String): List<String> {
        val modifiers = mutableListOf<String>()
        val possibleModifiers = listOf(
            "public", "private", "internal", "protected",
            "abstract", "sealed", "open", "final",
            "override", "suspend", "inline", "infix"
        )

        for (modifier in possibleModifiers) {
            if (Regex("""\b$modifier\b""").find(line) != null) {
                modifiers.add(modifier)
            }
        }

        return modifiers
    }
}