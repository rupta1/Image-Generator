package com.rupta.imagegenerator.engine

import android.content.Context
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.security.MessageDigest

/** Loads model graph sessions using conservative CPU and memory settings for mobile devices. */
class OnnxModelPack private constructor(
    private val environment: OrtEnvironment,
    val sessions: Map<String, OrtSession>,
    val directory: File,
    val manifest: ModelPackManifest
) : AutoCloseable {
    override fun close() { sessions.values.forEach { runCatching { it.close() } } }

    companion object {
        fun load(context: Context, directory: File): OnnxModelPack {
            val manifestFile = File(directory, "model-manifest.json")
            require(directory.isDirectory && manifestFile.isFile) { "Model folder or manifest is missing." }
            val manifest = ModelPackManifest.parse(manifestFile.readText())
            val canonicalDir = directory.canonicalFile
            val env = OrtEnvironment.getEnvironment()
            val loaded = linkedMapOf<String, OrtSession>()
            try {
                manifest.graphs.forEach { (key, fileName) ->
                    val graphFile = File(directory, fileName)
                    require(graphFile.isFile && graphFile.canonicalFile.parentFile == canonicalDir) {
                        "Missing graph file or graph outside model folder: $fileName"
                    }
                    manifest.sha256[key]?.let { expected ->
                        val actual = sha256(graphFile)
                        require(actual.equals(expected, ignoreCase = true)) { "SHA-256 verification failed for $fileName." }
                    }
                    loaded[key] = OrtSession.SessionOptions().use { options ->
                        options.setIntraOpNumThreads(1)
                        options.setInterOpNumThreads(1)
                        options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
                        options.setMemoryPatternOptimization(false)
                        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
                        env.createSession(graphFile.absolutePath, options)
                    }
                }
                return OnnxModelPack(env, loaded, directory, manifest)
            } catch (t: Throwable) {
                loaded.values.forEach { runCatching { it.close() } }
                throw t
            }
        }
        private fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}