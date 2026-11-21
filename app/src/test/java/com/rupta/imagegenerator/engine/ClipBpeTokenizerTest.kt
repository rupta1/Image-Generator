package com.rupta.imagegenerator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ClipBpeTokenizerTest {
    @Test fun encodesAndPadsToConfiguredLength() {
        val dir = createTempDir(prefix = "tokenizer-test")
        try {
            val vocab = File(dir, "vocab.json")
            vocab.writeText("""{"<|startoftext|>":0,"<|endoftext|>":1,"h":2,"e":3,"l":4,"o":5," ":6,"w":7,"r":8,"d":9,"!":10}""")
            val merges = File(dir, "merges.txt")
            merges.writeText("#version: 0.2\nh e\ne l\nl l\n")
            val tokenizer = ClipBpeTokenizer.fromFiles(vocab, merges, 8)
            val result = tokenizer.encode("hello!")
            assertEquals(8, result.size)
            assertEquals(0L, result.first())
            assertEquals(1L, result.last())
            assertTrue(result.contains(10L))
        } finally { dir.deleteRecursively() }
    }

    @Test fun truncatesLongInputAndKeepsEos() {
        val dir = createTempDir(prefix = "tokenizer-test")
        try {
            File(dir, "vocab.json").writeText("""{"<|startoftext|>":0,"<|endoftext|>":1,"a":2}""")
            File(dir, "merges.txt").writeText("#version: 0.2\n")
            val tokenizer = ClipBpeTokenizer.fromFiles(File(dir, "vocab.json"), File(dir, "merges.txt"), 5)
            val ids = tokenizer.encode("aaaaaa")
            assertEquals(5, ids.size)
            assertEquals(1L, ids[4])
        } finally { dir.deleteRecursively() }
    }
}
