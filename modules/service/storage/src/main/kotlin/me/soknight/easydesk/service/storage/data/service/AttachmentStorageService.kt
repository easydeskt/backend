package me.soknight.easydesk.service.storage.data.service

import java.util.UUID
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import me.soknight.easydesk.channel.api.model.Attachment.Kind
import me.soknight.easydesk.service.storage.config.StorageConfig
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/** Reads and writes attachment bytes on local FS by opaque [storagePath]. */
@Single
class AttachmentStorageService(@Provided config: StorageConfig) {

    private val rootPath = Path(config.rootPath)

    fun openSource(storagePath: String): Source =
        SystemFileSystem.source(Path(rootPath, storagePath)).buffered()

    fun store(source: Source, fileName: String, kind: Kind): String {
        val ext = fileName.substringAfterLast('.', "bin")
        val relativePath = "${kind.key}/${UUID.randomUUID()}.$ext"
        val target = Path(rootPath, relativePath)
        SystemFileSystem.createDirectories(Path(rootPath, kind.key))
        SystemFileSystem.sink(target).buffered().use { sink ->
            sink.write(source.readByteArray())
        }
        return relativePath
    }

}
