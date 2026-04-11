package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.MediaTestBytes
import com.NOSQL.NOSQL.model.generated.ActorMediaType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MediaUploadValidatorTest {

    @Test
    fun `jpeg with jpg extension is valid`() {
        assertThat(
            MediaUploadValidator.validate(ActorMediaType.photo, "a.JPG", MediaTestBytes.JPEG)
        ).isNull()
    }

    @Test
    fun `png is valid`() {
        assertThat(
            MediaUploadValidator.validate(ActorMediaType.photo, "x.png", MediaTestBytes.PNG)
        ).isNull()
    }

    @Test
    fun `mp4 is valid`() {
        assertThat(
            MediaUploadValidator.validate(ActorMediaType.video, "c.mp4", MediaTestBytes.MP4)
        ).isNull()
    }

    @Test
    fun `webm is valid`() {
        assertThat(
            MediaUploadValidator.validate(ActorMediaType.video, "w.webm", MediaTestBytes.WEBM)
        ).isNull()
    }

    @Test
    fun `missing file extension is invalid`() {
        assertThat(
            MediaUploadValidator.validate(ActorMediaType.photo, "noext", MediaTestBytes.JPEG)
        ).isEqualTo(MediaUploadValidator.INVALID_MEDIA_EXTENSION)
    }

    @Test
    fun `video extension with photo type is invalid`() {
        assertThat(
            MediaUploadValidator.validate(ActorMediaType.photo, "a.mp4", MediaTestBytes.MP4)
        ).isEqualTo(MediaUploadValidator.INVALID_MEDIA_EXTENSION)
    }

    @Test
    fun `png bytes with jpg name is invalid signature`() {
        assertThat(
            MediaUploadValidator.validate(ActorMediaType.photo, "a.jpg", MediaTestBytes.PNG)
        ).isEqualTo(MediaUploadValidator.INVALID_MEDIA_SIGNATURE)
    }
}
