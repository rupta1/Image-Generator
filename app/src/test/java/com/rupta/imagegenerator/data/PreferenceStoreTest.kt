package com.rupta.imagegenerator.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenceStoreTest {
    @Test fun aggregatesPositiveAndNegativeSignals() {
        val profile = PreferenceStore.aggregate(listOf(
            InteractionSignal("cinematic portrait warm light", SignalKind.LIKE, "Cinematic"),
            InteractionSignal("cinematic portrait", SignalKind.SAVE, "Cinematic"),
            InteractionSignal("flat illustration", SignalKind.DISLIKE)
        ))
        assertEquals(2, profile.preferredStyles["Cinematic"])
        assertEquals(1, profile.negativeSignals["illustration"])
        assertEquals(2, profile.promptKeywords["portrait"])
    }
}
