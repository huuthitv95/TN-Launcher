package vn.tnlauncher.app.data

import java.util.Base64

data class LauncherFolder(val id: String, val title: String, val appIds: List<String>)

object FolderRules {
    const val MAX_FOLDERS = 64
    fun isFolderId(id: String) = id.matches(Regex("folder:[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"))
    fun normalize(folders: List<LauncherFolder>): List<LauncherFolder> {
        val used = mutableSetOf<String>()
        return folders.filter { isFolderId(it.id) }.distinctBy { it.id }.take(MAX_FOLDERS).map { folder ->
            folder.copy(title = folder.title.trim().replace(Regex("[\\p{Cntrl}]"), " ").take(80).ifBlank { "Thư mục" },
                appIds = LauncherRules.validIds(folder.appIds.joinToString("\n")).take(128).filter { used.add(it) })
        }
    }
    fun assign(folders: List<LauncherFolder>, folder: LauncherFolder): List<LauncherFolder> {
        val ids = folder.appIds.toSet()
        val existing = folders.filter { it.id != folder.id }.map { it.copy(appIds = it.appIds.filterNot(ids::contains)) }
        val index = folders.indexOfFirst { it.id == folder.id }.takeIf { it >= 0 } ?: existing.size
        return normalize(existing.toMutableList().apply { add(index.coerceAtMost(size), folder) })
    }
    fun visibleIds(installed: List<String>, settings: LauncherSettings): List<String> {
        val folders = normalize(settings.folders)
        val grouped = folders.flatMap { it.appIds }.toSet()
        val visible = installed.filterNot(grouped::contains) + folders.map { it.id }
        return LauncherRules.orderedIds(visible, settings.homeOrder)
    }
}

// A canonical, line-based private preference value: no external paths or serialized Intents.
object FolderCodec {
    fun encode(folders: List<LauncherFolder>): String = FolderRules.normalize(folders).joinToString("\n") {
        "${it.id}\t${Base64.getEncoder().encodeToString(it.title.toByteArray(Charsets.UTF_8))}\t${it.appIds.joinToString(",")}"
    }
    fun decode(raw: String): List<LauncherFolder> = FolderRules.normalize(raw.lines().mapNotNull { line ->
        val parts = line.split('\t')
        if (parts.size != 3) null else try {
            LauncherFolder(parts[0], String(Base64.getDecoder().decode(parts[1]), Charsets.UTF_8), parts[2].split(',').filter(String::isNotEmpty))
        } catch (_: IllegalArgumentException) { null }
    })
}
