package com.chaomixian.vflow.ui.shortcut_picker

import android.graphics.drawable.Drawable

data class ShortcutPickerItem(
    val appName: String,
    val packageName: String,
    val shortcutLabel: String,
    val activityName: String,
    val launchCommand: String,
    val icon: Drawable?
) {
    val stableId: String
        get() = "$packageName|$shortcutLabel|$activityName|$launchCommand"
}
