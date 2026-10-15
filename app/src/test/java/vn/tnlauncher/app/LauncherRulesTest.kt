package vn.tnlauncher.app

import org.junit.Assert.*
import org.junit.Test
import vn.tnlauncher.app.data.LauncherRules
import vn.tnlauncher.app.integration.*

class LauncherRulesTest {
    @Test fun vietnameseSearchAndCase() {
        assertEquals("dieu huong ban do", LauncherRules.searchKey("ĐIỀU HƯỚNG Bản đồ"))
        assertEquals(LauncherRules.searchKey("Ca\u0300i đa\u0323t"), LauncherRules.searchKey("Cài đặt"))
    }
    @Test fun savedOrderingRetainsOnlyAvailableAndAppendsNewInOrder() {
        assertEquals(listOf("b", "a", "c"), LauncherRules.orderedIds(listOf("a", "b", "c"), listOf("missing", "b", "b")))
    }
    @Test fun pinIsUniqueAndBounded() {
        val full = (1..6).map(Int::toString)
        assertEquals(full, LauncherRules.pin(full, "7"))
        assertEquals(full, LauncherRules.pin(full, "3"))
    }
    @Test fun subsetReorderPreservesMissingAndOtherPages() {
        assertEquals(listOf("missing", "c", "b", "a", "d"),
            LauncherRules.reorderSubset(listOf("missing", "a", "b", "c", "d"), listOf("c", "b", "a")))
    }
    @Test fun moveClampsAndIgnoresUnknown() {
        assertEquals(listOf("b", "a", "c"), LauncherRules.move(listOf("a", "b", "c"), "a", 1))
        assertEquals(listOf("a"), LauncherRules.move(listOf("a"), "a", -1))
        assertEquals(emptyList<String>(), LauncherRules.move(emptyList(), "a", 1))
    }
    @Test fun damagedIdsAreRejectedWithoutInventingApps() {
        assertEquals(listOf("com.test/com.test.Main"), LauncherRules.validIds("nonsense\ncom.test/com.test.Main\ncom.test/com.test.Main\n../private"))
    }
    @Test fun capabilityRequiresEnabledExportedAndPermission() {
        assertEquals(Availability.AVAILABLE, ActivityAccess.availability(true, true, true))
        assertEquals(Availability.COMPONENT_UNAVAILABLE, ActivityAccess.availability(true, false, true))
        assertEquals(Availability.COMPONENT_UNAVAILABLE, ActivityAccess.availability(false, true, true))
        assertEquals(Availability.PERMISSION_REQUIRED, ActivityAccess.availability(true, true, false))
    }
    @Test fun launchErrorsAreTyped() {
        assertEquals(LaunchResult.Denied, ActivityAccess.runStart { throw SecurityException() })
        assertTrue(ActivityAccess.runStart { throw IllegalStateException() } is LaunchResult.Error)
        assertEquals(LaunchResult.Opened, ActivityAccess.runStart { })
    }
}
