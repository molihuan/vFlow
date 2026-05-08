package com.chaomixian.vflow.ui.common

import android.content.Context

object OverlayUiPreferences {
    const val PREFS_NAME = "vFlowPrefs"
    const val KEY_ALLOW_SHOW_ON_LOCK_SCREEN = "allowShowOnLockScreen"
    const val KEY_ALLOW_POPUP_KEEP_SCREEN_ON = "allowPopupKeepScreenOn"

    fun isShowOnLockScreenAllowed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ALLOW_SHOW_ON_LOCK_SCREEN, false)
    }

    fun isPopupKeepScreenOnAllowed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ALLOW_POPUP_KEEP_SCREEN_ON, false)
    }
}
