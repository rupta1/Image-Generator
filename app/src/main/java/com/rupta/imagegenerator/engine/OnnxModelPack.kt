package com.rupta.imagegenerator.engine

import android.content.Context
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File

/**
 * ONNX model-pack session loader. This opens graph files only; it does not yet execute
 * a complete diffusion pipeline (tokenizer, scheduler, conditioning and VAE orchestration).
 * Manifest format: JSON object with a "graphs" object mapping logical names to file names.
 */
class OnnxModelPack private constructor(
    private val environment: OrtEnvironment,
    val sessions: Map<String, OrtSession>,
    val directory: File
) : AutoCloseable {
    override fun close() {
        sessions.values.forEach { it.close() }
    }

    companion object {
        fun load(context: Context, directory: File): OnnxModelPack {
            val manifest = File(directory, "model-manifest.json")
            require(directory.isDirectory && manifest.isFile) { "Model folder or manifest is missing." }
            val json = org.json.JSONObject(manifest.readText())
            val graphs = json.optJSONObject("graphs") ?: throw IllegalArgumentException("Manifest must include a graphs object.")
            require(graphs.length() > 0) { "No graph entries found." }
            val env = OrtEnvironment.getEnvironment()
            val loaded = linkedMapOf<String, OrtSession>()
            try {
                val keys = graphs.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val fileName = graphs.optString(key)
                    require(fileName.isNotBlank() && File(fileName).name == fileName) { "Invalid graph filename for $key." }
                    val graphFile = File(directory, fileName)
                    require(graphFile.isFile) { "Missing graph file: $fileName" }
                    loaded[key] = OrtSession.SessionOptions().use { options -> env.createSession(graphFile.absolutePath, options) }
                }
                return OnnxModelPack(env, loaded, directory)
            } catch (t: Throwable) {
                loaded.values.forEach { runCatching { it.close() } }
                throw t
            }
        }
    }
}
