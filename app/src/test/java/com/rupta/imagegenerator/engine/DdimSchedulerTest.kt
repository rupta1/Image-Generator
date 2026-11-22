package com.rupta.imagegenerator.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import kotlin.math.sqrt

class DdimSchedulerTest {
    @Test fun producesDescendingTimesteps() {
        val scheduler = DdimScheduler()
        val steps = scheduler.timesteps(20)
        assertEquals(20, steps.size)
        assertTrue(steps.first() > steps.last())
        for (i in 1 until steps.size) {
            assertTrue("Timesteps must descend at index $i", steps[i - 1] > steps[i])
        }
    }

    @Test fun oneStepUsesHighestTrainingTimestep() {
        assertArrayEquals(intArrayOf(999), DdimScheduler().timesteps(1))
    }

    @Test fun rejectsInvalidInferenceCount() {
        assertThrows(IllegalArgumentException::class.java) { DdimScheduler().timesteps(0) }
    }

    @Test fun deterministicStepMaintainsShapeAndFiniteValues() {
        val scheduler = DdimScheduler(trainTimesteps = 10, betaStart = 0.001f, betaEnd = 0.01f)
        val sample = FloatTensorData(longArrayOf(1, 1, 2), floatArrayOf(0.5f, -0.5f))
        val noise = FloatTensorData(longArrayOf(1, 1, 2), floatArrayOf(0.1f, 0.2f))
        val result = scheduler.step(sample, noise, timestep = 9, previousTimestep = 4)
        assertArrayEquals(sample.shape, result.shape)
        assertTrue(result.values.all { it.isFinite() })
    }

    @Test fun finalStepRecoversCleanSampleWhenNoisePredictionIsZero() {
        val trainTimesteps = 10
        val betaStart = 0.001f
        val betaEnd = 0.01f
        var alphaAtT = 1.0
        for (i in 0 until trainTimesteps) {
            val beta = betaStart.toDouble() +
                (betaEnd - betaStart).toDouble() * i / (trainTimesteps - 1)
            alphaAtT *= 1.0 - beta
        }
        // For x0=1 and epsilon=0, the noisy sample at t=9 is sqrt(alphaT).
        val sampleAtT = sqrt(alphaAtT).toFloat()
        val scheduler = DdimScheduler(trainTimesteps, betaStart, betaEnd)
        val sample = FloatTensorData(longArrayOf(1), floatArrayOf(sampleAtT))
        val noise = FloatTensorData(longArrayOf(1), floatArrayOf(0f))
        val result = scheduler.step(sample, noise, timestep = 9, previousTimestep = -1)
        assertEquals(1f, result.values[0], 1e-5f)
    }

    @Test fun rejectsShapeMismatch() {
        val scheduler = DdimScheduler(trainTimesteps = 10, betaStart = 0.001f, betaEnd = 0.01f)
        val sample = FloatTensorData(longArrayOf(1), floatArrayOf(1f))
        val noise = FloatTensorData(longArrayOf(2), floatArrayOf(0f, 0f))
        assertThrows(IllegalArgumentException::class.java) {
            scheduler.step(sample, noise, timestep = 9, previousTimestep = 4)
        }
    }
    @Test fun loadsLinearSchedulerConfiguration() {
        val file = File.createTempFile("scheduler", ".json")
        try {
            file.writeText("""{"_class_name":"DDIMScheduler","num_train_timesteps":50,"beta_start":0.001,"beta_end":0.02,"beta_schedule":"linear"}""")
            assertEquals(50, DdimScheduler.fromConfig(file).timesteps(50).size)
        } finally { file.delete() }
    }

    @Test fun rejectsUnsupportedSchedulerConfiguration() {
        val file = File.createTempFile("scheduler", ".json")
        try {
            file.writeText("""{"_class_name":"EulerScheduler","beta_schedule":"linear"}""")
            assertThrows(IllegalArgumentException::class.java) { DdimScheduler.fromConfig(file) }
        } finally { file.delete() }
    }

    @Test fun rejectsUnsupportedBetaSchedule() {
        val file = File.createTempFile("scheduler", ".json")
        try {
            file.writeText("""{"_class_name":"DDIMScheduler","beta_schedule":"scaled_linear"}""")
            assertThrows(IllegalArgumentException::class.java) { DdimScheduler.fromConfig(file) }
        } finally { file.delete() }
    }

}
