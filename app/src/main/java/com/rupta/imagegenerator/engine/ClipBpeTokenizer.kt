package com.rupta.imagegenerator.engine

import org.json.JSONObject
import java.io.File

/**
 * Offline CLIP-style byte-level BPE tokenizer.
 * Reads a token-to-ID vocab JSON and merge rules text file. Pack assets must be
 * provided by the user; tokenizer does not fetch assets.
 */
class ClipBpeTokenizer private constructor(
    private val vocab: Map<String, Int>,
    private val mergeRanks: Map<Pair<String, String>, Int>,
    private val bosId: Int,
    private val eosId: Int,
    private val padId: Int,
    private val unknownId: Int,
    val maxTokenLength: Int
) {
    fun encode(text: String): LongArray {
        val ids = mutableListOf<Long>()
        ids.add(bosId.toLong())
        tokenize(text).forEach { token ->
            bpe(byteEncode(token)).forEach { piece ->
                ids.add((vocab[piece] ?: unknownId).toLong())
            }
        }
        ids.add(eosId.toLong())
        if (ids.size > maxTokenLength) {
            ids.subList(maxTokenLength, ids.size).clear()
            ids[maxTokenLength - 1] = eosId.toLong()
        }
        while (ids.size < maxTokenLength) ids.add(padId.toLong())
        return ids.toLongArray()
    }

    private fun tokenize(text: String): List<String> {
        val pattern = Regex("""(?i)'s|'t|'re|'ve|'m|'ll|'d|[a-z]+|[0-9]+|[^\s\p{L}\p{N}]+|\s+""")
        return pattern.findAll(text).map { it.value }.toList()
    }

    private fun bpe(encoded: String): List<String> {
        if (encoded.isEmpty()) return emptyList()
        var pieces = encoded.map { it.toString() }
        while (pieces.size > 1) {
            var bestRank = Int.MAX_VALUE
            var bestIndex = -1
            for (i in 0 until pieces.size - 1) {
                val rank = mergeRanks[pieces[i] to pieces[i + 1]] ?: Int.MAX_VALUE
                if (rank < bestRank) { bestRank = rank; bestIndex = i }
            }
            if (bestIndex < 0) break
            pieces = pieces.toMutableList().apply {
                this[bestIndex] = this[bestIndex] + this[bestIndex + 1]
                removeAt(bestIndex + 1)
            }
        }
        return pieces
    }

    private fun byteEncode(token: String): String {
        val bytes = token.toByteArray(Charsets.UTF_8)
        return bytes.map { byteToUnicode[it.toInt() and 0xff] }.joinToString("")
    }

    companion object {
        private val byteToUnicode: Map<Int, Char> = buildMap {
            val visible = mutableListOf<Int>()
            visible.addAll(33..126); visible.addAll(161..172); visible.addAll(174..255)
            val visibleSet = visible.toSet()
            var extra = 0
            for (b in 0..255) {
                val code = if (b in visibleSet) b else 256 + extra++
                put(b, code.toChar())
            }
        }

        fun fromFiles(vocabFile: File, mergesFile: File, maxTokenLength: Int,
                      bosToken: String = "<|startoftext|>", eosToken: String = "<|endoftext|>",
                      padToken: String = "<|endoftext|>", unknownToken: String = "<|endoftext|>"): ClipBpeTokenizer {
            require(vocabFile.isFile && mergesFile.isFile) { "Tokenizer files missing." }
            require(maxTokenLength in 2..256)
            val json = JSONObject(vocabFile.readText())
            val vocab = mutableMapOf<String, Int>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val id = json.optInt(key, -1)
                require(id >= 0) { "Invalid token ID." }
                vocab[key] = id
            }
            val merges = mergesFile.readLines().filter { it.isNotBlank() && !it.startsWith("#") }
                .drop(1).map { line -> line.trim().split(Regex("\\s+")) }
                .filter { it.size == 2 }
            val ranks = merges.mapIndexed { index, pair -> (pair[0] to pair[1]) to index }.toMap()
            fun requiredId(token: String) = vocab[token] ?: error("Required special token missing: $token")
            return ClipBpeTokenizer(vocab, ranks, requiredId(bosToken), requiredId(eosToken),
                requiredId(padToken), requiredId(unknownToken), maxTokenLength)
        }
    }
}
