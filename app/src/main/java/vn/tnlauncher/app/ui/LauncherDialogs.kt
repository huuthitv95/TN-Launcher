package vn.tnlauncher.app.ui

import androidx.appcompat.app.AlertDialog

/** Native dialog button layouts can override theme minimums; enforce the launcher touch target. */
fun AlertDialog.Builder.showLauncherDialog(): AlertDialog = create().also { dialog ->
    dialog.setOnShowListener {
        val target = (56 * dialog.context.resources.displayMetrics.density).toInt()
        listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL).forEach { which ->
            dialog.getButton(which)?.apply {
                minHeight = target; minimumHeight = target
                layoutParams = layoutParams.apply { height = maxOf(target, measuredHeight) }
            }
        }
    }
    dialog.show()
}
