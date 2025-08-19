package com.example.mindweaverstudio.data.mcp

object ProcessRunner {
    fun runCommand(command: List<String>, head: Int = 10, tail: Int = 10): String {
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val lines = process.inputStream.bufferedReader().use { it.readLines() }
            process.waitFor()

            if (lines.size <= head + tail) {
                lines.joinToString("\n")
            } else {
                val headLines = lines.take(head)
                val tailLines = lines.takeLast(tail)
                headLines.joinToString("\n") + "\n... [${lines.size - head - tail} lines skipped] ...\n" + tailLines.joinToString("\n")
            }
        } catch (e: Exception) {
            "Error while executing command: ${e.message}"
        }
    }
}