package com.rupta.imagegenerator.engine

import android.app.ActivityManager
import android.content.Context

/**
 * Lightweight launch gate: removes the old 5 GB requirement, but avoids starting
 * a very large graph load when Android is already under severe memory pressure.
 * This is a preflight estimate, not a hard native-memory cap.
 */
object MemorySafety {
    data class Snapshot(val totalBytes: Long, val availableBytes: Long, val lowMemory: Boolean)

    private const val MB = 1024L * 1024L
    private const val MIN_AVAILABLE_MB = 1800L

    fun snapshot(context: Context): Snapshot {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return Snapshot(info.totalMem, info.availMem, info.lowMemory)
    }

    fun requireSafeToLoad(context: Context, minimumAvailableBytes: Long = MIN_AVAILABLE_MB * MB) {
        val s = snapshot(context)
        val availableMb = s.availableBytes / MB
        require(!s.lowMemory && s.availableBytes >= minimumAvailableBytes) {
            "Generation paused to protect Android from a memory crash. Available RAM: ${availableMb} MB. " +
                "The old 5 GB gate is removed; this pack still needs a reasonable free-memory reserve. " +
                "Close background apps and retry when at least ${minimumAvailableBytes / MB} MB is available. " +
                "A reserve check cannot guarantee the model's peak RAM usage."
        }
    }

    /** Use from UI diagnostics only; never force-kill apps or clear unrelated user data. */
    fun availableMb(context: Context): Long = snapshot(context).availableBytes / MB
}
