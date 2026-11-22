package com.rupta.imagegenerator.engine

import kotlin.math.sqrt
import org.json.JSONObject
import java.io.File

/**
 * Minimal deterministic eta=0 DDIM scheduler for epsilon-prediction models.
 * Supports linear and Diffusers scaled_linear beta schedules.
 */
class DdimScheduler(
    trainTimesteps: Int = 1000,
    betaStart: Float = 0.00085f,
    betaEnd: Float = 0.012f,
    betaSchedule: String = "linear"
) {
    init {
        require(trainTimesteps >= 2) { "Training timesteps must be at least two." }
        require(betaStart > 0f && betaEnd > betaStart && betaEnd < 1f) { "Invalid beta range." }
        require(betaSchedule in setOf("linear", "scaled_linear")) { "Unsupported beta schedule: $betaSchedule" }
    }

    private val alphasCumprod: DoubleArray = run {
        val betas = DoubleArray(trainTimesteps) { i ->
            val fraction = i.toDouble() / (trainTimesteps - 1)
            when (betaSchedule) {
                "scaled_linear" -> {
                    val start = sqrt(betaStart.toDouble())
                    val end = sqrt(betaEnd.toDouble())
                    val interpolated = start + (end - start) * fraction
                    interpolated * interpolated
                }
                else -> betaStart.toDouble() + (betaEnd - betaStart).toDouble() * fraction
            }
        }
        val cumulative = DoubleArray(trainTimesteps)
        var product = 1.0
        for (i in betas.indices) {
            product *= (1.0 - betas[i])
            cumulative[i] = product
        }
        cumulative
    }

    companion object {
        /** Reads DDIM configuration; PNDM configs must be converted explicitly to DDIM configs. */
        fun fromConfig(file: File): DdimScheduler {
            require(file.isFile) { "Scheduler configuration file is missing." }
            val json = JSONObject(file.readText())
            val type = json.optString("scheduler_type", json.optString("_class_name", "DDIMScheduler"))
            require(type.equals("DDIMScheduler", ignoreCase = true) || type.equals("ddim", ignoreCase = true)) {
                "Only DDIM scheduler configurations are supported; convert the scheduler config explicitly."
            }
            val trainTimesteps = json.optInt("num_train_timesteps", 1000)
            val betaStart = json.optDouble("beta_start", 0.00085).toFloat()
            val betaEnd = json.optDouble("beta_end", 0.012).toFloat()
            val betaSchedule = json.optString("beta_schedule", "linear")
            require(betaSchedule in setOf("linear", "scaled_linear")) {
                "Only linear and scaled_linear beta schedules are supported."
            }
            return DdimScheduler(trainTimesteps, betaStart, betaEnd, betaSchedule)
        }
    }

    fun timesteps(inferenceSteps: Int): IntArray {
        require(inferenceSteps in 1..alphasCumprod.size) { "Invalid inference step count." }
        if (inferenceSteps == 1) return intArrayOf(alphasCumprod.lastIndex)
        val last = alphasCumprod.lastIndex
        return IntArray(inferenceSteps) { i ->
            (last - (last.toDouble() * i / (inferenceSteps - 1)).roundToInt()).coerceAtLeast(0)
        }.also { steps ->
            for (i in 1 until steps.size) require(steps[i - 1] > steps[i]) { "Timestep sequence must descend." }
        }
    }

    /** Deterministic eta=0 DDIM step for epsilon prediction. */
    fun step(sample: FloatTensorData, noisePrediction: FloatTensorData, timestep: Int, previousTimestep: Int): FloatTensorData {
        require(sample.shape.contentEquals(noisePrediction.shape)) { "Sample and noise shapes must match." }
        require(timestep in alphasCumprod.indices) { "Timestep is out of range." }
        require(previousTimestep in -1 until timestep) { "Previous timestep must be lower, or -1 for final." }
        val alphaT = alphasCumprod[timestep]
        val alphaPrevious = if (previousTimestep < 0) 1.0 else alphasCumprod[previousTimestep]
        val sqrtAlphaT = sqrt(alphaT)
        val sqrtOneMinusAlphaT = sqrt(1.0 - alphaT)
        val sqrtAlphaPrevious = sqrt(alphaPrevious)
        val sqrtOneMinusAlphaPrevious = sqrt(1.0 - alphaPrevious)
        val values = FloatArray(sample.values.size) { i ->
            val predictedClean = (sample.values[i] - sqrtOneMinusAlphaT * noisePrediction.values[i]) / sqrtAlphaT
            (sqrtAlphaPrevious * predictedClean + sqrtOneMinusAlphaPrevious * noisePrediction.values[i]).toFloat()
        }
        return FloatTensorData(sample.shape.copyOf(), values)
    }

    private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
}
