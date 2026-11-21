package com.rupta.imagegenerator.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class DdimSchedulerTest {
    @Test fun producesDescendingTimesteps() {
        val scheduler = DdimScheduler()
        val steps = scheduler.timesteps(20)
        assertEquals(20, steps.size)
        assertTrue(steps.first() > steps.last())
        assertTrue(steps.zipWithNext().all { (a, b) -> a > b })
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

    @Test fun finalStepTargetsCleanSampleAlpha() {
        val scheduler = DdimScheduler(trainTimesteps = 10, betaStart = 0.001f, betaEnd = 0.01f)
        val sample = FloatTensorData(longArrayOf(1), floatArrayOf(1f))
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
}
