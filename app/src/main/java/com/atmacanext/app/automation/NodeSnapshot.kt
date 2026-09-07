package com.atmacanext.app.automation

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

data class NodeSnapshot(
    val text: String?,
    val contentDescription: String?,
    val viewId: String?,
    val className: String?,
    val clickable: Boolean,
    val enabled: Boolean,
    val bounds: Rect,
    val scrollable: Boolean = false,
    val selected: Boolean = false,
    val checked: Boolean = false,
    val editable: Boolean = false,
    val visible: Boolean = true,
)

fun AccessibilityNodeInfo.toSnapshot(): NodeSnapshot {
    val rect = Rect()
    getBoundsInScreen(rect)
    return NodeSnapshot(
        text = text?.toString(),
        contentDescription = contentDescription?.toString(),
        viewId = viewIdResourceName,
        className = className?.toString(),
        clickable = isClickable,
        enabled = isEnabled,
        bounds = rect,
        scrollable = isScrollable,
        selected = isSelected,
        checked = isChecked,
        editable = isEditable,
        visible = isVisibleToUser,
    )
}
