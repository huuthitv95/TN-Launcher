package vn.tnlauncher.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class WallpaperImporter(context: Context) {
    private val context = context.applicationContext
    suspend fun importImage(uri: Uri): String {
        var created: File? = null
        try {
            return withContext(Dispatchers.IO) {
                val temporary = File.createTempFile("wallpaper-import-", ".tmp", context.cacheDir)
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        temporary.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var total = 0
                            while (true) {
                                coroutineContext.ensureActive()
                                val read = input.read(buffer); if (read < 0) break
                                total += read
                                require(total <= 32 * 1024 * 1024) { "Image exceeds 32 MB" }
                                output.write(buffer, 0, read)
                            }
                        }
                    } ?: error("Unreadable image")
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(temporary.path, bounds)
                    require(bounds.outWidth > 0 && bounds.outHeight > 0)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 1
                        while (bounds.outWidth / inSampleSize > 2048 || bounds.outHeight / inSampleSize > 2048) inSampleSize *= 2
                    }
                    val bitmap = BitmapFactory.decodeFile(temporary.path, options) ?: error("Invalid image")
                    val destination = File(context.filesDir, "wallpaper-${System.nanoTime()}.jpg")
                    created = destination
                    try { destination.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) } }
                    finally { bitmap.recycle() }
                    coroutineContext.ensureActive()
                    destination.name
                } finally { temporary.delete() }
            }
        } catch (error: Throwable) {
            // Also covers cancellation at the withContext return boundary.
            withContext(NonCancellable + Dispatchers.IO) { created?.delete() }
            throw error
        }
    }
    fun discard(name: String) {
        if (name.matches(Regex("wallpaper-[0-9]+\\.jpg"))) File(context.filesDir, name).delete()
    }
}
