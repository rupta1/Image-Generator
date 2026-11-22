package com.rupta.imagegenerator.engine

import org.junit.Assert.*
import org.junit.Test

class GraphCompatibilityValidatorTest {
    private val contract = GraphInterfaceContract(
        inputs = listOf(TensorSpec("input", listOf(1, 3, -1, -1), "float32")),
        outputs = listOf(TensorSpec("output", listOf(1, 3, -1, -1), "float32"))
    )

    @Test fun acceptsMatchingGraphMetadata() {
        val metadata = GraphCompatibilityValidator.GraphMetadata(
            inputs = mapOf("input" to GraphCompatibilityValidator.TensorMetadata(longArrayOf(1,3,64,64), "FLOAT")),
            outputs = mapOf("output" to GraphCompatibilityValidator.TensorMetadata(longArrayOf(1,3,64,64), "FLOAT"))
        )
        GraphCompatibilityValidator.validate("test", contract, metadata)
    }

    @Test fun rejectsShapeMismatch() {
        val metadata = GraphCompatibilityValidator.GraphMetadata(
            inputs = mapOf("input" to GraphCompatibilityValidator.TensorMetadata(longArrayOf(1,4,64,64), "FLOAT")),
            outputs = mapOf("output" to GraphCompatibilityValidator.TensorMetadata(longArrayOf(1,3,64,64), "FLOAT"))
        )
        assertThrows(IllegalArgumentException::class.java) { GraphCompatibilityValidator.validate("test", contract, metadata) }
    }

    @Test fun rejectsTypeMismatch() {
        val metadata = GraphCompatibilityValidator.GraphMetadata(
            inputs = mapOf("input" to GraphCompatibilityValidator.TensorMetadata(longArrayOf(1,3,64,64), "INT64")),
            outputs = mapOf("output" to GraphCompatibilityValidator.TensorMetadata(longArrayOf(1,3,64,64), "FLOAT"))
        )
        assertThrows(IllegalArgumentException::class.java) { GraphCompatibilityValidator.validate("test", contract, metadata) }
    }

    @Test fun rejectsUndeclaredGraphInputs() {
        val metadata = GraphCompatibilityValidator.GraphMetadata(
            inputs = mapOf("input" to GraphCompatibilityValidator.TensorMetadata(longArrayOf(1,3,64,64), "FLOAT"), "extra" to GraphCompatibilityValidator.TensorMetadata(longArrayOf(1), "FLOAT")),
            outputs = mapOf("output" to GraphCompatibilityValidator.TensorMetadata(longArrayOf(1,3,64,64), "FLOAT"))
        )
        assertThrows(IllegalArgumentException::class.java) { GraphCompatibilityValidator.validate("test", contract, metadata) }
    }
}
