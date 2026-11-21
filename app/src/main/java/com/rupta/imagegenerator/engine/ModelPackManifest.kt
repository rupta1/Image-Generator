package com.rupta.imagegenerator.engine

import org.json.JSONObject

/** Versioned manifest with optional SHA-256 integrity digests for graph files. */
data class ModelPackManifest(
    val formatVersion: Int,
    val displayName: String,
    val graphs: Map<String, String>,
    val sha256: Map<String, String> = emptyMap()
) {
    companion object {
        fun parse(jsonText: String): ModelPackManifest {
            val root = JSONObject(jsonText)
            val version = root.optInt("formatVersion", 0)
            require(version == 1) { "Unsupported manifest format version." }
            val name = root.optString("displayName").trim()
            require(name.isNotEmpty()) { "Manifest displayName is required." }
            val graphObject = root.optJSONObject("graphs") ?: error("Manifest graphs is required.")
            val graphMap = mutableMapOf<String, String>()
            val keys = graphObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = graphObject.optString(key).trim()
                require(key.matches(Regex("[a-zA-Z0-9_-]+"))) { "Invalid graph identifier." }
                require(value.matches(Regex("[a-zA-Z0-9._-]+")) && value != "." && value != "..") {
                    "Graph paths must be safe plain filenames."
                }
                require(value.lowercase() != "model-manifest.json") { "Graph filename cannot be the manifest." }
                graphMap[key] = value
            }
            require(graphMap.isNotEmpty()) { "Manifest must declare at least one graph." }
            val hashObject = root.optJSONObject("sha256")
            val hashes = mutableMapOf<String, String>()
            if (hashObject != null) {
                val hashKeys = hashObject.keys()
                while (hashKeys.hasNext()) {
                    val key = hashKeys.next()
                    val hash = hashObject.optString(key).trim().lowercase()
                    require(key in graphMap) { "SHA-256 entry must reference a declared graph." }
                    require(hash.matches(Regex("[a-f0-9]{64}"))) { "Invalid SHA-256 digest." }
                    hashes[key] = hash
                }
            }
            return ModelPackManifest(version, name, graphMap, hashes)
        }
    }
}
