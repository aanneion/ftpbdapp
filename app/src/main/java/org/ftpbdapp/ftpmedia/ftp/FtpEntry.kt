package org.ftpbdapp.ftpmedia.ftp

/** One entry in a remote FTP directory listing. */
data class FtpEntry(
    val name: String,
    /** Absolute, decoded path on the server, e.g. "/movies/Some File.mkv". Always starts with '/'. */
    val path: String,
    val isDirectory: Boolean,
    /** File size in bytes, or -1 when unknown / not applicable (folders). */
    val size: Long,
    /** Last-modified time in millis since epoch, or -1 when unknown. */
    val modifiedMillis: Long
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()
}

object MediaTypes {
    private val video = setOf(
        "mp4", "mkv", "avi", "mov", "webm", "m4v", "mpg", "mpeg",
        "wmv", "flv", "ts", "m2ts", "3gp"
    )
    private val audio = setOf(
        "mp3", "flac", "aac", "m4a", "ogg", "wav", "wma", "opus"
    )

    fun isVideo(name: String) = extensionOf(name) in video
    fun isAudio(name: String) = extensionOf(name) in audio
    fun isMedia(name: String) = isVideo(name) || isAudio(name)

    private fun extensionOf(name: String) =
        name.substringAfterLast('.', "").lowercase()
}
