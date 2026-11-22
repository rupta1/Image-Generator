package com.rupta.imagegenerator.engine

/**
 * Core DDIM denoising orchestration. Runtime graph adapters supply text conditioning
 * and UNet prediction; this class owns latent evolution, classifier-free guidance,
 * progress callbacks and cancellation.
 */
class DenoisingLoop(private val scheduler: DdimScheduler = DdimScheduler()) {
    fun run(
        initialLatents: FloatTensorData,
        positiveConditioning: FloatTensorData,
        negativeConditioning: FloatTensorData,
        steps: Int,
        guidanceScale: Float,
        predictNoise: (sample: FloatTensorData, timestep: Int, conditioning: FloatTensorData) -> FloatTensorData,
        onProgress: (GenerationProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): FloatTensorData {
        require(initialLatents.shape.size == 4 && initialLatents.shape[0] == 1L) {
            "Latents must have shape [1, channels, height, width]."
        }
        require(positiveConditioning.shape.contentEquals(negativeConditioning.shape)) {
            "Positive and negative conditioning shapes must match."
        }
        require(positiveConditioning.shape.firstOrNull() == 1L) { "Conditioning tensors must have batch size one." }
        require(guidanceScale.isFinite() && guidanceScale >= 0f) { "Guidance scale must be finite and non-negative." }
        require(steps in 1..50) { "Steps must be between 1 and 50." }

        val cfgConditioning = concatBatch(negativeConditioning, positiveConditioning)
        var latents = initialLatents
        val timesteps = scheduler.timesteps(steps)
        for (i in timesteps.indices) {
            if (isCancelled()) throw java.util.concurrent.CancellationException("Image generation cancelled.")
            val timestep = timesteps[i]
            val previous = if (i == timesteps.lastIndex) -1 else timesteps[i + 1]
            val doubled = concatBatch(latents, latents)
            val modelPrediction = predictNoise(doubled, timestep, cfgConditioning)
            val guided = applyGuidance(modelPrediction, guidanceScale)
            latents = scheduler.step(latents, guided, timestep, previous)
            onProgress(GenerationProgress(i + 1, timesteps.size, "Denoising ${i + 1}/${timesteps.size}"))
        }
        return latents
    }

    private fun applyGuidance(prediction: FloatTensorData, scale: Float): FloatTensorData {
        require(prediction.shape.size == 4 && prediction.shape[0] == 2L) {
            "UNet CFG prediction must have batch size two."
        }
        val singleShape = prediction.shape.copyOf().also { it[0] = 1L }
        val perBatch = prediction.values.size / 2
        val values = FloatArray(perBatch) { i ->
            val unconditional = prediction.values[i]
            val conditional = prediction.values[perBatch + i]
            unconditional + scale * (conditional - unconditional)
        }
        return FloatTensorData(singleShape, values)
    }

    private fun concatBatch(a: FloatTensorData, b: FloatTensorData): FloatTensorData {
        require(a.shape.size == b.shape.size && a.shape.indices.drop(1).all { a.shape[it] == b.shape[it] }) {
            "Tensor shapes must match except batch dimension."
        }
        require(a.shape[0] == 1L && b.shape[0] == 1L) { "Only single-item tensors can be batch-concatenated." }
        val shape = a.shape.copyOf().also { it[0] = 2L }
        return FloatTensorData(shape, a.values + b.values)
    }
}
