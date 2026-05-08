package com.chaomixian.vflow.server.wrappers.root

import com.chaomixian.vflow.server.common.Logger
import com.chaomixian.vflow.server.wrappers.ServiceWrapper
import org.json.JSONObject
import java.lang.reflect.Method

class IHotspotManagerWrapper : ServiceWrapper("wifi", "android.net.wifi.IWifiManager\$Stub", "hotspot") {

    companion object {
        private const val HOTSPOT_DISABLED = 11
        private const val HOTSPOT_ENABLED = 13
    }

    private var startTetheredHotspotMethod: Method? = null
    private var startSoftApMethod: Method? = null
    private var stopSoftApMethod: Method? = null
    private var getWifiApEnabledStateMethod: Method? = null

    override fun onServiceConnected(service: Any) {
        val clazz = service.javaClass
        startTetheredHotspotMethod = clazz.methods.find { it.name == "startTetheredHotspot" }
        startSoftApMethod = clazz.methods.find { it.name == "startSoftAp" }
        stopSoftApMethod = clazz.methods.find { it.name == "stopSoftAp" }
        getWifiApEnabledStateMethod = clazz.methods.find { it.name == "getWifiApEnabledState" }

        Logger.debug("HotspotManager", "=== Hotspot Manager Methods ===")
        Logger.debug("HotspotManager", "startTetheredHotspot: ${startTetheredHotspotMethod != null}")
        if (startTetheredHotspotMethod != null) {
            Logger.debug("HotspotManager", "startTetheredHotspot params: ${startTetheredHotspotMethod!!.parameterTypes.toList()}")
        }
        Logger.debug("HotspotManager", "startSoftAp: ${startSoftApMethod != null}")
        if (startSoftApMethod != null) {
            Logger.debug("HotspotManager", "startSoftAp params: ${startSoftApMethod!!.parameterTypes.toList()}")
        }
        Logger.debug("HotspotManager", "stopSoftAp: ${stopSoftApMethod != null}")
        if (stopSoftApMethod != null) {
            Logger.debug("HotspotManager", "stopSoftAp params: ${stopSoftApMethod!!.parameterTypes.toList()}")
        }
        Logger.debug("HotspotManager", "getWifiApEnabledState: ${getWifiApEnabledStateMethod != null}")
        if (getWifiApEnabledStateMethod != null) {
            Logger.debug("HotspotManager", "getWifiApEnabledState params: ${getWifiApEnabledStateMethod!!.parameterTypes.toList()}")
        }
    }

    override fun handle(method: String, params: JSONObject): JSONObject {
        if (!isAvailable) {
            return JSONObject()
                .put("success", false)
                .put("error", "Hotspot service is not available or no permission")
        }

        return when (method) {
            "setEnabled" -> JSONObject().put("success", setHotspotEnabled(params.getBoolean("enabled")))
            "isEnabled" -> JSONObject().put("success", true).put("enabled", isHotspotEnabled())
            "toggle" -> {
                val currentState = isHotspotEnabled()
                val newState = !currentState
                val success = setHotspotEnabled(newState)
                JSONObject()
                    .put("success", success)
                    .put("enabled", if (success) newState else currentState)
            }
            else -> JSONObject()
                .put("success", false)
                .put("error", "Unknown method: $method")
        }
    }

    private fun setHotspotEnabled(enable: Boolean): Boolean {
        return try {
            if (enable) startHotspot() else stopHotspot()
        } catch (e: Exception) {
            Logger.error("HotspotManager", "setHotspotEnabled failed: ${e.message}", e)
            false
        }
    }

    private fun isHotspotEnabled(): Boolean {
        val method = getWifiApEnabledStateMethod ?: return false
        return try {
            val state = invokeWithDefaultArgs(method, serviceInterface) as? Int ?: -1
            Logger.info("HotspotManager", "Hotspot state: $state")
            state == HOTSPOT_ENABLED
        } catch (e: Exception) {
            Logger.error("HotspotManager", "getWifiApEnabledState failed: ${e.message}", e)
            false
        }
    }

    private fun startHotspot(): Boolean {
        startTetheredHotspotMethod?.let { method ->
            if (invokeHotspotStart(method)) {
                Logger.info("HotspotManager", "Hotspot started via startTetheredHotspot")
                return true
            }
        }

        startSoftApMethod?.let { method ->
            if (invokeHotspotStart(method)) {
                Logger.info("HotspotManager", "Hotspot started via startSoftAp")
                return true
            }
        }

        Logger.warn("HotspotManager", "No usable method available to start hotspot")
        return false
    }

    private fun stopHotspot(): Boolean {
        val method = stopSoftApMethod ?: return false
        return try {
            invokeWithDefaultArgs(method, serviceInterface)
            Logger.info("HotspotManager", "Hotspot stopped via stopSoftAp")
            true
        } catch (e: Exception) {
            Logger.error("HotspotManager", "stopSoftAp failed: ${e.message}", e)
            false
        }
    }

    private fun invokeHotspotStart(method: Method): Boolean {
        return try {
            val result = invokeWithDefaultArgs(method, serviceInterface)
            when (result) {
                is Boolean -> result
                is Int -> result == 0 || result == 1
                else -> true
            }
        } catch (e: Exception) {
            Logger.error("HotspotManager", "invokeHotspotStart failed for ${method.name}: ${e.message}", e)
            false
        }
    }

    private fun invokeWithDefaultArgs(method: Method, target: Any?): Any? {
        val args = method.parameterTypes.map { parameterType ->
            buildDefaultArg(parameterType)
        }.toTypedArray()
        return method.invoke(target, *args)
    }

    private fun buildDefaultArg(parameterType: Class<*>): Any? {
        return when {
            parameterType == Boolean::class.javaPrimitiveType || parameterType == Boolean::class.java -> false
            parameterType == Int::class.javaPrimitiveType || parameterType == Int::class.java -> 0
            parameterType == Long::class.javaPrimitiveType || parameterType == Long::class.java -> 0L
            parameterType == Float::class.javaPrimitiveType || parameterType == Float::class.java -> 0f
            parameterType == Double::class.javaPrimitiveType || parameterType == Double::class.java -> 0.0
            parameterType == Char::class.javaPrimitiveType || parameterType == Char::class.java -> '\u0000'
            parameterType == Byte::class.javaPrimitiveType || parameterType == Byte::class.java -> 0.toByte()
            parameterType == Short::class.javaPrimitiveType || parameterType == Short::class.java -> 0.toShort()
            parameterType == String::class.java || CharSequence::class.java.isAssignableFrom(parameterType) -> "com.android.shell"
            parameterType.name == "android.net.wifi.SoftApConfiguration" -> createSoftApConfiguration()
            parameterType.name == "android.os.WorkSource" -> null
            else -> null
        }
    }

    private fun createSoftApConfiguration(): Any? {
        return try {
            val builderClass = Class.forName("android.net.wifi.SoftApConfiguration\$Builder")
            val builder = builderClass.getDeclaredConstructor().newInstance()
            val buildMethod = builderClass.getDeclaredMethod("build")
            buildMethod.invoke(builder)
        } catch (e: Exception) {
            Logger.warn("HotspotManager", "Unable to create SoftApConfiguration: ${e.message}")
            null
        }
    }
}
