package me.soknight.easydesk.service.storage.data.service

import java.util.UUID
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import me.soknight.easydesk.service.storage.data.domain.Attachment
import org.koin.core.annotation.Single

/**
 * Reads and writes attachment bytes on local FS by opaque [storagePath].
 *
 * `rootPath` is provided by the `app` Koin module from `application.yaml`. For now,
 * a placeholder default is used — the real wiring is part of a separate config task.
 */
@Single
class AttachmentStorageService {

    // TODO: inject `rootPath: Path` from application config when storage config lands
    private val rootPath: Path = Path("./data/attachments")

    fun openSource(storagePath: String): Source =
        SystemFileSystem.source(Path(rootPath, storagePath)).buffered()

    fun store(source: Source, fileName: String, kind: Attachment.Kind): String {
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
