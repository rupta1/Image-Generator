package com.rupta.imagegenerator.engine

import org.junit.Assert.*
import org.junit.Test

class StableDiffusionUnetAdapterTest {
    @Test fun mapsInputsByCommonNames() {
        val contract = GraphInterfaceContract(
            inputs = listOf(
                TensorSpec("sample", listOf(2,4,8,8), "float32"),
                TensorSpec("timestep", listOf(1), "float32"),
                TensorSpec("encoder_hidden_states", listOf(2,77,768), "float32")
            ),
            outputs = listOf(TensorSpec("noise", listOf(2,4,8,8), "float32"))
        )
        val adapter = StableDiffusionUnetAdapter(null, contract)
        val sample = FloatTensorData(longArrayOf(2,4,8,8), FloatArray(512))
        val conditioning = FloatTensorData(longArrayOf(2,77,768), FloatArray(2*77*768))
        val inputs = adapter.mapInputs(sample, 12, conditioning)
        assertArrayEquals(sample.shape, inputs["sample"]!!.shape)
        assertEquals(12f, inputs["timestep"]!!.values[0], 0f)
        assertArrayEquals(conditioning.shape, inputs["encoder_hidden_states"]!!.shape)
    }

    @Test fun unsupportedInputNamesAreRejected() {
        val contract = GraphInterfaceContract(
            inputs = listOf(TensorSpec("mystery", listOf(1), "float32")),
            outputs = listOf(TensorSpec("output", listOf(1), "float32"))
        )
        val adapter = StableDiffusionUnetAdapter(null, contract)
        val sample = FloatTensorData(longArrayOf(1,4,2,2), FloatArray(16))
        val cond = FloatTensorData(longArrayOf(1,1,1), floatArrayOf(0f))
        try {
            adapter.mapInputs(sample, 1, cond)
            fail("Expected unsupported input rejection")
        } catch (_: IllegalStateException) { }
    }
}
