package com.rupta.imagegenerator.engine

import android.content.Context
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Safely installs a user-selected ZIP model pack into app-private storage.
 * ZIP entries are restricted to flat filenames; unpacked size and entry count are bounded.
 */
object ModelPackInstaller {
    private const val MAX_UNPACKED_BYTES = 6L * 1024L * 1024L * 1024L
    private const val MAX_ENTRIES = 128

    fun installZip(context: Context, modelId: String, input: InputStream): File {
        val destination = ModelCatalog.directory(context, modelId)
        val parent = destination.parentFile ?: error("Invalid destination.")
        val staging = File(parent, ".$modelId-installing")
        require(!destination.exists()) { "A model with this ID is already installed. Remove it first." }
        staging.deleteRecursively()
        require(staging.mkdirs()) { "Could not create staging folder." }
        var total = 0L
        var count = 0
        try {
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    count++
                    require(count <= MAX_ENTRIES) { "Model pack has too many entries." }
                    if (!entry.isDirectory) {
                        val name = entry.name
                        require(name.matches(Regex("[a-zA-Z0-9._-]+")) && name != "." && name != "..") {
                            "ZIP entries must be flat safe filenames."
                        }
                        val outFile = File(staging, name)
                        require(outFile.canonicalFile.parentFile == staging.canonicalFile) { "Unsafe ZIP entry." }
                        outFile.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = zip.read(buffer)
                                if (read < 0) break
                                total += read
                                require(total <= MAX_UNPACKED_BYTES) { "Model pack exceeds unpacked size limit." }
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
            val manifestFile = File(staging, "model-manifest.json")
            require(manifestFile.isFile) { "ZIP is missing model-manifest.json." }
            val manifest = ModelPackManifest.parse(manifestFile.readText())
            manifest.graphs.forEach { (key, filename) ->
                val graph = File(staging, filename)
                require(graph.isFile) { "Missing graph file: $filename" }
                manifest.sha256[key]?.let { expected ->
                    require(sha256(graph) == expected) { "SHA-256 verification failed for $filename." }
                }
            }
            ModelPackPreflight.inspect(staging)
            require(staging.renameTo(destination)) { "Could not finalize model installation." }
            return destination
        } catch (t: Throwable) {
            staging.deleteRecursively()
            throw t
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
