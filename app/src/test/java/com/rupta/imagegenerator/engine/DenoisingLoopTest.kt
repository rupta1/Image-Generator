package com.rupta.imagegenerator.engine

import org.junit.Assert.*
import org.junit.Test

class DenoisingLoopTest {
    private fun tensor(shape: LongArray, values: FloatArray) = FloatTensorData(shape, values)

    @Test fun runsStepsAndReportsProgress() {
        val latents = tensor(longArrayOf(1, 1, 1, 2), floatArrayOf(0.1f, -0.2f))
        val cond = tensor(longArrayOf(1, 2, 1), floatArrayOf(1f, 2f))
        val progress = mutableListOf<GenerationProgress>()
        val result = DenoisingLoop(DdimScheduler(trainTimesteps = 20)).run(
            latents, cond, cond, steps = 4, guidanceScale = 7f,
            predictNoise = { sample, _, _ ->
                assertEquals(1L, sample.shape[0])
                tensor(sample.shape, FloatArray(sample.values.size) { 0.25f })
            }, onProgress = { progress.add(it) }
        )
        assertArrayEquals(latents.shape, result.shape)
        assertEquals(4, progress.size)
        assertEquals(1f, progress.last().fraction, 0.0001f)
    }

    @Test fun classifierFreeGuidanceUsesUnconditionalAndConditionalBatches() {
        val latents = tensor(longArrayOf(1, 1, 1, 1), floatArrayOf(0f))
        val cond = tensor(longArrayOf(1, 1, 1), floatArrayOf(1f))
        val captured = mutableListOf<FloatTensorData>()
        DenoisingLoop(DdimScheduler(trainTimesteps = 10)).run(
            latents, cond, cond, steps = 1, guidanceScale = 2f,
            predictNoise = { _, _, conditioning ->
                captured.add(conditioning)
                tensor(longArrayOf(1, 1, 1, 1), floatArrayOf(if (captured.size == 1) 1f else 3f))
            }
        )
        assertEquals(2L, captured!!.shape[0])
        assertArrayEquals(floatArrayOf(1f, 1f), captured!!.values, 0f)
    }

    @Test fun cancellationIsCheckedBeforeEachStep() {
        val latents = tensor(longArrayOf(1, 1, 1, 1), floatArrayOf(0f))
        val cond = tensor(longArrayOf(1, 1, 1), floatArrayOf(1f))
        try {
            DenoisingLoop().run(latents, cond, cond, 3, 1f,
                predictNoise = { sample, _, _ -> tensor(sample.shape, FloatArray(sample.values.size)) },
                isCancelled = { true })
            fail("Expected cancellation")
        } catch (_: java.util.concurrent.CancellationException) { }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMismatchedConditioning() {
        DenoisingLoop().run(
            tensor(longArrayOf(1,1,1,1), floatArrayOf(0f)),
            tensor(longArrayOf(1,1,1), floatArrayOf(1f)),
            tensor(longArrayOf(1,1,2), floatArrayOf(1f,2f)),
            2, 1f, predictNoise = { s, _, _ -> tensor(s.shape, FloatArray(s.values.size)) }
        )
    }
}
