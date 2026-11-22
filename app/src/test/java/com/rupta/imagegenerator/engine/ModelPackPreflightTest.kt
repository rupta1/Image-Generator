package com.rupta.imagegenerator.engine

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ModelPackPreflightTest {
    @Test fun rejectsMissingPackFolder() {
        assertThrows(IllegalArgumentException::class.java) { ModelPackPreflight.inspect(File("not-a-pack")) }
    }

    @Test fun rejectsMissingManifest() {
        val dir = Files.createTempDirectory("pack-preflight").toFile()
        try { assertThrows(IllegalArgumentException::class.java) { ModelPackPreflight.inspect(dir) } }
        finally { dir.deleteRecursively() }
    }
}
