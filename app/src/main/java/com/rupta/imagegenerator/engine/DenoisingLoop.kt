package com.rupta.imagegenerator.engine

/** Memory-conscious DDIM denoising using two sequential batch-one UNet passes for CFG. */
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

        var latents = initialLatents
        val timesteps = scheduler.timesteps(steps)
        for (i in timesteps.indices) {
            if (isCancelled()) throw java.util.concurrent.CancellationException("Image generation cancelled.")
            val timestep = timesteps[i]
            val previous = if (i == timesteps.lastIndex) -1 else timesteps[i + 1]
            val unconditional = predictNoise(latents, timestep, negativeConditioning)
            if (isCancelled()) throw java.util.concurrent.CancellationException("Image generation cancelled.")
            val conditional = predictNoise(latents, timestep, positiveConditioning)
            val guided = applyGuidance(unconditional, conditional, guidanceScale)
            latents = scheduler.step(latents, guided, timestep, previous)
            onProgress(GenerationProgress(i + 1, timesteps.size, "Denoising " + (i + 1) + "/" + timesteps.size))
        }
        return latents
    }

    private fun applyGuidance(unconditional: FloatTensorData, conditional: FloatTensorData, scale: Float): FloatTensorData {
        require(unconditional.shape.contentEquals(conditional.shape) &&
            unconditional.shape.size == 4 && unconditional.shape[0] == 1L) {
            "Both guidance predictions must have identical batch-one shapes."
        }
        val values = FloatArray(unconditional.values.size) { i ->
            unconditional.values[i] + scale * (conditional.values[i] - unconditional.values[i])
        }
        return FloatTensorData(unconditional.shape.copyOf(), values)
    }
}
