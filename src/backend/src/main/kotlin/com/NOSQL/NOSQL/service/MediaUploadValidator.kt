package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.generated.ActorMediaType
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.SequenceInputStream

/**
 * Validates uploaded media: filename extension and leading bytes (magic numbers).
 */
object MediaUploadValidator {

    const val PEEK_HEADER_BYTES: Int = 32

    private val PHOTO_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
    private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mov", "m4v")

    const val INVALID_MEDIA_EXTENSION = "INVALID_MEDIA_EXTENSION"
    const val INVALID_MEDIA_SIGNATURE = "INVALID_MEDIA_SIGNATURE"

    private enum class PhysicalKind {
        JPEG,
        PNG,
        GIF,
        WEBP,
        MP4,
        WEBM,
    }

    fun peekHeaderAndWrap(inputStream: InputStream): Pair<ByteArray, InputStream> {
        val header = ByteArray(PEEK_HEADER_BYTES)
        var total = 0
        while (total < PEEK_HEADER_BYTES) {
            val r = inputStream.read(header, total, PEEK_HEADER_BYTES - total)
            if (r == -1) break
            total += r
        }
        val captured = header.copyOf(total)
        val rest = SequenceInputStream(ByteArrayInputStream(captured), inputStream)
        return Pair(captured, rest)
    }

    fun validate(actorMediaType: ActorMediaType, filename: String, header: ByteArray): String? {
        val ext = extractExtension(filename)
        if (ext.isEmpty()) {
            return INVALID_MEDIA_EXTENSION
        }

        val allowedExt = when (actorMediaType) {
            ActorMediaType.photo -> PHOTO_EXTENSIONS
            ActorMediaType.video -> VIDEO_EXTENSIONS
        }
        if (ext !in allowedExt) {
            return INVALID_MEDIA_EXTENSION
        }

        val expectedKind = extensionToKind(ext) ?: return INVALID_MEDIA_EXTENSION
        val detected = detectPhysicalKind(header) ?: return INVALID_MEDIA_SIGNATURE

        if (detected != expectedKind) {
            return INVALID_MEDIA_SIGNATURE
        }

        return null
    }

    private fun extractExtension(filename: String): String {
        val dot = filename.lastIndexOf('.')
        if (dot < 0 || dot == filename.length - 1) {
            return ""
        }
        return filename.substring(dot + 1).lowercase()
    }

    private fun extensionToKind(ext: String): PhysicalKind? =
        when (ext) {
            "jpg", "jpeg" -> PhysicalKind.JPEG
            "png" -> PhysicalKind.PNG
            "gif" -> PhysicalKind.GIF
            "webp" -> PhysicalKind.WEBP
            "mp4", "mov", "m4v" -> PhysicalKind.MP4
            "webm" -> PhysicalKind.WEBM
            else -> null
        }

    private fun detectPhysicalKind(bytes: ByteArray): PhysicalKind? {
        if (bytes.isEmpty()) {
            return null
        }
        if (isJpeg(bytes)) {
            return PhysicalKind.JPEG
        }
        if (isPng(bytes)) {
            return PhysicalKind.PNG
        }
        if (isGif(bytes)) {
            return PhysicalKind.GIF
        }
        if (isWebp(bytes)) {
            return PhysicalKind.WEBP
        }
        if (isWebm(bytes)) {
            return PhysicalKind.WEBM
        }
        if (isMp4Family(bytes)) {
            return PhysicalKind.MP4
        }
        return null
    }

    private fun isJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()

    private fun isPng(bytes: ByteArray): Boolean =
        bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() &&
            bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() &&
            bytes[7] == 0x0A.toByte()

    private fun isGif(bytes: ByteArray): Boolean =
        bytes.size >= 6 &&
            bytes[0] == 'G'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == '8'.code.toByte() &&
            (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
            bytes[5] == 'a'.code.toByte()

    private fun isWebp(bytes: ByteArray): Boolean {
        if (bytes.size < 12) {
            return false
        }
        if (!isRiff(bytes)) {
            return false
        }
        return bytes[8] == 'W'.code.toByte() &&
            bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() &&
            bytes[11] == 'P'.code.toByte()
    }

    private fun isRiff(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()

    private fun isWebm(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 0x1A.toByte() &&
            bytes[1] == 0x45.toByte() &&
            bytes[2] == 0xDF.toByte() &&
            bytes[3] == 0xA3.toByte()

    /** ISO BMFF: 4-byte size + `ftyp` (MP4 / M4V / MOV). */
    private fun isMp4Family(bytes: ByteArray): Boolean {
        if (bytes.size < 8) {
            return false
        }
        return bytes[4] == 0x66.toByte() &&
            bytes[5] == 0x74.toByte() &&
            bytes[6] == 0x79.toByte() &&
            bytes[7] == 0x70.toByte()
    }
}
