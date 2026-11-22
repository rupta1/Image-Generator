package com.rupta.imagegenerator.engine

import kotlin.math.sqrt
import org.json.JSONObject
import java.io.File

/**
 * Minimal DDIM scheduler for epsilon-prediction models with a linear beta schedule.
 * Config values are explicit so model-pack compatibility can be validated by callers.
 */
class DdimScheduler(
    trainTimesteps: Int = 1000,
    betaStart: Float = 0.00085f,
    betaEnd: Float = 0.012f
) {
    init {
        require(trainTimesteps >= 2) { "Training timesteps must be at least two." }
        require(betaStart > 0f && betaEnd > betaStart && betaEnd < 1f) { "Invalid beta range." }
    }

    private val alphasCumprod: DoubleArray = run {
        val betas = DoubleArray(trainTimesteps) { i ->
            betaStart.toDouble() + (betaEnd - betaStart).toDouble() * i / (trainTimesteps - 1)
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
        /** Reads common Diffusers scheduler JSON fields; unsupported schedule types are rejected. */
        fun fromConfig(file: File): DdimScheduler {
            require(file.isFile) { "Scheduler configuration file is missing." }
            val json = JSONObject(file.readText())
            val type = json.optString("scheduler_type", json.optString("_class_name", "DDIMScheduler"))
            require(type.equals("DDIMScheduler", ignoreCase = true) || type.equals("ddim", ignoreCase = true)) {
                "Only DDIM scheduler configurations are supported."
            }
            val trainTimesteps = json.optInt("num_train_timesteps", 1000)
            val betaStart = json.optDouble("beta_start", 0.00085).toFloat()
            val betaEnd = json.optDouble("beta_end", 0.012).toFloat()
            val betaSchedule = json.optString("beta_schedule", "linear")
            require(betaSchedule == "linear") { "Only linear beta schedules are currently supported." }
            return DdimScheduler(trainTimesteps, betaStart, betaEnd)
        }
    }

    /** Returns monotonically descending training timesteps for the requested inference count. */
    fun timesteps(inferenceSteps: Int): IntArray {
        require(inferenceSteps in 1..alphasCumprod.size) { "Invalid inference step count." }
        if (inferenceSteps == 1) return intArrayOf(alphasCumprod.lastIndex)
        val last = alphasCumprod.lastIndex
        return IntArray(inferenceSteps) { i ->
            (last - (last.toDouble() * i / (inferenceSteps - 1)).roundToInt()).coerceAtLeast(0)
        }.also { steps ->
            for (i in 1 until steps.size) {
                require(steps[i - 1] > steps[i]) { "Timestep sequence must descend." }
            }
        }
    }

    /**
     * Deterministic eta=0 DDIM step for epsilon prediction.
     * previousTimestep=-1 denotes the final transition to clean sample.
     */
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
