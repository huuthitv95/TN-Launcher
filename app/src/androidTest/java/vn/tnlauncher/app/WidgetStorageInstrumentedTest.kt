package vn.tnlauncher.app

import android.content.ComponentName
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import vn.tnlauncher.app.data.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WidgetStorageInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val sample = HostedWidget(123456, ComponentName("vn.tnlauncher.fixture", "vn.tnlauncher.fixture.Provider"))
    @Test fun malformedWidgetRecordIsPreservedAndCannotBeOverwritten() = runBlocking {
        val name = "widget-record-${System.nanoTime()}"
        try {
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            assertTrue(prefs.edit().putString("state", "broken-record").commit())
            val repository = LauncherWidgetRepository(context, name)
            assertTrue(runCatching { repository.read() }.isFailure)
            assertTrue(runCatching { repository.begin(sample) }.isFailure)
            assertEquals("broken-record", prefs.getString("state", null))
        } finally { context.deleteSharedPreferences(name) }
    }
    @Test fun wrongTypePendingRecordIsPreservedAndCannotBeOverwritten() = runBlocking {
        listOf("\"broken\"", "[]", "12", "true").forEach { invalid ->
            val name = "widget-pending-type-${System.nanoTime()}"
            try {
                val raw = """{"version":1,"widgets":[],"pending":$invalid}"""
                val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                assertTrue(prefs.edit().putString("state", raw).commit())
                val repository = LauncherWidgetRepository(context, name)
                assertTrue(runCatching { repository.read() }.isFailure)
                assertTrue(runCatching { repository.begin(sample) }.isFailure)
                var allocations = 0
                assertTrue(runCatching {
                    repository.allocatePending(sample.provider, { allocations++; sample.id }, {})
                }.isFailure)
                assertEquals("Invalid stored state must be rejected before allocating an ID", 0, allocations)
                assertEquals(raw, prefs.getString("state", null))
            } finally { context.deleteSharedPreferences(name) }
        }
    }
    @Test fun cancellationDuringAllocationReleasesIdAndClearsOnlyItsPendingRecord() = runBlocking {
        val name = "widget-allocation-cancel-${System.nanoTime()}"
        val enteredAllocation = CountDownLatch(1)
        val releaseAllocation = CountDownLatch(1)
        val deleted = mutableListOf<Int>()
        val repository = LauncherWidgetRepository(context, name)
        val task = launch(Dispatchers.Default) {
            repository.allocatePending(sample.provider, {
                enteredAllocation.countDown()
                check(releaseAllocation.await(10, TimeUnit.SECONDS)) { "Allocation test was not released" }
                sample.id
            }, { deleted.add(it) })
        }
        try {
            assertTrue("The allocator must be running before cancellation", enteredAllocation.await(10, TimeUnit.SECONDS))
            // Cancel before IO returns. A bare withContext(IO) would lose its returned ID.
            task.cancel()
            releaseAllocation.countDown()
            task.join()
            assertTrue(task.isCancelled)
            assertEquals(listOf(sample.id), deleted)
            assertEquals(LauncherWidgets(), repository.read())
        } finally {
            releaseAllocation.countDown()
            task.cancelAndJoin()
            context.deleteSharedPreferences(name)
        }
    }
    @Test fun allocationPersistsPendingBeforeReturningToTheCaller() = runBlocking {
        val name = "widget-allocation-${System.nanoTime()}"
        try {
            val deleted = mutableListOf<Int>()
            val repository = LauncherWidgetRepository(context, name)
            assertEquals(sample, repository.allocatePending(sample.provider, { sample.id }, deleted::add))
            assertEquals(PendingWidget(sample, WidgetAddStage.BIND), LauncherWidgetRepository(context, name).read().pending)
            assertTrue(deleted.isEmpty())
            repository.discardIncomplete(sample.id, deleted::add)
            assertEquals(listOf(sample.id), deleted)
        } finally { context.deleteSharedPreferences(name) }
    }
    @Test fun finishingAnOldFlowNeverDeletesItsAlreadyCommittedWidget() = runBlocking {
        val name = "widget-complete-${System.nanoTime()}"
        try {
            val repository = LauncherWidgetRepository(context, name)
            repository.begin(sample); repository.complete(sample)
            var deletions = 0
            assertFalse(repository.discardIncomplete(sample.id) { deletions++ })
            assertEquals(0, deletions)
            assertEquals(listOf(sample), LauncherWidgetRepository(context, name).read().widgets)
        } finally { context.deleteSharedPreferences(name) }
    }
    @Test fun cleanupIsLimitedToItsOwnPendingAllocation() = runBlocking {
        val name = "widget-pending-${System.nanoTime()}"
        try {
            val repository = LauncherWidgetRepository(context, name)
            repository.begin(sample)
            val deleted = mutableListOf<Int>()
            assertTrue(repository.discardIncomplete(sample.id + 1, deleted::add))
            assertEquals(sample, repository.read().pending!!.widget)
            assertTrue(repository.discardIncomplete(sample.id, deleted::add))
            assertNull(repository.read().pending)
            assertEquals(listOf(sample.id + 1, sample.id), deleted)
        } finally { context.deleteSharedPreferences(name) }
    }
}
