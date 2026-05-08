package com.chaomixian.vflow.core.telemetry

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.core.content.edit
import com.chaomixian.vflow.ui.main.MainActivity
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure

object TelemetryManager {

    private const val KEY_TELEMETRY_ENABLED = "telemetryEnabled"
    private const val APP_KEY = "69e62d246f259537c79bda50"
    private const val CHANNEL = "Umeng"

    fun preInit(context: Context) {
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        UMConfigure.setLogEnabled(debuggable)
        UMConfigure.preInit(context, APP_KEY, CHANNEL)
    }

    fun init(context: Context) {
        UMConfigure.submitPolicyGrantResult(context, true)
        UMConfigure.init(context, APP_KEY, CHANNEL, UMConfigure.DEVICE_TYPE_PHONE, "")
    }

    fun onKillProcess(context: Context) {
        MobclickAgent.onKillProcess(context)
    }

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_TELEMETRY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_TELEMETRY_ENABLED, enabled)
        }
        if (enabled) {
            init(context)
        }
    }
}
