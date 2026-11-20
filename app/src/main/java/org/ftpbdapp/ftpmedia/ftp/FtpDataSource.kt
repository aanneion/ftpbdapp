package org.ftpbdapp.ftpmedia.ftp

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import org.ftpbdapp.ftpmedia.data.Source
import java.io.IOException
import java.io.InputStream

/** Builds the internal URI handed to ExoPlayer, e.g. ftp://host:21/dir/Some%20File.mkv */
fun buildFtpUri(source: Source, path: String): Uri {
    val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
    val encodedPath = segments.joinToString("/", prefix = "/") { Uri.encode(it) }
    val scheme = if (source.useFtps) "ftps" else "ftp"
    return Uri.parse("$scheme://${source.host.trim()}:${source.port}$encodedPath")
}

/**
 * Media3 DataSource that streams a file straight from FTP/FTPS.
 * Uses REST (restart offset) so seeking backwards/forwards works on servers that
 * support it (most do). File size is discovered via MLSD for accurate progress.
 */
class FtpDataSource(private val source: Source) : BaseDataSource(/* isNetwork = */ true) {

    private var ftp: FTPClient? = null
    private var stream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()

    override fun open(spec: DataSpec): Long {
        transferInitializing(spec)
        uri = spec.uri
        val path = spec.uri.path ?: "/"
        val client = connect(path)
        try {
            // Best-effort size lookup for duration/seek accuracy.
            var totalSize: Long = C.LENGTH_UNSET.toLong()
            try {
                val file: FTPFile? = client.mlistFile(path)
                if (file != null && file.size >= 0) totalSize = file.size
            } catch (_: Exception) {
                // MLSD unsupported — playback still works, seeking is less precise.
            }

            val position = spec.position
            if (position > 0) {
                // Request server-side seek into the file.
                client.setRestartOffset(position)
            }

            val input = client.retrieveFileStream(path)
                ?: throw IOException("Server refused the data connection for '$path'")

            ftp = client
            stream = input
            bytesRemaining = when {
                spec.length != C.LENGTH_UNSET.toLong() -> spec.length
                totalSize != C.LENGTH_UNSET.toLong() -> (totalSize - position).coerceAtLeast(0L)
                else -> C.LENGTH_UNSET.toLong()
            }
            transferStarted(spec)
            return bytesRemaining
        } catch (e: Exception) {
            runCatching { client.disconnect() }
            if (e is IOException) throw e
            throw IOException(e.message, e)
        }
    }

    override fun getUri(): Uri? = uri

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val toRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            minOf(length.toLong(), bytesRemaining).toInt()
        }
        val input = stream ?: return C.RESULT_END_OF_INPUT
        val read = input.read(buffer, offset, toRead)
        if (read == -1) return C.RESULT_END_OF_INPUT
        bytesTransferred(read)
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read
        return read
    }

    override fun close() {
        try {
            runCatching { stream?.close() }
            ftp?.let { client ->
                runCatching { client.completePendingCommand() }
                runCatching { client.logout() }
                runCatching { client.disconnect() }
            }
        } finally {
            stream = null
            ftp = null
            uri = null
            bytesRemaining = C.LENGTH_UNSET.toLong()
            transferEnded()
        }
    }

    private fun connect(path: String): FTPClient {
        val client: FTPClient = if (source.useFtps) FTPSClient("TLS") else FTPClient()
        try {
            client.connectTimeout = 10_000
            client.setControlEncoding("UTF-8")
            client.connect(source.host.trim(), source.port)
            if (!FTPReply.isPositiveCompletion(client.replyCode)) {
                throw IOException("FTP connect failed (reply ${client.replyCode})")
            }
            val user = source.username.ifBlank { "anonymous" }
            val pass = source.password.ifBlank { "anonymous@" }
            if (!client.login(user, pass)) {
                throw IOException("FTP login failed for user '$user'")
            }
            client.enterLocalPassiveMode()
            client.setFileType(FTPClient.BINARY_FILE_TYPE)
            client.setSoTimeout(30_000)
            return client
        } catch (e: Exception) {
            runCatching { client.disconnect() }
            if (e is IOException) throw e
            throw IOException("Cannot open '$path': ${e.message}", e)
        }
    }
}

class FtpDataSourceFactory(private val source: Source) : DataSource.Factory {
    override fun createDataSource(): DataSource = FtpDataSource(source)
}
