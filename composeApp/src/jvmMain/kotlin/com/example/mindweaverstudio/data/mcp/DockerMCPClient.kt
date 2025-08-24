package com.example.mindweaverstudio.data.mcp

import com.example.mindweaverstudio.data.models.mcp.base.ToolCall
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ListToolsRequest
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Files

class DockerMCPClient() {
    private val clientOut = PipedOutputStream()
    private val serverIn = PipedInputStream(clientOut)
    private val serverOut = PipedOutputStream()
    private val clientIn = PipedInputStream(serverOut)
    private val clientTransport = StdioClientTransport(
        input = clientIn.asSource().buffered(),
        output = clientOut.asSink().buffered()
    )
    private val serverTransport = StdioServerTransport(
        inputStream = serverIn.asSource().buffered(),
        outputStream = serverOut.asSink().buffered()
    )

    private val server = Server(
        serverInfo = Implementation(
            name = "example-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                resources = ServerCapabilities.Resources(
                    subscribe = true,
                    listChanged = true
                ),
                tools = ServerCapabilities.Tools(true),
            )
        )
    )
    private val client = Client(
        clientInfo = Implementation(
            name = "example-client",
            version = "1.0.0"
        )
    )
    private var tools: List<Tool> = emptyList()

    init {
        server.addTool(
            name = "run_junit_tests",
            description = "Run JUnit tests on a given Kotlin source code file using provided test code in a isolated Docker container. The tool creates a full Java/Kotlin environment with Gradle and JUnit dependencies, compiles the code, and executes the tests. If tests succeed, saves the test code as a file in the same directory as the source file, named after the test class (e.g., MyTest.kt). Input is the file path to the source code and the test code as a string.",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("file_path") {
                        put("type", "string")
                        put("description", "The file system path to the Kotlin source code file to be tested (e.g., /path/to/MyClass.kt)")
                    }
                    putJsonObject("test_code") {
                        put("type", "string")
                        put("description", "The Kotlin test code using JUnit (e.g., a class with @Test annotations). Included all necessary imports.")
                    }
                },
                required = listOf("file_path", "test_code")
            )
        ) { request ->
            val filePath = request.arguments["file_path"]?.jsonPrimitive?.content ?: return@addTool CallToolResult(
                content = listOf(TextContent("The 'file_path' parameter is required."))
            )
            val testCode = request.arguments["test_code"]?.jsonPrimitive?.content ?: return@addTool CallToolResult(
                content = listOf(TextContent("The 'test_code' parameter is required."))
            )

            // Step 1: Validate and read the source file
            val sourceFile = File(filePath)
            if (!sourceFile.exists() || !sourceFile.isFile) {
                return@addTool CallToolResult(
                    content = listOf(TextContent("The provided file_path does not point to a valid file."))
                )
            }
            val sourceCode = try {
                sourceFile.readText()
            } catch (e: Exception) {
                return@addTool CallToolResult(
                    content = listOf(TextContent("Error reading the source file: ${e.message}"))
                )
            }

            // Step 2: Create a temporary directory for the project
            val tempDir = Files.createTempDirectory("junit_test_project_").toFile()
            tempDir.deleteOnExit()

            // Step 3: Set up project structure
            // Create src/main/kotlin/Source.kt (assuming the source is a single file; adjust if needed)
            val mainSrcDir = File(tempDir, "src/main/kotlin")
            mainSrcDir.mkdirs()
            val sourceFileName = sourceFile.name // Preserve original file name
            File(mainSrcDir, sourceFileName).writeText(sourceCode)

            // Create src/test/kotlin/Test.kt
            val testSrcDir = File(tempDir, "src/test/kotlin")
            testSrcDir.mkdirs()
            val testFileName = "Test.kt" // Temporary name inside container
            File(testSrcDir, testFileName).writeText(testCode)

            // Create build.gradle for Kotlin + JUnit setup
            val buildGradleContent = """
        plugins {
            id 'org.jetbrains.kotlin.jvm' version '1.9.0'
            id 'application'
        }

        group = 'com.example'
        version = '1.0-SNAPSHOT'

        repositories {
            mavenCentral()
        }

        dependencies {
            implementation 'org.jetbrains.kotlin:kotlin-stdlib'
            testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
            testImplementation 'org.jetbrains.kotlin:kotlin-test'
        }

        test {
            useJUnitPlatform()
        }
    """.trimIndent()
            File(tempDir, "build.gradle").writeText(buildGradleContent)

            // Create settings.gradle (required for Gradle)
            File(tempDir, "settings.gradle").writeText("rootProject.name = 'junit_test_project'")

            // Pre-pull Docker image quietly to avoid verbose pull output in main run
            val pullCommand = listOf("docker", "pull", "--quiet", "gradle:8.0-jdk17")
            val pullProcessBuilder = ProcessBuilder(pullCommand)
            val pullProcess = try {
                pullProcessBuilder.start()
            } catch (e: Exception) {
                tempDir.deleteRecursively()
                return@addTool CallToolResult(
                    content = listOf(TextContent("Error pulling Docker image: ${e.message}. Ensure Docker is installed and running."))
                )
            }
            pullProcess.waitFor()
            if (pullProcess.exitValue() != 0) {
                val pullError = BufferedReader(InputStreamReader(pullProcess.errorStream)).use { it.readText() }
                tempDir.deleteRecursively()
                return@addTool CallToolResult(
                    content = listOf(TextContent("Failed to pull Docker image: $pullError"))
                )
            }

            // Step 4: Run Docker container with Gradle image
            // Add --info for detailed test logging
            val dockerCommand = listOf(
                "docker", "run", "--rm",
                "-v", "${tempDir.absolutePath}:/project",
                "-w", "/project",
                // Используем gradle + jdk17 образ с полноценным Debian
                "gradle:8.5-jdk17",
                "gradle", "clean", "test", "--no-daemon", "--console=plain", "--info"
            )

            val processBuilder = ProcessBuilder(dockerCommand)
            processBuilder.redirectErrorStream(true) // Merge stderr into stdout
            val process = try {
                processBuilder.start()
            } catch (e: Exception) {
                tempDir.deleteRecursively()
                return@addTool CallToolResult(
                    content = listOf(TextContent("Error starting Docker process: ${e.message}. Ensure Docker is installed and running."))
                )
            }

            // Capture output
            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()

            // Filter output to minimal useful info: start from first task line
            val lines = output.toString().lines()
            val startIndex = lines.indexOfFirst { it.startsWith(">") }
            val filteredOutput = if (startIndex >= 0) {
                lines.subList(startIndex, lines.size).joinToString("\n")
            } else {
                output.toString() // Fallback if no tasks found (e.g., early error)
            }

            // Clean up temp dir
            tempDir.deleteRecursively()

            // Step 5: Check if tests succeeded and save test code if yes
            val exitValue = process.exitValue()
            var resultContent = if (exitValue == 0) {
                // Extract test class name from test_code using regex
                val classNameRegex = Regex("""class\s+(\w+)\s*(?:\{|:)""")
                val match = classNameRegex.find(testCode)
                val testClassName = match?.groupValues?.get(1) ?: "GeneratedTest" // Fallback if not found

                // Save test code to file in source directory
                val sourceDir = sourceFile.parentFile
                println("info bout source file,  parentFile = ${sourceFile.parentFile}, absoluteFile = ${sourceFile.absoluteFile}, absolutePath = ${sourceFile.absolutePath}")
                val testOutputFile = File(sourceDir, "$testClassName.kt")
                try {
                    testOutputFile.writeText(testCode)
                    "Tests executed successfully. Test code saved to ${testOutputFile.absolutePath}.\nOutput:\n$filteredOutput"
                } catch (e: Exception) {
                    "Tests executed successfully, but error saving test file: ${e.message}.\\nOutput:\\n$filteredOutput"
                }
            } else {
                "Tests failed with exit code $exitValue.\nOutput:\n$filteredOutput"
            }

            CallToolResult(content = listOf(TextContent(resultContent)))
        }
    }

    suspend fun init() {
        server.connect(serverTransport)
        client.connect(clientTransport)

        val toolsResult = client.listTools(request = ListToolsRequest())
        tools = toolsResult?.tools.orEmpty()
    }

    suspend fun getTools(): List<Tool> {
        if (tools.isNotEmpty()) return tools

        val toolsResult = client.listTools(request = ListToolsRequest())
        tools = toolsResult?.tools.orEmpty()
        return tools
    }

    suspend fun callTool(call: ToolCall): List<TextContent>? {
        val arguments = buildJsonObject {
            call.params.forEach {
                put(it.key, it.value)
            }
        }

        // Вызов tool
        val request = CallToolRequest(
            name = call.tool,
            arguments = arguments
        )

        val result = client.callTool(request)

        return result?.content as List<TextContent>?
    }
}
