package com.rupta.imagegenerator.engine

/**
 * Portable request passed to a local inference backend.
 * Backend implementations must not upload prompts or images.
 */
data class GenerationRequest(
    val prompt: String,
    val negativePrompt: String = "",
    val width: Int = 512,
    val height: Int = 512,
    val steps: Int = 20,
    val seed: Long? = null
) {
    init {
        require(prompt.isNotBlank()) { "Prompt must not be blank." }
        require(width in 256..1024 && height in 256..1024) { "Dimensions must be between 256 and 1024." }
        require(width % 8 == 0 && height % 8 == 0) { "Dimensions must be multiples of 8." }
        require(steps in 1..50) { "Steps must be between 1 and 50." }
    }
}

data class GenerationProgress(val step: Int, val totalSteps: Int, val message: String = "") {
    val fraction: Float get() = if (totalSteps <= 0) 0f else (step.coerceAtLeast(0).toFloat() / totalSteps).coerceIn(0f, 1f)
}
