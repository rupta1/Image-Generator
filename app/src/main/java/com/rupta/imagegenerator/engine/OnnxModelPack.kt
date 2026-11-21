package com.rupta.imagegenerator.engine

import android.content.Context
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File

/**
 * ONNX model-pack session loader. Opens graph files only; it does not execute a complete
 * diffusion pipeline (tokenizer, scheduler, conditioning and VAE orchestration).
 */
class OnnxModelPack private constructor(
    private val environment: OrtEnvironment,
    val sessions: Map<String, OrtSession>,
    val directory: File,
    val manifest: ModelPackManifest
) : AutoCloseable {
    override fun close() {
        sessions.values.forEach { it.close() }
    }

    companion object {
        fun load(context: Context, directory: File): OnnxModelPack {
            val manifestFile = File(directory, "model-manifest.json")
            require(directory.isDirectory && manifestFile.isFile) { "Model folder or manifest is missing." }
            val manifest = ModelPackManifest.parse(manifestFile.readText())
            val env = OrtEnvironment.getEnvironment()
            val loaded = linkedMapOf<String, OrtSession>()
            try {
                manifest.graphs.forEach { (key, fileName) ->
                    val graphFile = File(directory, fileName)
                    require(graphFile.isFile) { "Missing graph file: $fileName" }
                    loaded[key] = OrtSession.SessionOptions().use { options ->
                        env.createSession(graphFile.absolutePath, options)
                    }
                }
                return OnnxModelPack(env, loaded, directory, manifest)
            } catch (t: Throwable) {
                loaded.values.forEach { runCatching { it.close() } }
                throw t
            }
        }
    }
}
