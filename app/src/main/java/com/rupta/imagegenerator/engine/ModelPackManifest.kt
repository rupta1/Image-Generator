package com.rupta.imagegenerator.engine

import org.json.JSONObject

/** Basic schema validation for model-pack manifests before loading graph files. */
data class ModelPackManifest(
    val formatVersion: Int,
    val displayName: String,
    val graphs: Map<String, String>
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
                require(value.isNotEmpty() && value == value.substringAfterLast('/')) { "Graph paths must be plain filenames." }
                graphMap[key] = value
            }
            require(graphMap.isNotEmpty()) { "Manifest must declare at least one graph." }
            return ModelPackManifest(version, name, graphMap)
        }
    }
}
