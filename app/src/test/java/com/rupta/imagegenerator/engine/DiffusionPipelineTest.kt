package com.rupta.imagegenerator.engine

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DiffusionPipelineTest {
    private fun tokenizer(): ClipBpeTokenizer {
        val dir = createTempDir(prefix = "pipeline-tokenizer")
        File(dir, "vocab.json").writeText("""{"<|startoftext|>":0,"<|endoftext|>":1,"a":2,"b":3}""")
        File(dir, "merges.txt").writeText("#version: 0.2\n")
        return ClipBpeTokenizer.fromFiles(File(dir, "vocab.json"), File(dir, "merges.txt"), 8)
    }

    @Test fun orchestratesPromptEncodingDenoisingAndDecoder() {
        var encodeCalls = 0
        var predictCalls = 0
        var decodeLatents: FloatTensorData? = null
        val pipeline = DiffusionPipeline(
            tokenizer = tokenizer(),
            encodeText = { ids ->
                encodeCalls++
                assertEquals(8, ids.size)
                FloatTensorData(longArrayOf(1, 2, 1), floatArrayOf(1f, 2f))
            },
            predictNoise = { sample, _, _ ->
                predictCalls++
                FloatTensorData(sample.shape, FloatArray(sample.values.size))
            },
            decodeLatents = { latents ->
                decodeLatents = latents
                throw IllegalStateException("decoder reached")
            },
            vaeScale = 8,
            scheduler = DdimScheduler(trainTimesteps = 20)
        )
        try {
            pipeline.generate(GenerationRequest("a", "b", 256, 256, steps = 2, seed = 42))
            fail("Expected decoder sentinel")
        } catch (e: IllegalStateException) {
            assertEquals("decoder reached", e.message)
        }
        assertEquals(2, encodeCalls)
        assertEquals(4, predictCalls)
        assertNotNull(decodeLatents)
        assertArrayEquals(longArrayOf(1, 4, 32, 32), decodeLatents!!.shape)
    }

    @Test(expected = java.util.concurrent.CancellationException::class)
    fun cancellationBeforeEncodingStopsPipeline() {
        val pipeline = DiffusionPipeline(
            tokenizer(), { FloatTensorData(longArrayOf(1, 1, 1), floatArrayOf(1f)) },
            { sample, _, _ -> sample },
            { throw IllegalStateException("should not decode") },
            scheduler = DdimScheduler(trainTimesteps = 10)
        )
        pipeline.generate(GenerationRequest("a", width = 256, height = 256, steps = 1), isCancelled = { true })
    }
}
