package com.chaomixian.vflow.core.workflow

import android.content.Context
import androidx.core.content.edit
import com.chaomixian.vflow.core.types.VObject
import com.chaomixian.vflow.core.types.VObjectFactory
import com.chaomixian.vflow.core.types.basic.VBoolean
import com.chaomixian.vflow.core.types.basic.VNumber
import com.chaomixian.vflow.core.types.basic.VString
import com.google.gson.Gson

object GlobalVariableStore {
    private const val PREFS_NAME = "global_variable_store"
    private const val KEY_VARIABLES_JSON = "variables_json"

    private val gson = Gson()

    private data class StoredVariable(
        val name: String,
        val type: String,
        val value: Any?
    )

    fun getAll(context: Context): Map<String, VObject> {
        val json = prefs(context).getString(KEY_VARIABLES_JSON, null).orEmpty()
        if (json.isBlank()) return emptyMap()

        return runCatching {
            val stored = gson.fromJson(json, Array<StoredVariable>::class.java).orEmpty()
            stored.associate { entry -> entry.name to deserialize(entry) }
        }.getOrDefault(emptyMap())
    }

    fun get(context: Context, key: String): VObject {
        return getAll(context)[key] ?: VObjectFactory.from(null)
    }

    fun put(context: Context, key: String, value: Any?) {
        val current = getAll(context).toMutableMap()
        current[key] = VObjectFactory.from(value)
        save(context, current)
    }

    fun remove(context: Context, key: String) {
        val current = getAll(context).toMutableMap()
        if (current.remove(key) != null) {
            save(context, current)
        }
    }

    fun replaceAll(context: Context, values: Map<String, VObject>) {
        save(context, values)
    }

    private fun save(context: Context, values: Map<String, VObject>) {
        val stored = values.entries.map { (name, value) ->
            serialize(name, value)
        }
        prefs(context).edit(commit = true) {
            putString(KEY_VARIABLES_JSON, gson.toJson(stored))
        }
    }

    private fun serialize(name: String, value: VObject): StoredVariable {
        return when (value) {
            is VString -> StoredVariable(name, "string", value.raw)
            is VNumber -> StoredVariable(name, "number", value.raw.toString())
            is VBoolean -> StoredVariable(name, "boolean", value.raw)
            else -> StoredVariable(name, "string", value.asString())
        }
    }

    private fun deserialize(item: StoredVariable): VObject {
        return when (item.type) {
            "string" -> VString(item.value?.toString() ?: "")
            "number" -> (item.value as? Number)?.let { VNumber(it) }
                ?: item.value?.toString()?.toDoubleOrNull()?.let { VNumber(it) }
                ?: VNumber(0)
            "boolean" -> when (item.value) {
                is Boolean -> VBoolean(item.value)
                else -> VBoolean(item.value?.toString()?.toBoolean() ?: false)
            }
            else -> VString(item.value?.toString() ?: "")
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
