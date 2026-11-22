package com.rupta.imagegenerator.engine

import kotlin.random.Random

/** Produces reproducible float32 Gaussian latents for the requested seed. */
object LatentNoiseFactory {
    fun create(batch: Int = 1, channels: Int = 4, height: Int, width: Int, seed: Long): FloatTensorData {
        require(batch > 0 && channels > 0 && height > 0 && width > 0)
        val count = Math.multiplyExact(Math.multiplyExact(Math.multiplyExact(batch, channels), height), width)
        val random = Random(seed)
        val values = FloatArray(count)
        var i = 0
        while (i < count) {
            val u1 = random.nextDouble().coerceAtLeast(1e-12)
            val u2 = random.nextDouble()
            val radius = kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1))
            val angle = 2.0 * Math.PI * u2
            values[i++] = (radius * kotlin.math.cos(angle)).toFloat()
            if (i < count) values[i++] = (radius * kotlin.math.sin(angle)).toFloat()
        }
        return FloatTensorData(longArrayOf(batch.toLong(), channels.toLong(), height.toLong(), width.toLong()), values)
    }
}
