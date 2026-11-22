package com.rupta.imagegenerator.engine

/** Lightweight, device-local timing and memory diagnostics for inference benchmarking. */
data class InferenceMetrics(
    val promptEncodingMillis: Long,
    val denoisingMillis: Long,
    val decodingMillis: Long,
    val totalMillis: Long,
    val heapUsedBeforeBytes: Long,
    val heapUsedAfterBytes: Long,
    val heapPeakApproxBytes: Long,
    val seed: Long,
    val width: Int,
    val height: Int,
    val steps: Int
) {
    companion object {
        internal fun heapUsed(): Long {
            val runtime = Runtime.getRuntime()
            return runtime.totalMemory() - runtime.freeMemory()
        }
    }
}
