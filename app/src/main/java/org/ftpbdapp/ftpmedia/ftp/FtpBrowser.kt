package org.ftpbdapp.ftpmedia.ftp

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import org.ftpbdapp.ftpmedia.data.Source
import java.io.IOException

/** Lists directories and tests connectivity for a user-added source. */
object FtpBrowser {

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val SOCKET_TIMEOUT_MS = 20_000

    fun list(source: Source, path: String): List<FtpEntry> {
        val client = connect(source)
        try {
            val dir = if (path.isBlank()) "/" else path
            val files: Array<FTPFile> = client.listFiles(dir)
                ?: throw IOException("Server returned an empty response for '$dir'")
            return files
                .filter { it.name != "." && it.name != ".." }
                .map { f ->
                    val child = if (dir.endsWith("/")) dir + f.name else "$dir/${f.name}"
                    FtpEntry(
                        name = f.name,
                        path = child,
                        isDirectory = f.isDirectory,
                        size = if (f.isDirectory) -1L else f.size,
                        modifiedMillis = f.timestamp?.timeInMillis ?: -1L
                    )
                }
        } finally {
            runCatching { client.logout() }
            runCatching { client.disconnect() }
        }
    }

    /** Human-readable connectivity check used by the "Test" button. */
    fun test(source: Source): String {
        val root = source.rootPath.ifBlank { "/" }
        return try {
            val entries = list(source, root)
            "Connected OK — ${entries.size} item(s) in $root"
        } catch (e: Exception) {
            "Connection failed: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun connect(source: Source): FTPClient {
        val host = source.host.trim()
        if (host.isEmpty()) throw IOException("Host is empty")
        val client: FTPClient = if (source.useFtps) FTPSClient("TLS") else FTPClient()
        client.connectTimeout = CONNECT_TIMEOUT_MS
        client.setDefaultTimeout(CONNECT_TIMEOUT_MS)
        client.setControlEncoding("UTF-8")
        client.connect(host, source.port)
        if (!FTPReply.isPositiveCompletion(client.replyCode)) {
            client.runCatching { disconnect() }
            throw IOException("Server refused FTP connection (reply ${client.replyCode})")
        }
        val user = source.username.ifBlank { "anonymous" }
        val pass = source.password.ifBlank { "anonymous@" }
        if (!client.login(user, pass)) {
            client.runCatching { disconnect() }
            throw IOException("Login failed for user '$user'")
        }
        client.enterLocalPassiveMode()
        client.setFileType(FTPClient.BINARY_FILE_TYPE)
        client.setSoTimeout(SOCKET_TIMEOUT_MS)
        return client
    }
}
