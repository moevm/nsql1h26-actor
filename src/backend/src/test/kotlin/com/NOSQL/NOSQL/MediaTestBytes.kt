package com.NOSQL.NOSQL

/**
 * Minimal valid file headers for media upload tests.
 */
object MediaTestBytes {

    /** JPEG: SOI + minimal JFIF-like segment and EOI. */
    val JPEG: ByteArray =
        byteArrayOf(
            0xFF.toByte(),
            0xD8.toByte(),
            0xFF.toByte(),
            0xE0.toByte(),
            0x00,
            0x10,
            0x4A,
            0x46,
            0x49,
            0x46,
            0x00,
            0x01,
            0x01,
            0x00,
            0x00,
            0x01,
            0x00,
            0x01,
            0x00,
            0x00,
            0xFF.toByte(),
            0xD9.toByte(),
        )

    val PNG: ByteArray =
        byteArrayOf(
            0x89.toByte(),
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A,
            0x00,
            0x00,
            0x00,
            0x00,
        )

    /** MP4/MOV: `ftyp` box (isom brand). */
    val MP4: ByteArray =
        byteArrayOf(
            0x00,
            0x00,
            0x00,
            0x18,
            0x66,
            0x74,
            0x79,
            0x70,
            0x69,
            0x73,
            0x6F,
            0x6D,
            0x00,
            0x00,
            0x00,
            0x00,
        )

    val WEBM: ByteArray =
        byteArrayOf(
            0x1A,
            0x45,
            0xDF.toByte(),
            0xA3.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
        )
}
