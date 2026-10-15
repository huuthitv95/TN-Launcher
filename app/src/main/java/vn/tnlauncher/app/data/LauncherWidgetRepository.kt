package vn.tnlauncher.app.data

import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class HostedWidget(val id: Int, val provider: ComponentName)
enum class WidgetAddStage { BIND, CONFIGURE }
data class PendingWidget(val widget: HostedWidget, val stage: WidgetAddStage)
data class LauncherWidgets(val widgets: List<HostedWidget> = emptyList(), val pending: PendingWidget? = null)

/** Host bookkeeping only; Android owns binding permission and the actual RemoteViews. */
class LauncherWidgetRepository(context: Context, storeName: String = "widgets") {
    companion object {
        const val HOST_ID = 0x544E01
        const val MAX_WIDGETS = 12
        private val lock = Mutex()
    }
    private val prefs = context.applicationContext.getSharedPreferences(storeName, Context.MODE_PRIVATE)

    private fun decodeWidget(value: JSONObject): HostedWidget {
        val id = value.getInt("id")
        val provider = ComponentName.unflattenFromString(value.getString("provider"))
        check(id > 0 && provider != null) { "Invalid saved widget" }
        return HostedWidget(id, provider)
    }
    private fun readState(): LauncherWidgets {
        val raw = prefs.getString("state", null) ?: return LauncherWidgets()
        val json = JSONObject(raw)
        check(json.getInt("version") == 1) { "Unsupported widget state" }
        val widgets = json.getJSONArray("widgets").let { array ->
            (0 until array.length()).map { decodeWidget(array.getJSONObject(it)) }
        }
        val pending = if (json.has("pending") && !json.isNull("pending")) {
            // A wrong-type record must fail closed, not be overwritten as an absent flow.
            val value = json.getJSONObject("pending")
            PendingWidget(decodeWidget(value), WidgetAddStage.valueOf(value.getString("stage")))
        } else null
        check(widgets.size <= MAX_WIDGETS && widgets.map { it.id }.distinct().size == widgets.size)
        check(pending == null || widgets.none { it.id == pending.widget.id })
        return LauncherWidgets(widgets, pending)
    }
    private fun encodeWidget(widget: HostedWidget) = JSONObject()
        .put("id", widget.id).put("provider", widget.provider.flattenToString())
    private fun writeState(state: LauncherWidgets) {
        val json = JSONObject().put("version", 1).put("widgets", JSONArray().apply {
            state.widgets.forEach { put(encodeWidget(it)) }
        })
        state.pending?.let { json.put("pending", encodeWidget(it.widget).put("stage", it.stage.name)) }
        if (!prefs.edit().putString("state", json.toString()).commit()) throw IOException("Widget state was not saved")
    }
    suspend fun read(): LauncherWidgets = withContext(Dispatchers.IO) { lock.withLock { readState() } }
    private suspend fun update(transform: (LauncherWidgets) -> LauncherWidgets): LauncherWidgets =
        withContext(Dispatchers.IO) { lock.withLock { transform(readState()).also(::writeState) } }

    /** Capture allocation before crossing a cancellable dispatcher boundary. */
    suspend fun allocatePending(provider: ComponentName, allocateId: () -> Int, deleteId: (Int) -> Unit): HostedWidget {
        var allocatedId: Int? = null
        try {
            val widget = withContext(NonCancellable + Dispatchers.IO) {
                lock.withLock {
                    val current = readState()
                    check(current.pending == null && current.widgets.size < MAX_WIDGETS)
                    val id = allocateId()
                    // Never treat a previously saved ID as a new, disposable allocation.
                    check(id > 0 && current.widgets.none { it.id == id })
                    allocatedId = id
                    HostedWidget(id, provider).also {
                        writeState(current.copy(pending = PendingWidget(it, WidgetAddStage.BIND)))
                    }
                }
            }
            // Also handle callers already on IO, where withContext may not dispatch back.
            currentCoroutineContext().ensureActive()
            return widget
        } catch (error: Throwable) {
            allocatedId?.let { id ->
                try {
                    withContext(NonCancellable + Dispatchers.IO) { discardIncomplete(id, deleteId) }
                } catch (cleanup: Throwable) { error.addSuppressed(cleanup) }
            }
            throw error
        }
    }

    suspend fun begin(widget: HostedWidget) = update {
        check(it.pending == null && it.widgets.size < MAX_WIDGETS)
        check(widget.id > 0 && it.widgets.none { saved -> saved.id == widget.id })
        it.copy(pending = PendingWidget(widget, WidgetAddStage.BIND))
    }
    suspend fun configuring(id: Int) = update {
        val pending = checkNotNull(it.pending)
        check(pending.widget.id == id)
        it.copy(pending = pending.copy(stage = WidgetAddStage.CONFIGURE))
    }
    suspend fun complete(widget: HostedWidget) = update {
        check(it.pending?.widget == widget && it.widgets.size < MAX_WIDGETS)
        it.copy(widgets = it.widgets + widget, pending = null)
    }
    suspend fun clearPending(id: Int) = update {
        check(it.pending == null || it.pending.widget.id == id)
        it.copy(pending = null)
    }
    suspend fun discardIncomplete(id: Int, deleteId: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        lock.withLock {
            val current = readState()
            if (current.widgets.any { it.id == id }) return@withLock false
            deleteId(id)
            if (current.pending?.widget?.id == id) writeState(current.copy(pending = null))
            true
        }
    }
    suspend fun remove(id: Int) = update { it.copy(widgets = it.widgets.filterNot { widget -> widget.id == id }) }
}
