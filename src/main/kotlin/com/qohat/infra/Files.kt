package com.qohat.infra

import arrow.core.Either
import com.qohat.domain.Attachment
import com.qohat.domain.FileContent
import com.qohat.error.DomainError
import com.qohat.error.FileNotFoundError
import com.qohat.error.Unexpected
import com.qohat.http.logger
import java.io.File
import java.io.FileOutputStream
import java.util.Base64

@JvmInline
value class Path(val value: String)
object Files {

    fun decode(content: String): ByteArray =
        Base64.getDecoder().decode(content)

    fun save(attachment: Attachment) {
        val imageByteArray = decode(attachment.content)
        val directory = File(attachment.path)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        File("${attachment.path}/${attachment.name}").writeBytes(imageByteArray)
    }

    fun from(path: Path): Either<DomainError, FileContent> =
        Either.catch { File(path.value).readText() }
        .map { FileContent(it) }
        .tapLeft { e -> logger.error("Not File Found Exception", e) }
        .mapLeft { FileNotFoundError(path.value) }

    fun createFile(filename: String, content: String): Either<DomainError, Unit> = Either.catch {
        val file = File("scripts/$filename") // Specify the folder path and the desired filename
        file.parentFile.mkdirs() // Create parent directories if they don't exist
        val outputStream = FileOutputStream(file)
        outputStream.write(content.toByteArray()) // Write content to the file
        outputStream.close() // Close the output stream
    }.mapLeft { t -> Unexpected("Failed creating the sql file $filename", t) }

}