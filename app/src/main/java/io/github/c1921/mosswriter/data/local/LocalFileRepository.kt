package io.github.c1921.mosswriter.data.local

import android.content.Context
import io.github.c1921.mosswriter.data.model.NoteFile
import java.io.File

class LocalFileRepository(private val context: Context) {

    private fun projectDir(projectName: String): File =
        File(context.filesDir, "projects/$projectName").also { it.mkdirs() }

    private fun resolvedDir(projectName: String, relativePath: List<String>): File =
        relativePath.fold(projectDir(projectName)) { dir, segment -> File(dir, segment) }

    // --- Flat list for WebDAV sync (root only) ---

    fun listFiles(projectName: String): List<NoteFile> =
        projectDir(projectName).listFiles()
            ?.filter { it.isFile && it.name.endsWith(".md") }
            ?.map { NoteFile(it.name, it.lastModified(), it.length()) }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()

    // --- Recursive list for WebDAV sync (all depths) ---

    fun listFilesRecursive(projectName: String): List<NoteFile> {
        val root = projectDir(projectName)
        return root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".md") }
            .map { file ->
                val relativePath = file.relativeTo(root).path.replace(File.separatorChar, '/')
                NoteFile(relativePath, file.lastModified(), file.length())
            }
            .toList()
    }

    // --- Path-aware directory listing ---

    fun listEntries(projectName: String, relativePath: List<String>): List<NoteFile> {
        val dir = resolvedDir(projectName, relativePath)
        return dir.listFiles()
            ?.filter { it.isDirectory || it.name.endsWith(".md") }
            ?.map { NoteFile(it.name, it.lastModified(), if (it.isDirectory) 0L else it.length(), it.isDirectory) }
            ?.sortedWith(compareByDescending<NoteFile> { it.isDirectory }.thenByDescending { it.lastModified })
            ?: emptyList()
    }

    // --- Original two-arg overloads (updated to support slash-separated sub-paths) ---

    fun readFile(projectName: String, name: String): String {
        val segments = name.split("/")
        val file = if (segments.size > 1)
            File(resolvedDir(projectName, segments.dropLast(1)), segments.last())
        else
            File(projectDir(projectName), name)
        return file.takeIf { it.exists() }?.readText(Charsets.UTF_8) ?: ""
    }

    fun writeFile(projectName: String, name: String, content: String) {
        val segments = name.split("/")
        val dir = if (segments.size > 1)
            resolvedDir(projectName, segments.dropLast(1)).also { it.mkdirs() }
        else
            projectDir(projectName)
        File(dir, segments.last()).writeText(content, Charsets.UTF_8)
    }

    fun getFile(projectName: String, name: String): File {
        val segments = name.split("/")
        return if (segments.size > 1)
            File(resolvedDir(projectName, segments.dropLast(1)), segments.last())
        else
            File(projectDir(projectName), name)
    }

    // --- Path-aware overloads ---

    fun fileExists(projectName: String, relativePath: List<String>, name: String): Boolean =
        File(resolvedDir(projectName, relativePath), name).exists()

    fun writeFile(projectName: String, relativePath: List<String>, name: String, content: String) {
        resolvedDir(projectName, relativePath).mkdirs()
        File(resolvedDir(projectName, relativePath), name).writeText(content, Charsets.UTF_8)
    }

    fun deleteFile(projectName: String, relativePath: List<String>, name: String) {
        File(resolvedDir(projectName, relativePath), name).delete()
    }

    // --- Folder operations ---

    fun createFolder(projectName: String, relativePath: List<String>, folderName: String): Boolean {
        val dir = File(resolvedDir(projectName, relativePath), folderName)
        if (dir.exists()) return false
        return dir.mkdirs()
    }

    fun deleteFolder(projectName: String, relativePath: List<String>): Boolean {
        return resolvedDir(projectName, relativePath).deleteRecursively()
    }

    // --- Kept for backward compatibility (original signatures) ---

    fun deleteFile(projectName: String, name: String) {
        File(projectDir(projectName), name).delete()
    }

    fun fileExists(projectName: String, name: String): Boolean =
        File(projectDir(projectName), name).exists()
}
