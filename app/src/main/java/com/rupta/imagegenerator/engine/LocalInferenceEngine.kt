package com.rupta.imagegenerator.engine

import android.graphics.Bitmap
import java.io.Closeable

/**
 * Interface for strictly on-device image inference.
 * Concrete backends should be added only after their model/runtime compatibility is verified.
 */
interface LocalInferenceEngine : Closeable {
    val backendName: String
    suspend fun load(model: InstalledModel, onProgress: (GenerationProgress) -> Unit = {})
    suspend fun generate(request: GenerationRequest, onProgress: (GenerationProgress) -> Unit = {}): Bitmap
    fun cancel()
    override fun close()
}

class InferenceUnavailableException(message: String) : IllegalStateException(message)

/** Honest stub until a supported inference runtime and model pipeline are integrated. */
class UnavailableInferenceEngine : LocalInferenceEngine {
    override val backendName: String = "Not installed"
    override suspend fun load(model: InstalledModel, onProgress: (GenerationProgress) -> Unit) {
        throw InferenceUnavailableException("No compatible local inference backend is installed yet.")
    }
    override suspend fun generate(request: GenerationRequest, onProgress: (GenerationProgress) -> Unit): Bitmap {
        throw InferenceUnavailableException("Image generation is not available until a model and inference backend are installed.")
    }
    override fun cancel() = Unit
    override fun close() = Unit
}
