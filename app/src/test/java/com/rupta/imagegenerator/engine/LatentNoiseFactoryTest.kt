package com.rupta.imagegenerator.engine

import org.junit.Assert.*
import org.junit.Test

class LatentNoiseFactoryTest {
    @Test fun sameSeedProducesSameLatents() {
        val a = LatentNoiseFactory.create(height=8, width=8, seed=42)
        val b = LatentNoiseFactory.create(height=8, width=8, seed=42)
        assertArrayEquals(a.shape, b.shape)
        assertArrayEquals(a.values, b.values, 0f)
    }
    @Test fun differentSeedsProduceDifferentLatents() {
        val a = LatentNoiseFactory.create(height=8, width=8, seed=42)
        val b = LatentNoiseFactory.create(height=8, width=8, seed=43)
        assertFalse(a.values.contentEquals(b.values))
    }
    @Test fun dimensionsAreApplied() {
        assertArrayEquals(longArrayOf(1,4,16,24), LatentNoiseFactory.create(height=16,width=24,seed=1).shape)
    }
}
