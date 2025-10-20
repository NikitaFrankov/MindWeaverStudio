package com.example.mindweaverstudio.data.clients

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.Binds.fromPrimitive
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.WaitResponse
import com.github.dockerjava.core.DockerClientBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory

class DockerClient {
    private val dockerClient: DockerClient = DockerClientBuilder.getInstance().build()

    fun checkCode(code: String, language: String): String {
        val tempDir: Path = createTempDirectory("code-check-")
        val codeFile: File = when (language.lowercase()) {
            "python" -> tempDir.resolve("script.py").toFile()
            "java" -> tempDir.resolve("Main.java").toFile()
            "kotlin" -> tempDir.resolve("Main.kt").toFile()
            else -> throw IllegalArgumentException("Unsupported language: $language")
        }
        codeFile.writeText(code)

        val (image, cmd) = when (language.lowercase()) {
            "python" -> "python:3-slim" to listOf("python", "/code/script.py")
            "java" -> "openjdk:17-slim" to listOf("javac", "/code/Main.java", "&&", "java", "-cp", "/code", "Main")
            "kotlin" -> "zenika/kotlin" to listOf(
                "kotlinc", "/code/Main.kt", "-include-runtime", "-d", "/code/Main.jar", "&&", "java", "-jar", "/code/Main.jar"
            )
            else -> throw IllegalArgumentException("Unsupported language")
        }

        val imageStream = File(image).inputStream()
        dockerClient.loadImageCmd(imageStream)?.exec()

        val createCmd: CreateContainerCmd = dockerClient.createContainerCmd(image)
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withBinds(fromPrimitive(arrayOf("$tempDir:/code")))
                    .withCpuQuota(100000L)
                    .withMemory(256L * 1024 * 1024)
            )
            .withCmd("/bin/sh", "-c", cmd.joinToString(" "))

        val containerId = createCmd.exec().id

        try {
            dockerClient.startContainerCmd(containerId).exec()

            val exitCode = AtomicInteger(-1)
            val callback = object : ResultCallback.Adapter<WaitResponse>() {
                override fun onNext(response: WaitResponse) {
                    exitCode.set(response.statusCode)
                    super.onNext(response)
                }
            }

            val waitResult = dockerClient.waitContainerCmd(containerId)
                .exec(callback)
                .awaitCompletion(30, TimeUnit.SECONDS)

            if (!waitResult) {
                dockerClient.killContainerCmd(containerId).exec()
                return "Execution timed out after 30 seconds"
            }

            val logs = dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .exec(ResultCallback.Adapter())
                .awaitCompletion()
                .toString()

            val status = exitCode.get()
            return if (status == 0) {
                logs
            } else {
                "Execution failed with exit code $status\nLogs: $logs"
            }
        } finally {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec()
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
        }
    }
}