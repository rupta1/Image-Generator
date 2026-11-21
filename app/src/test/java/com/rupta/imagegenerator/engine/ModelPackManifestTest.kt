package com.rupta.imagegenerator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelPackManifestTest {
    @Test
    fun parsesValidManifest() {
        val parsed = ModelPackManifest.parse(
            """{"formatVersion":1,"displayName":"Test Pack","graphs":{"unet":"unet.onnx","vae":"vae.onnx"}}"""
        )
        assertEquals(1, parsed.formatVersion)
        assertEquals("Test Pack", parsed.displayName)
        assertEquals("unet.onnx", parsed.graphs["unet"])
        assertEquals("vae.onnx", parsed.graphs["vae"])
    }

    @Test
    fun rejectsUnsupportedVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            ModelPackManifest.parse("""{"formatVersion":2,"displayName":"Test","graphs":{"unet":"unet.onnx"}}""")
        }
    }

    @Test
    fun rejectsPathTraversal() {
        assertThrows(IllegalArgumentException::class.java) {
            ModelPackManifest.parse("""{"formatVersion":1,"displayName":"Test","graphs":{"unet":"../unet.onnx"}}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ModelPackManifest.parse("""{"formatVersion":1,"displayName":"Test","graphs":{"unet":"..\\unet.onnx"}}""")
        }
    }

    @Test
    fun rejectsEmptyGraphs() {
        assertThrows(IllegalArgumentException::class.java) {
            ModelPackManifest.parse("""{"formatVersion":1,"displayName":"Test","graphs":{}}""")
        }
    }

    @Test
    fun rejectsMissingName() {
        assertThrows(IllegalArgumentException::class.java) {
            ModelPackManifest.parse("""{"formatVersion":1,"graphs":{"unet":"unet.onnx"}}""")
        }
    }
}
