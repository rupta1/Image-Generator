package com.rupta.imagegenerator.engine

import android.content.Context
import java.io.File

enum class ModelFamily { SD15, SDXL, OTHER }

data class ModelDescriptor(
    val id: String,
    val name: String,
    val family: ModelFamily,
    val description: String,
    val requiredFiles: List<String>,
    val approximateStorageBytes: Long,
    val licenseUrl: String,
    val sourceUrl: String? = null
)

data class InstalledModel(val descriptor: ModelDescriptor, val directory: File)

/**
 * Model catalog deliberately contains no model binaries or unverified download URLs.
 * Users/importers must provide model files and confirm the model's licence.
 */
object ModelCatalog {
    val entries: List<ModelDescriptor> = listOf(
        ModelDescriptor(
            id = "sd15-onnx-mobile",
            name = "Stable Diffusion 1.5 (ONNX pack)",
            family = ModelFamily.SD15,
            description = "A potential mobile-compatible model pack. A validated runtime pipeline is still required.",
            requiredFiles = listOf("model-manifest.json"),
            approximateStorageBytes = 2_000_000_000L,
            licenseUrl = "https://huggingface.co/runwayml/stable-diffusion-v1-5"
        )
    )

    fun root(context: Context): File = File(context.filesDir, "models").apply { mkdirs() }

    fun directory(context: Context, modelId: String): File {
        require(modelId.matches(Regex("[a-zA-Z0-9._-]+"))) { "Invalid model id." }
        return File(root(context), modelId)
    }

    fun installed(context: Context): List<InstalledModel> = entries.mapNotNull { descriptor ->
        val dir = directory(context, descriptor.id)
        if (dir.exists() && dir.isDirectory && descriptor.requiredFiles.all { File(dir, it).isFile }) {
            InstalledModel(descriptor, dir)
        } else null
    }

    fun availableBytes(context: Context): Long = root(context).usableSpace
}
