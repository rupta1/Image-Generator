package com.rupta.imagegenerator.engine

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.IntBuffer
import java.nio.LongBuffer

/** Result of executing the text encoder: output tensor shape and flattened values. */
data class TextConditioning(val shape: LongArray, val values: FloatArray)

/** Executes a CLIP text encoder using the token integer type declared by its ONNX graph. */
class OnnxTextEncoder(
    private val environment: OrtEnvironment,
    private val session: OrtSession,
    private val graphContract: GraphInterfaceContract
) {
    fun encode(tokenIds: LongArray): TextConditioning {
        require(tokenIds.isNotEmpty()) { "Token IDs cannot be empty." }
        val inputSpec = graphContract.inputs.singleOrNull()
            ?: error("Text encoder contract must declare exactly one input.")
        require(inputSpec.dataType in setOf("int64", "int32")) {
            "Text encoder input must be int32 or int64 token IDs (got ${inputSpec.dataType})."
        }
        val outputSpec = graphContract.outputs.firstOrNull()
            ?: error("Text encoder contract must declare an output.")
        require(outputSpec.dataType == "float32") { "Text encoder output must be float32." }
        val shape = longArrayOf(1, tokenIds.size.toLong())
        require(inputSpec.validateShape(shape)) { "Token ID tensor shape violates contract." }
        require(inputSpec.name in session.inputNames) { "ONNX graph input is missing." }
        require(outputSpec.name in session.outputNames) { "ONNX graph output is missing." }

        val tensor = when (inputSpec.dataType) {
            "int64" -> OnnxTensor.createTensor(environment, LongBuffer.wrap(tokenIds), shape)
            "int32" -> {
                require(tokenIds.all { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }) {
                    "Token ID cannot be represented as int32."
                }
                OnnxTensor.createTensor(environment, IntBuffer.wrap(tokenIds.map { it.toInt() }.toIntArray()), shape)
            }
            else -> error("Unsupported text encoder input type.")
        }
        tensor.use { inputTensor ->
            session.run(mapOf(inputSpec.name to inputTensor), setOf(outputSpec.name)).use { result ->
                val output = result.get(outputSpec.name)
                    .orElseThrow { IllegalStateException("Text encoder output missing.") }
                require(output is OnnxTensor) { "Text encoder output is not a tensor." }
                val outputShape = output.info.shape
                require(outputSpec.validateShape(outputShape)) { "Text encoder output shape violates contract." }
                val buffer = output.floatBuffer
                val values = FloatArray(buffer.remaining())
                buffer.get(values)
                require(values.all { it.isFinite() }) { "Text encoder output contains non-finite values." }
                return TextConditioning(outputShape, values)
            }
        }
    }
}
