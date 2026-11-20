package org.ftpbdapp.ftpmedia.data

/** A user-added FTP/FTPS server source. Nothing is bundled or pre-filled by the app. */
data class Source(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val rootPath: String,
    val useFtps: Boolean
)
