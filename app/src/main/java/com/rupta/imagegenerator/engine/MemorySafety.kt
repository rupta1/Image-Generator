package com.rupta.imagegenerator.engine

import android.app.ActivityManager
import android.content.Context

/** Conservative RAM gate to avoid attempting a multi-gigabyte ONNX load during system memory pressure. */
object MemorySafety {
    data class Snapshot(val totalBytes: Long, val availableBytes: Long, val lowMemory: Boolean)

    fun snapshot(context: Context): Snapshot {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return Snapshot(info.totalMem, info.availMem, info.lowMemory)
    }

    fun requireSafeToLoad(context: Context, minimumAvailableBytes: Long = 5L * 1024 * 1024 * 1024) {
        val s = snapshot(context)
        require(!s.lowMemory && s.availableBytes >= minimumAvailableBytes) {
            "Not enough free RAM to safely load Stable Diffusion. Android currently reports ${s.availableBytes / (1024 * 1024)} MB available. Close other apps and retry only when at least 5 GB RAM is available. This model may still exceed device memory."
        }
    }
}
