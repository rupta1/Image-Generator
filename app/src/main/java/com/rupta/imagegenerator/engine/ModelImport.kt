package com.rupta.imagegenerator.engine

import android.content.Context
import java.io.File
import java.io.InputStream

/** Copies an explicitly selected manifest into a model folder without accepting path traversal. */
object ModelImport {
    fun importManifest(context: Context, modelId: String, input: InputStream): File {
        val targetDir = ModelCatalog.directory(context, modelId)
        targetDir.mkdirs()
        val target = File(targetDir, "model-manifest.json")
        input.use { source -> target.outputStream().use { sink -> source.copyTo(sink) } }
        return target
    }

    fun deleteModel(context: Context, modelId: String): Boolean {
        val directory = ModelCatalog.directory(context, modelId)
        if (!directory.exists()) return true
        return directory.deleteRecursively()
    }
}
