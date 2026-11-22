package com.rupta.imagegenerator.engine

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

/** Explicit float tensor payload used to feed a UNet graph. */
data class FloatTensorData(val shape: LongArray, val values: FloatArray) {
    init {
        require(shape.isNotEmpty() && shape.all { it > 0 }) { "Tensor dimensions must be positive." }
        val expected = shape.fold(1L) { acc, dimension -> Math.multiplyExact(acc, dimension) }
        require(expected == values.size.toLong()) { "Tensor shape does not match payload length." }
        require(values.all { it.isFinite() }) { "Tensor payload contains non-finite values." }
    }
}

/** UNet prediction result, retaining shape and flattened float values. */
data class UnetPrediction(val shape: LongArray, val values: FloatArray)

/**
 * Runs a declared UNet ONNX interface using float32 inputs.
 * Callers supply every graph input by name (latent sample, timestep, conditioning, etc.).
 */
class OnnxUnetRunner(
    private val environment: OrtEnvironment,
    private val session: OrtSession,
    private val contract: GraphInterfaceContract
) {
    fun predict(inputs: Map<String, FloatTensorData>): UnetPrediction {
        require(inputs.keys == contract.inputs.map { it.name }.toSet()) {
            "Input payload names must exactly match the UNet contract."
        }
        contract.inputs.forEach { spec ->
            require(spec.dataType == "float32") { "UNet runner currently supports float32 inputs only." }
            val payload = inputs.getValue(spec.name)
            require(spec.validateShape(payload.shape)) { "Input shape violates contract: ${spec.name}" }
            require(spec.name in session.inputNames) { "ONNX graph input is missing: ${spec.name}" }
        }
        val outputSpec = contract.outputs.firstOrNull() ?: error("UNet contract has no outputs.")
        require(outputSpec.dataType == "float32") { "UNet runner currently supports float32 output only." }
        require(outputSpec.name in session.outputNames) { "ONNX graph output is missing: ${outputSpec.name}" }

        val tensors = mutableMapOf<String, OnnxTensor>()
        try {
            inputs.forEach { (name, payload) ->
                tensors[name] = OnnxTensor.createTensor(
                    environment, FloatBuffer.wrap(payload.values), payload.shape
                )
            }
            session.run(tensors, setOf(outputSpec.name)).use { result ->
                val output = result.get(outputSpec.name).orElseThrow {
                    IllegalStateException("UNet output missing.")
                }
                require(output is OnnxTensor) { "UNet output is not a tensor." }
                val shape = output.info.shape
                require(outputSpec.validateShape(shape)) { "UNet output shape violates contract." }
                val buffer = output.floatBuffer
                val values = FloatArray(buffer.remaining())
                buffer.get(values)
                require(values.all { it.isFinite() }) { "UNet output contains non-finite values." }
                return UnetPrediction(shape, values)
            }
        } finally {
            tensors.values.forEach { runCatching { it.close() } }
        }
    }
}


/** Helper that maps common Stable Diffusion UNet input names to tensors for the denoising loop. */
class StableDiffusionUnetAdapter(private val runner: OnnxUnetRunner, private val contract: GraphInterfaceContract) {
    fun predict(sample: FloatTensorData, timestep: Int, conditioning: FloatTensorData): FloatTensorData {
        val payloads = contract.inputs.associate { spec ->
            val lower = spec.name.lowercase()
            val payload = when {
                lower.contains("sample") || lower.contains("latent") -> sample
                lower.contains("timestep") || lower == "time" -> FloatTensorData(longArrayOf(1), floatArrayOf(timestep.toFloat()))
                lower.contains("encoder") || lower.contains("hidden") || lower.contains("condition") -> conditioning
                else -> error("Unsupported UNet input name: ${spec.name}")
            }
            require(spec.validateShape(payload.shape)) { "Mapped input shape does not match contract: ${spec.name}" }
            spec.name to payload
        }
        val result = runner.predict(payloads)
        return FloatTensorData(result.shape, result.values)
    }
}
