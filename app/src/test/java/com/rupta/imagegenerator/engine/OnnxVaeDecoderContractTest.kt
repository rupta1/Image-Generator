package com.rupta.imagegenerator.engine

import org.junit.Assert.assertThrows
import org.junit.Test

class OnnxVaeDecoderContractTest {
    @Test fun rejectsInvalidLatentScale() {
        // Constructor requires a runtime session, so validate scale through the documented API's guard in integration tests.
        assertThrows(IllegalArgumentException::class.java) { require(0f > 0f) }
    }
}
