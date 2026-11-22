package com.rupta.imagegenerator.engine

import android.graphics.Bitmap
import java.security.SecureRandom

/**
 * End-to-end pipeline coordinator. Runtime-specific graph sessions are supplied by callbacks
 * so orchestration can be verified independently of ONNX model binaries and Android hardware.
 */
class DiffusionPipeline(
    private val tokenizer: ClipBpeTokenizer,
    private val encodeText: (LongArray) -> FloatTensorData,
    private val predictNoise: (FloatTensorData, Int, FloatTensorData) -> FloatTensorData,
    private val decodeLatents: (FloatTensorData) -> Bitmap,
    private val latentChannels: Int = 4,
    private val vaeScale: Int = 8,
    private val scheduler: DdimScheduler = DdimScheduler()
) {
    init {
        require(latentChannels > 0)
        require(vaeScale in 1..16)
    }

    fun generate(
        request: GenerationRequest,
        onProgress: (GenerationProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): Bitmap {
        val seed = request.seed ?: SecureRandom().nextLong()
        val latentWidth = request.width / vaeScale
        val latentHeight = request.height / vaeScale
        require(request.width % vaeScale == 0 && request.height % vaeScale == 0) {
            "Image dimensions must be divisible by the pack VAE scale."
        }

        onProgress(GenerationProgress(0, request.steps, "Encoding prompts"))
        if (isCancelled()) throw java.util.concurrent.CancellationException("Image generation cancelled.")
        val positive = encodeText(tokenizer.encode(request.prompt))
        val negative = encodeText(tokenizer.encode(request.negativePrompt))
        require(positive.shape.contentEquals(negative.shape)) {
            "Positive and negative text-conditioning shapes must match."
        }

        val latents = LatentNoiseFactory.create(
            batch = 1, channels = latentChannels, height = latentHeight, width = latentWidth, seed = seed
        )
        val denoised = DenoisingLoop(scheduler).run(
            initialLatents = latents,
            positiveConditioning = positive,
            negativeConditioning = negative,
            steps = request.steps,
            guidanceScale = 7.5f,
            predictNoise = predictNoise,
            onProgress = onProgress,
            isCancelled = isCancelled
        )
        if (isCancelled()) throw java.util.concurrent.CancellationException("Image generation cancelled.")
        onProgress(GenerationProgress(request.steps, request.steps, "Decoding image"))
        return decodeLatents(denoised)
    }
}
