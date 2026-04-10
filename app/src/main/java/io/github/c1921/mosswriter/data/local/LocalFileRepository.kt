package io.github.c1921.mosswriter.data.local

import android.content.Context
import io.github.c1921.mosswriter.data.model.NoteFile
import java.io.File

class LocalFileRepository(private val context: Context) {

    private fun projectDir(projectName: String): File =
        File(context.filesDir, "projects/$projectName").also { it.mkdirs() }

    fun listFiles(projectName: String): List<NoteFile> =
        projectDir(projectName).listFiles()
            ?.filter { it.isFile && it.name.endsWith(".md") }
            ?.map { NoteFile(it.name, it.lastModified(), it.length()) }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()

    fun readFile(projectName: String, name: String): String =
        File(projectDir(projectName), name).takeIf { it.exists() }?.readText(Charsets.UTF_8) ?: ""

    fun writeFile(projectName: String, name: String, content: String) {
        File(projectDir(projectName), name).writeText(content, Charsets.UTF_8)
    }

    fun deleteFile(projectName: String, name: String) {
        File(projectDir(projectName), name).delete()
    }

    fun fileExists(projectName: String, name: String): Boolean =
        File(projectDir(projectName), name).exists()

    fun getFile(projectName: String, name: String): File =
        File(projectDir(projectName), name)
}
