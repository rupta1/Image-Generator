package com.rupta.imagegenerator.engine

import android.graphics.Bitmap
import android.graphics.Color
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

/** Executes a single-input VAE decoder and converts normalized float output to an RGB bitmap. */
class OnnxVaeDecoder(
    private val environment: OrtEnvironment,
    private val session: OrtSession,
    private val contract: GraphInterfaceContract
) {
    fun decode(latents: FloatTensorData, latentScale: Float = 0.18215f): Bitmap {
        require(latentScale.isFinite() && latentScale > 0f)
        val input = contract.inputs.singleOrNull() ?: error("VAE decoder must declare exactly one input.")
        val outputSpec = contract.outputs.firstOrNull() ?: error("VAE decoder has no output.")
        require(input.dataType == "float32" && outputSpec.dataType == "float32") { "VAE decoder supports float32 tensors only." }
        require(input.validateShape(latents.shape) && input.name in session.inputNames) { "Latent input violates VAE contract." }
        require(outputSpec.name in session.outputNames) { "VAE output missing from graph." }
        val scaled = FloatTensorData(latents.shape.copyOf(), latents.values.map { it / latentScale }.toFloatArray())
        OnnxTensor.createTensor(environment, FloatBuffer.wrap(scaled.values), scaled.shape).use { tensor ->
            session.run(mapOf(input.name to tensor), setOf(outputSpec.name)).use { result ->
                val out = result.get(outputSpec.name).orElseThrow { IllegalStateException("VAE output missing.") }
                require(out is OnnxTensor) { "VAE output is not a tensor." }
                val shape = out.info.shape
                require(outputSpec.validateShape(shape) && shape.size == 4 && shape[0] == 1L) { "VAE output shape is unsupported." }
                val buffer = out.floatBuffer
                val values = FloatArray(buffer.remaining()); buffer.get(values)
                require(values.all { it.isFinite() }) { "VAE output contains non-finite values." }
                val channelsFirst = shape[1] == 3L
                val width = if (channelsFirst) shape[3].toInt() else shape[2].toInt()
                val height = if (channelsFirst) shape[2].toInt() else shape[1].toInt()
                val channelCount = if (channelsFirst) shape[1].toInt() else shape[3].toInt()
                require(width > 0 && height > 0 && channelCount >= 3) { "VAE output must contain RGB channels." }
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                for (y in 0 until height) for (x in 0 until width) {
                    fun channel(c: Int): Int {
                        val index = if (channelsFirst) c * width * height + y * width + x else (y * width + x) * channelCount + c
                        return (((values[index] + 1f) * 127.5f).roundToInt().coerceIn(0,255))
                    }
                    bitmap.setPixel(x, y, Color.rgb(channel(0), channel(1), channel(2)))
                }
                return bitmap
            }
        }
    }
    private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
}
