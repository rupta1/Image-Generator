package com.rupta.imagegenerator.engine

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer

/** Result of executing the text encoder: output tensor shape and flattened values. */
data class TextConditioning(val shape: LongArray, val values: FloatArray)

/** Executes a text encoder with a single int64 token input and float32 output. */
class OnnxTextEncoder(private val environment: OrtEnvironment, private val session: OrtSession, private val graphContract: GraphInterfaceContract) {
    fun encode(tokenIds: LongArray): TextConditioning {
        require(tokenIds.isNotEmpty()) { "Token IDs cannot be empty." }
        val inputSpec = graphContract.inputs.singleOrNull() ?: error("Text encoder contract must declare exactly one input.")
        require(inputSpec.dataType == "int64") { "Text encoder input must be int64 token IDs." }
        val outputSpec = graphContract.outputs.first()
        require(outputSpec.dataType == "float32") { "Text encoder output must be float32." }
        val shape = longArrayOf(1, tokenIds.size.toLong())
        require(inputSpec.validateShape(shape)) { "Token ID tensor shape violates contract." }
        require(inputSpec.name in session.inputNames) { "ONNX graph input is missing." }
        require(outputSpec.name in session.outputNames) { "ONNX graph output is missing." }
        OnnxTensor.createTensor(environment, LongBuffer.wrap(tokenIds), shape).use { inputTensor ->
            session.run(mapOf(inputSpec.name to inputTensor), setOf(outputSpec.name)).use { result ->
                val output = result.get(outputSpec.name).orElseThrow { IllegalStateException("Text encoder output missing.") }
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
