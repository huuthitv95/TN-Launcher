package vn.tnlauncher.app.data

import java.text.Normalizer
import java.util.Locale

enum class ThemeMode { SYSTEM, LIGHT, DARK }
data class LauncherSettings(
    val homeOrder: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val wallpaper: String? = null,
    val reducedMotion: Boolean = false,
    val recoveryNotice: String? = null,
    val folders: List<LauncherFolder> = emptyList(),
    val systemWallpaper: Boolean = false,
)

object LauncherRules {
    const val MAX_FAVORITES = 6
    fun searchKey(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "").replace('đ', 'd').replace('Đ', 'D').lowercase(Locale.ROOT)
    fun validIds(value: String): List<String> = value.lines()
        .filter { it.matches("[A-Za-z_][A-Za-z0-9_$]*(?:\\.[A-Za-z_][A-Za-z0-9_$]*)*/\\.?[A-Za-z_][A-Za-z0-9_$]*(?:\\.[A-Za-z_][A-Za-z0-9_$]*)*".toRegex()) }.distinct()
    fun validHomeIds(value: String): List<String> = value.lines()
        .filter { FolderRules.isFolderId(it) || validIds(it).isNotEmpty() }.distinct()
    fun orderedIds(installed: List<String>, saved: List<String>): List<String> =
        saved.filter { it in installed }.distinct() + installed.filter { it !in saved }.distinct()
    fun pin(favorites: List<String>, id: String): List<String> = (favorites + id).distinct().take(MAX_FAVORITES)
    fun move(ids: List<String>, id: String, delta: Int): List<String> {
        val from = ids.indexOf(id)
        if (from < 0) return ids
        val to = (from + delta).coerceIn(0, ids.lastIndex)
        return ids.toMutableList().apply { add(to, removeAt(from)) }
    }
    // Replace the visible subset in situ; preserve saved entries for temporarily absent packages.
    fun reorderSubset(all: List<String>, reordered: List<String>): List<String> {
        val subset = reordered.toSet()
        val queue = reordered.distinct().iterator()
        return all.map { if (it in subset && queue.hasNext()) queue.next() else it }
    }
}
