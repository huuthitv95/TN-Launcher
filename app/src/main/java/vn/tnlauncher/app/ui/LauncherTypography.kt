package vn.tnlauncher.app.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import vn.tnlauncher.app.R

/** Locally bundled, user-authorized fonts; never depend on a Windows runtime path. */
class LauncherTypography(context: Context) {
    val body: Typeface = loadOrFallback(context, R.font.tn_sf_body)
    val heading: Typeface = loadOrFallback(context, R.font.tn_sf_heading)

    // This font lacks several combining marks. Restrict it to the fixed Latin Home brand.
    val brand: Typeface = loadOrFallback(context, R.font.tn_new_york).let { candidate ->
        val paint = Paint().apply { typeface = candidate }
        if ("TN Launcher".filterNot(Char::isWhitespace).all { paint.hasGlyph(it.toString()) }) candidate else body
    }

    companion object {
        /** Missing, invalid or unsupported fonts must not stop Home from opening. */
        fun loadOrFallback(context: Context, @FontRes id: Int, fallback: Typeface = Typeface.SANS_SERIF): Typeface =
            try {
                ResourcesCompat.getFont(context.applicationContext, id) ?: fallback
            } catch (_: RuntimeException) {
                fallback
            }
    }
}
