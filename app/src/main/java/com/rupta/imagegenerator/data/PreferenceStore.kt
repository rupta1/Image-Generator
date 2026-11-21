package com.rupta.imagegenerator.data

/** Local preference-learning record foundation. Persistent learning storage will be wired in a later milestone. */
data class InteractionSignal(
    val prompt: String,
    val kind: SignalKind,
    val style: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)
enum class SignalKind { LIKE, DISLIKE, SAVE, EDIT, REGENERATE, RATING }

data class UserPreferenceProfile(
    val preferredStyles: Map<String, Int> = emptyMap(),
    val preferredAspectRatios: Map<String, Int> = emptyMap(),
    val promptKeywords: Map<String, Int> = emptyMap(),
    val negativeSignals: Map<String, Int> = emptyMap()
)

/** Pure deterministic profile aggregation. Signals are not sent off-device. */
object PreferenceStore {
    fun aggregate(signals: List<InteractionSignal>): UserPreferenceProfile {
        val positive = signals.filter { it.kind in setOf(SignalKind.LIKE, SignalKind.SAVE, SignalKind.RATING) }
        val negative = signals.filter { it.kind == SignalKind.DISLIKE }
        return UserPreferenceProfile(
            preferredStyles = positive.mapNotNull { it.style }.groupingBy { it }.eachCount(),
            promptKeywords = positive.flatMap { tokenize(it.prompt) }.groupingBy { it }.eachCount(),
            negativeSignals = negative.flatMap { tokenize(it.prompt) }.groupingBy { it }.eachCount()
        )
    }
    private fun tokenize(text: String) = text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 2 }.distinct()
}
