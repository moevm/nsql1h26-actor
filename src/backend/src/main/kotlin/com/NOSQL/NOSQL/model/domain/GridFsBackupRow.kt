package com.NOSQL.NOSQL.model.domain

/** Одна строка экспорта GridFS до кодирования в Base64. */
data class GridFsBackupRow(
    val id: String,
    val actorId: String,
    val filename: String,
    val contentType: String?,
    val mediaType: String,
    val caption: String?,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GridFsBackupRow
        if (id != other.id) return false
        if (actorId != other.actorId) return false
        if (filename != other.filename) return false
        if (contentType != other.contentType) return false
        if (mediaType != other.mediaType) return false
        if (caption != other.caption) return false
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + actorId.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + mediaType.hashCode()
        result = 31 * result + (caption?.hashCode() ?: 0)
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
