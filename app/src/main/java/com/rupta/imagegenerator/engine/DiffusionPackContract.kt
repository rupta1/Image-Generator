package com.rupta.imagegenerator.engine

import org.json.JSONObject

/**
 * Declarative contract for a Stable-Diffusion-style ONNX pack.
 * This validates pack metadata and graph interfaces before pipeline wiring.
 * It does not itself execute diffusion.
 */
data class DiffusionPackContract(
    val formatVersion: Int,
    val modelFamily: String,
    val latentChannels: Int,
    val vaeScale: Int,
    val maxTokenLength: Int,
    val tokenizerFiles: Map<String, String>,
    val schedulerConfig: String,
    val graphInterfaces: Map<String, GraphInterfaceContract>
) {
    companion object {
        private val requiredGraphs = setOf("text_encoder", "unet", "vae_decoder")

        fun parse(json: String): DiffusionPackContract {
            val root = JSONObject(json)
            require(root.optInt("formatVersion", 0) == 1) { "Unsupported pipeline contract version." }
            val family = root.optString("modelFamily").trim()
            require(family in setOf("sd15", "sdxl_base")) { "Unsupported model family." }
            val latentChannels = root.optInt("latentChannels", 0)
            require(latentChannels == 4) { "Currently supported packs must use 4 latent channels." }
            val scale = root.optInt("vaeScale", 0)
            require(scale in setOf(4, 8)) { "VAE scale must be 4 or 8." }
            val tokenLength = root.optInt("maxTokenLength", 0)
            require(tokenLength in 1..256) { "Invalid maximum token length." }

            val tokenizer = root.optJSONObject("tokenizerFiles") ?: error("tokenizerFiles is required.")
            val tokenizerFiles = mutableMapOf<String, String>()
            val tokenizerKeys = tokenizer.keys()
            while (tokenizerKeys.hasNext()) {
                val key = tokenizerKeys.next()
                val file = tokenizer.optString(key).trim()
                require(key.matches(Regex("[a-zA-Z0-9_-]+"))) { "Invalid tokenizer asset key." }
                require(file.matches(Regex("[a-zA-Z0-9._-]+")) && file != "." && file != "..") {
                    "Tokenizer assets must use safe flat filenames."
                }
                tokenizerFiles[key] = file
            }
            require(tokenizerFiles.keys.containsAll(setOf("vocab", "mergesOrConfig"))) {
                "Tokenizer must declare vocab and mergesOrConfig assets."
            }

            val scheduler = root.optString("schedulerConfig").trim()
            require(scheduler.matches(Regex("[a-zA-Z0-9._-]+")) && scheduler != "." && scheduler != "..") {
                "Scheduler config must be a safe filename."
            }

            val interfaces = root.optJSONObject("graphInterfaces") ?: error("graphInterfaces is required.")
            val contracts = mutableMapOf<String, GraphInterfaceContract>()
            val interfaceKeys = interfaces.keys()
            while (interfaceKeys.hasNext()) {
                val graph = interfaceKeys.next()
                require(graph in requiredGraphs) { "Unknown graph interface: $graph" }
                contracts[graph] = GraphInterfaceContract.parse(interfaces.getJSONObject(graph))
            }
            require(contracts.keys.containsAll(requiredGraphs)) { "Contract must define text_encoder, unet and vae_decoder." }
            return DiffusionPackContract(1, family, latentChannels, scale, tokenLength, tokenizerFiles, scheduler, contracts)
        }
    }
}

data class TensorSpec(val name: String, val dimensions: List<Int>, val dataType: String) {
    init {
        require(name.matches(Regex("[a-zA-Z0-9_.-]+"))) { "Invalid tensor name." }
        require(dimensions.isNotEmpty() && dimensions.all { it >= -1 }) { "Invalid tensor dimensions." }
        require(dataType in setOf("float32", "float16", "int32", "int64")) { "Unsupported tensor data type." }
    }

    fun validateShape(actual: LongArray): Boolean =
        actual.size == dimensions.size && dimensions.indices.all { dimensions[it] == -1 || dimensions[it].toLong() == actual[it] }
}

data class GraphInterfaceContract(val inputs: List<TensorSpec>, val outputs: List<TensorSpec>) {
    companion object {
        fun parse(json: JSONObject): GraphInterfaceContract {
            return GraphInterfaceContract(parseSpecs(json.optJSONArray("inputs"), "inputs"),
                parseSpecs(json.optJSONArray("outputs"), "outputs"))
        }

        private fun parseSpecs(array: org.json.JSONArray?, label: String): List<TensorSpec> {
            require(array != null && array.length() > 0) { "Graph $label must not be empty." }
            return (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val dims = item.getJSONArray("dimensions")
                val shape = (0 until dims.length()).map { dims.getInt(it) }
                TensorSpec(item.getString("name"), shape, item.getString("dataType"))
            }.also { specs ->
                require(specs.map { it.name }.distinct().size == specs.size) { "Duplicate tensor name in $label." }
            }
        }
    }
}
