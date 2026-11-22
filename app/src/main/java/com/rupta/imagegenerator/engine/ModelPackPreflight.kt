package com.rupta.imagegenerator.engine

import java.io.File

/** Static model-pack preflight. It verifies required pack assets before ONNX sessions are created. */
object ModelPackPreflight {
    data class Report(val checkedFiles: List<String>, val modelFamily: String) {
        val ready: Boolean get() = checkedFiles.isNotEmpty()
    }

    fun inspect(directory: File): Report {
        require(directory.isDirectory) { "Model pack folder is missing." }
        val root = directory.canonicalFile
        fun safeFile(name: String, label: String): File {
            val file = File(directory, name)
            require(file.isFile && file.canonicalFile.parentFile == root) { "$label missing or outside model pack: $name" }
            return file
        }
        val manifest = ModelPackManifest.parse(safeFile("model-manifest.json", "Manifest").readText())
        val contractFile = safeFile("pipeline-contract.json", "Pipeline contract")
        val contract = DiffusionPackContract.parse(contractFile.readText())
        require(manifest.graphs.keys.containsAll(contract.graphInterfaces.keys)) {
            "Manifest does not declare every graph required by the pipeline contract."
        }
        val checked = mutableListOf("model-manifest.json", "pipeline-contract.json")
        manifest.graphs.forEach { (key, filename) ->
            safeFile(filename, "Graph '$key'")
            checked += filename
        }
        contract.tokenizerFiles.values.distinct().forEach { filename ->
            safeFile(filename, "Tokenizer asset")
            checked += filename
        }
        safeFile(contract.schedulerConfig, "Scheduler config")
        checked += contract.schedulerConfig
        return Report(checked.distinct(), contract.modelFamily)
    }
}
