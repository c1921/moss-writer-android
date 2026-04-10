package io.github.c1921.mosswriter.data.remote

import io.github.c1921.mosswriter.data.local.LocalFileRepository
import io.github.c1921.mosswriter.data.model.WebDavConfig

class WebDavRepository(
    private val localRepo: LocalFileRepository,
    private val config: WebDavConfig
) {

    fun sync(): Result<Unit> = runCatching {
        val client = WebDavClient(config)
        val projectName = config.projectName

        val remoteFiles = client.listRemoteFiles().getOrThrow()
            .associateBy { it.name }

        val localFiles = localRepo.listFilesRecursive(projectName)
            .associateBy { it.name }

        val allNames = (remoteFiles.keys + localFiles.keys).toSet()

        for (name in allNames) {
            val remote = remoteFiles[name]
            val local = localFiles[name]

            when {
                remote != null && local == null -> {
                    val content = client.downloadFile(name).getOrThrow()
                    localRepo.writeFile(projectName, name, content)
                    localRepo.getFile(projectName, name).setLastModified(remote.lastModified)
                }
                remote == null && local != null -> {
                    val parentPath = name.substringBeforeLast('/', "")
                    if (parentPath.isNotEmpty()) client.ensureRemoteDir(parentPath)
                    val content = localRepo.readFile(projectName, name)
                    client.uploadFile(name, content).getOrThrow()
                }
                remote != null && local != null -> {
                    val localModified = local.lastModified
                    val remoteModified = remote.lastModified
                    when {
                        remoteModified > localModified -> {
                            val content = client.downloadFile(name).getOrThrow()
                            localRepo.writeFile(projectName, name, content)
                            localRepo.getFile(projectName, name).setLastModified(remote.lastModified)
                        }
                        localModified > remoteModified -> {
                            val parentPath = name.substringBeforeLast('/', "")
                            if (parentPath.isNotEmpty()) client.ensureRemoteDir(parentPath)
                            val content = localRepo.readFile(projectName, name)
                            client.uploadFile(name, content).getOrThrow()
                        }
                    }
                }
            }
        }
    }
}
