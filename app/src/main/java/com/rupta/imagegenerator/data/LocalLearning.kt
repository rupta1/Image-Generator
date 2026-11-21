package com.rupta.imagegenerator.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Lightweight local storage for studio settings and preference signals. */
class LocalLearning(context: Context) {
    private val prefs = context.getSharedPreferences("local_ai_studio", Context.MODE_PRIVATE)
    fun loadPrompt() = prefs.getString("prompt", "") ?: ""
    fun savePrompt(v:String) = prefs.edit().putString("prompt",v).apply()
    fun loadNegative() = prefs.getString("negative", "") ?: ""
    fun saveNegative(v:String) = prefs.edit().putString("negative",v).apply()
    fun loadSteps() = prefs.getFloat("steps",20f)
    fun saveSteps(v:Float) = prefs.edit().putFloat("steps",v).apply()
    fun loadSize() = prefs.getString("size","512 × 512") ?: "512 × 512"
    fun saveSize(v:String) = prefs.edit().putString("size",v).apply()
    fun loadSeed() = prefs.getString("seed","") ?: ""
    fun saveSeed(v:String) = prefs.edit().putString("seed",v).apply()
    fun learningEnabled() = prefs.getBoolean("learning_enabled",true)
    fun setLearning(v:Boolean) = prefs.edit().putBoolean("learning_enabled",v).apply()
    fun signals():List<InteractionSignal> {
        val arr=JSONArray(prefs.getString("signals","[]"))
        return (0 until arr.length()).map { i ->
            val o=arr.getJSONObject(i)
            InteractionSignal(o.optString("prompt"), SignalKind.valueOf(o.optString("kind")),o.optString("style").ifBlank { null },o.optLong("time"))
        }
    }
    fun addSignal(prompt:String,kind:SignalKind,style:String?=null) {
        if (!learningEnabled()) return
        val arr=JSONArray(prefs.getString("signals","[]"))
        arr.put(JSONObject().put("prompt",prompt).put("kind",kind.name).put("style",style ?: "").put("time",System.currentTimeMillis()))
        prefs.edit().putString("signals",arr.toString()).apply()
    }
    fun clearLearning() = prefs.edit().remove("signals").apply()
    fun profile():UserPreferenceProfile = PreferenceStore.aggregate(signals())
}
