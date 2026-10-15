package vn.tnlauncher.app

import org.junit.Assert.*
import org.junit.Test
import vn.tnlauncher.app.data.*
import vn.tnlauncher.app.integration.AppManagementRules

class LauncherFolderRulesTest {
    private val a = "com.a/com.a.Main"
    private val b = "com.b/com.b.Main"
    private val first = "folder:00000000-0000-0000-0000-000000000001"
    private val second = "folder:00000000-0000-0000-0000-000000000002"
    @Test fun folderCodecRoundTripsVietnameseAndMissingPackages() {
        val folders = listOf(LauncherFolder(first, "Điều hướng · Đường", listOf(a, b)))
        assertEquals(folders, FolderCodec.decode(FolderCodec.encode(folders)))
        assertEquals(listOf(first, a), LauncherRules.validHomeIds("$first\n$a\n../private"))
        assertTrue(LauncherRules.validIds(first).isEmpty())
    }
    @Test fun corruptFolderRowsAndUnsafeIdsCannotCreateLaunchTargets() {
        assertTrue(FolderCodec.decode("folder:../private\t@@@\t$a").isEmpty())
        assertTrue(FolderCodec.decode("truncated").isEmpty())
        val normalized = FolderRules.normalize(listOf(LauncherFolder(first, "\t\n", listOf(a, a, "file:/private"))))
        assertEquals("Thư mục", normalized.single().title)
        assertEquals(listOf(a), normalized.single().appIds)
    }
    @Test fun membershipIsUniqueAndAssignMovesBetweenFolders() {
        val original = listOf(LauncherFolder(first, "Một", listOf(a)), LauncherFolder(second, "Hai", listOf(b)))
        val moved = FolderRules.assign(original, LauncherFolder(second, "Hai", listOf(a, b)))
        assertTrue(moved[0].appIds.isEmpty())
        assertEquals(listOf(a, b), moved[1].appIds)
    }
    @Test fun folderOrderMissingMembersAndDeletionPreserveApplications() {
        val settings = LauncherSettings(homeOrder = listOf(b, first), folders = listOf(LauncherFolder(first, "Đi đường", listOf(a))))
        assertEquals(listOf(b, first), FolderRules.visibleIds(listOf(a, b), settings))
        assertEquals(listOf(b, first), FolderRules.visibleIds(listOf(b), settings))
        assertEquals(listOf(b, a), FolderRules.visibleIds(listOf(a, b), settings.copy(folders = emptyList())))
        assertEquals(listOf(a), settings.folders.single().appIds)
    }
    @Test fun boundedFoldersAndTitlesAvoidUnboundedPreferenceGrowth() {
        val folders = (1..100).map { LauncherFolder("folder:00000000-0000-0000-0000-${it.toString().padStart(12, '0')}", "Đ".repeat(100), listOf(a)) }
        val normalized = FolderRules.normalize(folders)
        assertEquals(64, normalized.size); assertEquals(80, normalized.first().title.length)
        assertEquals(1, normalized.flatMap { it.appIds }.size)
    }
    @Test fun uninstallNeverTargetsSystemSelfOrOriginalVendorPackages() {
        assertFalse(AppManagementRules.mayUninstall("vn.tnlauncher.app", "vn.tnlauncher.app", false))
        assertFalse(AppManagementRules.mayUninstall("vn.tnlauncher.app", "com.android.settings", true))
        assertFalse(AppManagementRules.mayUninstall("vn.tnlauncher.app", "com.android.launcher8", false))
        assertFalse(AppManagementRules.mayUninstall("vn.tnlauncher.app", "com.syu.fourcamera2", false))
        assertFalse(AppManagementRules.mayUninstall("vn.tnlauncher.app", "com.fyt.service", false))
        assertTrue(AppManagementRules.mayUninstall("vn.tnlauncher.app", "org.example.userapp", false))
    }
}
