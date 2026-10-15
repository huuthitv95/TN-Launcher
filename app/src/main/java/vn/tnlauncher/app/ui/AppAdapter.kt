package vn.tnlauncher.app.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import vn.tnlauncher.app.data.LauncherApp
import vn.tnlauncher.app.databinding.ItemAppBinding

class AppAdapter(private val dock: Boolean, private val click: (LauncherApp) -> Unit,
    private val menu: (LauncherApp) -> Unit, private val reordered: (List<String>) -> Unit,
    private var typography: LauncherTypography? = null) : RecyclerView.Adapter<AppAdapter.Holder>() {
    private val items = mutableListOf<LauncherApp>()
    var textColor: Int = Color.WHITE
    var draggingEnabled = true
    private var dirty = false
    private var dragInProgress = false
    private var pointerDown = false
    private var pending: Pair<List<LauncherApp>, Int>? = null
    private var selectedHolder: RecyclerView.ViewHolder? = null
    private var suppressClick = false
    class Holder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)
    fun submit(apps: List<LauncherApp>, color: Int) {
        if (dirty || dragInProgress || pointerDown) { pending = apps to color; return }
        if (items == apps && textColor == color) return
        items.clear(); items.addAll(apps); textColor = color; notifyDataSetChanged()
    }
    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        if (typography == null) typography = LauncherTypography(parent.context)
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (dock) binding.root.layoutParams = RecyclerView.LayoutParams(
            (96 * parent.resources.displayMetrics.density).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
        else binding.root.layoutParams.height = (112 * parent.resources.displayMetrics.density *
            maxOf(1f, parent.resources.configuration.fontScale)).toInt()
        return Holder(binding)
    }
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.icon.setImageDrawable(item.icon ?: holder.itemView.context.getDrawable(android.R.drawable.sym_def_app_icon))
        holder.binding.label.text = item.label
        holder.binding.label.setTextColor(textColor)
        holder.binding.label.typeface = typography?.body ?: Typeface.SANS_SERIF
        holder.itemView.contentDescription = item.label
        holder.itemView.setOnClickListener { if (!suppressClick) click(item) }
        holder.itemView.setOnLongClickListener { if (!draggingEnabled && !dragInProgress) menu(item); true }
        androidx.core.view.ViewCompat.replaceAccessibilityAction(holder.itemView,
            androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
            "Tùy chỉnh ${item.label}") { _, _ -> menu(item); true }
    }
    fun attachDrag(recycler: RecyclerView) {
        val flags = if (dock) ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT else ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(flags, 0) {
            override fun isLongPressDragEnabled() = draggingEnabled
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                if (!draggingEnabled) return false
                val a = from.bindingAdapterPosition; val b = to.bindingAdapterPosition
                if (a !in items.indices || b !in items.indices) return false
                items.add(b, items.removeAt(a)); dirty = true; notifyItemMoved(a, b); return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
            override fun onSelectedChanged(holder: RecyclerView.ViewHolder?, actionState: Int) {
                dragInProgress = actionState == ItemTouchHelper.ACTION_STATE_DRAG
                if (dragInProgress) {
                    suppressClick = true
                    selectedHolder = holder; holder?.itemView?.cancelLongPress()
                }
                super.onSelectedChanged(holder, actionState)
            }
            override fun clearView(rv: RecyclerView, holder: RecyclerView.ViewHolder) {
                super.clearView(rv, holder)
                if (selectedHolder !== holder) return
                selectedHolder = null
                if (dirty) { dirty = false; reordered(items.map { it.id }) }
                else items.getOrNull(holder.bindingAdapterPosition)?.let { item ->
                    // Finish dispatching ACTION_UP before opening a new window.
                    rv.post { menu(item) }
                }
                rv.post { suppressClick = false }
                pointerDown = false
                // The persisted reorder will publish the authoritative list next.
                pending = null
            }
        }).also { helper ->
            helper.attachToRecyclerView(recycler)
            recycler.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                private var downX = 0f; private var downY = 0f; private var downAt = 0L
                override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                    when (e.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            suppressClick = false
                            pointerDown = true
                            downX = e.x; downY = e.y; downAt = e.eventTime
                            // Keep the enclosing Dock scroller from stealing a held drag.
                            if (dock) rv.parent.requestDisallowInterceptTouchEvent(true)
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val distance = kotlin.math.hypot(e.x - downX, e.y - downY)
                            if (dock && e.eventTime - downAt < android.view.ViewConfiguration.getLongPressTimeout() && distance > android.view.ViewConfiguration.get(rv.context).scaledTouchSlop) {
                                rv.parent.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            pointerDown = false
                            downAt = Long.MAX_VALUE
                            if (dock) rv.parent.requestDisallowInterceptTouchEvent(false)
                            if (!dragInProgress) {
                                val latest = pending; pending = null
                                if (latest != null) rv.post { submit(latest.first, latest.second) }
                            }
                        }
                    }
                    return false
                }
            })
        }
    }
}
