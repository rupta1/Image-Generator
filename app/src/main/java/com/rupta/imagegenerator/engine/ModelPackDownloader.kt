package com.rupta.imagegenerator.engine

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Downloads a user-supplied ZIP pack to app-private temporary storage, then installs it safely. */
object ModelPackDownloader {
    data class Progress(val downloaded: Long, val total: Long) {
        val fraction: Float get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
    }

    fun downloadAndInstall(
        context: Context,
        modelId: String,
        urlText: String,
        expectedSha256: String? = null,
        cancelled: () -> Boolean = { false },
        onProgress: (Progress) -> Unit = {}
    ): File {
        val url = URL(urlText.trim())
        require(url.protocol == "https") { "Downloads must use HTTPS." }
        expectedSha256?.takeIf { it.isNotBlank() }?.let {
            require(it.matches(Regex("[a-fA-F0-9]{64}"))) { "SHA-256 must contain 64 hexadecimal characters." }
        }
        val temp = File(context.cacheDir, "model-pack-download.zip")
        temp.delete()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            connection.connect()
            require(connection.responseCode in 200..299) { "Download server returned HTTP ${connection.responseCode}." }
            val total = connection.contentLengthLong
            var downloaded = 0L
            temp.outputStream().buffered().use { output ->
                connection.inputStream.buffered().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        if (cancelled()) throw InterruptedException("Download cancelled.")
                        val read = input.read(buffer)
                        if (read < 0) break
                        downloaded += read
                        output.write(buffer, 0, read)
                        onProgress(Progress(downloaded, total))
                    }
                }
            }
            require(temp.length() > 0) { "Downloaded file is empty." }
            expectedSha256?.takeIf { it.isNotBlank() }?.let { expected ->
                require(sha256(temp).equals(expected, ignoreCase = true)) { "Download SHA-256 mismatch." }
            }
            return temp.inputStream().use { ModelPackInstaller.installZip(context, modelId, it) }
        } catch (t: Throwable) {
            temp.delete()
            throw t
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
