package com.rupta.imagegenerator.engine

import java.io.File

/** Loads the contract and tokenizer assets from a validated, app-private model-pack folder. */
data class LoadedTokenizerPack(
    val contract: DiffusionPackContract,
    val tokenizer: ClipBpeTokenizer
) {
    companion object {
        fun load(directory: File, contractFileName: String = "pipeline-contract.json"): LoadedTokenizerPack {
            val canonicalDir = directory.canonicalFile
            require(directory.isDirectory) { "Model pack folder is missing." }
            val contractFile = File(directory, contractFileName)
            require(contractFile.isFile && contractFile.canonicalFile.parentFile == canonicalDir) {
                "Pipeline contract file missing or outside pack folder."
            }
            val contract = DiffusionPackContract.parse(contractFile.readText())
            fun safeAsset(key: String): File {
                val filename = contract.tokenizerFiles[key] ?: error("Tokenizer asset '$key' is undeclared.")
                val asset = File(directory, filename)
                require(asset.isFile && asset.canonicalFile.parentFile == canonicalDir) {
                    "Tokenizer asset missing or outside model pack: $filename"
                }
                return asset
            }
            val vocab = safeAsset("vocab")
            val mergeAsset = safeAsset("mergesOrConfig")
            val tokenizer = ClipBpeTokenizer.fromFiles(vocab, mergeAsset, contract.maxTokenLength)
            return LoadedTokenizerPack(contract, tokenizer)
        }
    }
}
