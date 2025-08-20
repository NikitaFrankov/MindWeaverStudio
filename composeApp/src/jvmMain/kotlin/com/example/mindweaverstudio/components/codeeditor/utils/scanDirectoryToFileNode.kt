package com.example.mindweaverstudio.components.codeeditor.utils

import com.example.mindweaverstudio.components.codeeditor.models.FileNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Сканирует каталог по пути [rootPathStr] и строит дерево [FileNode].
 * - Выполняется в Dispatchers.IO (suspend).
 * - Защищён от циклических симлинков (visited set).
 * - Можно ограничить maxDepth и maxFiles чтобы не виснуть на огромных репозиториях.
 */
suspend fun scanDirectoryToFileNode(
    rootPathStr: String,
    includeHidden: Boolean = false,
    maxDepth: Int = Int.MAX_VALUE,
    maxFiles: Int = 50_000
): FileNode = withContext(Dispatchers.IO) {
    val rootPath = Paths.get(rootPathStr)
    val rootReal = try { rootPath.toRealPath(LinkOption.NOFOLLOW_LINKS) } catch (e: IOException) { rootPath.toAbsolutePath() }

    val visited = HashSet<Path>()
    var fileCounter = 0

    fun uiPathFor(p: Path): String {
        return try {
            val rel = rootReal.relativize(p).toString().replace(File.separatorChar, '/')
            if (rel.isEmpty()) "/${rootReal.fileName?.toString() ?: ""}" else "/$rel"
        } catch (e: IllegalArgumentException) {
            // p is on different root (shouldn't happen for normal recursion) — use absolute
            p.toAbsolutePath().toString().replace(File.separatorChar, '/')
        }
    }

    fun walk(p: Path, depth: Int): FileNode {
        if (fileCounter >= maxFiles) return FileNode(
            name = p.fileName?.toString() ?: p.toString(),
            path = uiPathFor(p),
            isDirectory = Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS),
            children = emptyList()
        )

        val isDir = Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)
        val name = p.fileName?.toString() ?: p.toString()
        val pathStr = uiPathFor(p)

        if (!includeHidden) {
            try {
                if (Files.isHidden(p)) {
                    return FileNode(
                        name = name,
                        path = pathStr,
                        isDirectory = isDir,
                        children = emptyList(),
                    )
                }
            } catch (_: IOException) {}
        }

        if (!isDir) {
            fileCounter++
            val content = try {
                Files.readString(p) // читаем содержимое файла
            } catch (_: IOException) {
                null // если не удалось прочитать (например бинарник или нет доступа)
            }
            return FileNode(
                name = name,
                path = pathStr,
                isDirectory = false,
                content = content
            )
        }

        // directory: protect against symlink loops
        val real = try { p.toRealPath(LinkOption.NOFOLLOW_LINKS) } catch (e: IOException) { p.toAbsolutePath() }
        if (!visited.add(real)) {
            // already visited (symlink loop) — return empty children to avoid recursion
            return FileNode(name = name, path = pathStr, isDirectory = true, children = emptyList())
        }

        if (depth >= maxDepth) {
            return FileNode(name = name, path = pathStr, isDirectory = true, children = emptyList())
        }

        val children = ArrayList<FileNode>()
        try {
            Files.newDirectoryStream(p).use { ds ->
                for (child in ds) {
                    // re-check fileCounter limit
                    if (fileCounter >= maxFiles) break
                    // try skip hidden
                    if (!includeHidden) {
                        try { if (Files.isHidden(child)) continue } catch (_: IOException) {}
                    }
                    children.add(walk(child, depth + 1))
                }
            }
        } catch (e: IOException) {
            // On permission error or I/O error — just skip children
        }

        // sort children by name for stable UI order
        children.sortBy { it.name }
        return FileNode(name = name, path = pathStr, isDirectory = true, children = children)
    }

    walk(rootReal, 0)
}