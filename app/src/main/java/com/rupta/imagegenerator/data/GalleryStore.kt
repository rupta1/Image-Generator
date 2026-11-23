package com.rupta.imagegenerator.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import java.io.File
import java.io.FileOutputStream

data class SavedCreation(val file: File, val createdAt: Long)

object GalleryStore {
    private fun directory(context: Context): File = File(context.filesDir, "gallery").apply { mkdirs() }

    fun save(context: Context, bitmap: Bitmap): SavedCreation {
        val dir = directory(context)
        val file = File(dir, "creation_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return SavedCreation(file, file.lastModified())
    }

    fun list(context: Context): List<SavedCreation> = directory(context).listFiles()
        ?.filter { it.isFile && it.extension.equals("png", true) }
        ?.sortedByDescending { it.lastModified() }
        ?.map { SavedCreation(it, it.lastModified()) } ?: emptyList()

    fun load(file: File): Bitmap? = BitmapFactory.decodeFile(file.absolutePath)

    fun delete(file: File): Boolean = file.delete()
}
