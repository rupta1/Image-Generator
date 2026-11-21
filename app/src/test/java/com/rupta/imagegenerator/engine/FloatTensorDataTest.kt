package com.rupta.imagegenerator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FloatTensorDataTest {
    @Test fun acceptsMatchingShapeAndPayload() {
        val tensor = FloatTensorData(longArrayOf(1, 2, 2), floatArrayOf(1f, 2f, 3f, 4f))
        assertEquals(4, tensor.values.size)
    }

    @Test fun rejectsMismatchedPayloadLength() {
        assertThrows(IllegalArgumentException::class.java) {
            FloatTensorData(longArrayOf(1, 2, 2), floatArrayOf(1f, 2f))
        }
    }

    @Test fun rejectsNonPositiveDimensions() {
        assertThrows(IllegalArgumentException::class.java) {
            FloatTensorData(longArrayOf(1, 0), floatArrayOf())
        }
    }

    @Test fun rejectsNonFiniteValues() {
        assertThrows(IllegalArgumentException::class.java) {
            FloatTensorData(longArrayOf(1), floatArrayOf(Float.NaN))
        }
    }
}
