package com.chaomixian.vflow.ui.shortcut_picker

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.chaomixian.vflow.services.ShellManager
import java.util.Locale

object ShortcutPickerSupport {
    private val shortcutBlockRegex = Regex(
        """ShortcutInfo.*flags.*\n\s+packageName=(.*)\n\s+activity=(.*)\n\s+shortLabel=(.*), resId=.*\n\s+longLabel=(.*), resId=.*\n\s+disabledMessage=(.*), resId=.*\n\s+disabledReason=(.*)\n\s+categories=(.*)\n\s+persons=(.*)\n\s+icon=(.*)\n\s+rank=(.*), timestamp=.*\n\s+intents=([\s\S]*?)extras=(.*)\n\s+iconRes="""
    )

    suspend fun loadShortcuts(context: Context): List<ShortcutPickerItem> {
        if (!ShellManager.isShizukuActive(context) && !ShellManager.isRootAvailable()) {
            return emptyList()
        }

        val output = ShellManager.execShellCommand(context, "dumpsys shortcut", ShellManager.ShellMode.AUTO)
        if (output.startsWith("Error:", ignoreCase = true) || !output.contains("Shortcuts:")) {
            return emptyList()
        }

        val packageManager = context.packageManager
        val defaultIcon = packageManager.defaultActivityIcon

        return shortcutBlockRegex.findAll(output)
            .mapNotNull { match ->
                val packageName = match.groupValues.getOrNull(1)?.trim().orEmpty()
                val activityName = match.groupValues.getOrNull(2)?.trim().orEmpty()
                val shortcutLabel = match.groupValues.getOrNull(3)?.trim().orEmpty()
                val rawIntent = match.groupValues.getOrNull(11).orEmpty()

                if (packageName.isBlank() || shortcutLabel.isBlank() || rawIntent.isBlank()) {
                    return@mapNotNull null
                }

                val launchCommand = buildLaunchCommand(rawIntent) ?: return@mapNotNull null
                ShortcutPickerItem(
                    appName = loadAppName(packageManager, packageName) ?: packageName,
                    packageName = packageName,
                    shortcutLabel = shortcutLabel,
                    activityName = activityName,
                    launchCommand = launchCommand,
                    icon = loadAppIcon(packageManager, packageName) ?: defaultIcon
                )
            }
            .distinctBy { it.stableId }
            .sortedWith(
                compareBy<ShortcutPickerItem> { it.appName.lowercase(Locale.getDefault()) }
                    .thenBy { it.shortcutLabel.lowercase(Locale.getDefault()) }
            )
            .toList()
    }

    internal fun buildLaunchCommand(rawIntentBlock: String): String? {
        val condensed = rawIntentBlock.replace(Regex("\n\\s+"), "")
        val match = Regex("""Intent \{(.*?)\}/(?:PersistableBundle\[(.*?)\]|null)\]""").find(condensed)
            ?: return null
        val intentData = readData(match.groupValues.getOrNull(1).orEmpty())
        val extraData = readData(match.groupValues.getOrNull(2).orEmpty().removePrefix("{").removeSuffix("}"))

        if (intentData.isEmpty()) {
            return null
        }

        return buildString {
            append("am start")
            intentData["act"]?.takeIf { it.isNotBlank() }?.let {
                append(" -a ")
                append(shellQuote(it))
            }
            intentData["cmp"]?.takeIf { it.isNotBlank() }?.let {
                append(" -n ")
                append(shellQuote(it))
            }
            intentData["dat"]?.takeIf { it.isNotBlank() }?.let {
                append(" -d ")
                append(shellQuote(it))
            }
            intentData["flg"]?.takeIf { it.isNotBlank() }?.let {
                append(" -f ")
                append(it)
            }
            extraData.forEach { (key, value) ->
                if (key.isBlank() || value.isBlank()) return@forEach
                when {
                    isBoolean(value) -> {
                        append(" --ez ")
                        append(shellQuote(key))
                        append(' ')
                        append(value.lowercase(Locale.ROOT))
                    }
                    isInteger(value) && value.length < 10 -> {
                        append(" --ei ")
                        append(shellQuote(key))
                        append(' ')
                        append(value)
                    }
                    isLong(value) -> {
                        append(" --el ")
                        append(shellQuote(key))
                        append(' ')
                        append(value)
                    }
                    isFloat(value) -> {
                        append(" --ef ")
                        append(shellQuote(key))
                        append(' ')
                        append(value)
                    }
                    else -> {
                        append(" --es ")
                        append(shellQuote(key))
                        append(' ')
                        append(shellQuote(value))
                    }
                }
            }
        }
    }

    internal fun readData(rawData: String): Map<String, String> {
        val normalized = rawData.replace(", ", " ")
        val key = StringBuilder()
        val value = StringBuilder()
        val result = linkedMapOf<String, String>()
        var readingKey = true
        var seenEquals = false

        normalized.forEach { char ->
            when {
                char == ' ' -> {
                    if (key.isNotEmpty() && value.isNotEmpty()) {
                        result[key.toString()] = value.toString()
                        key.clear()
                        value.clear()
                        readingKey = true
                        seenEquals = false
                    }
                }
                char == '=' && !seenEquals -> {
                    seenEquals = true
                    readingKey = false
                }
                readingKey -> key.append(char)
                else -> value.append(char)
            }
        }

        if (key.isNotEmpty() && value.isNotEmpty()) {
            result[key.toString()] = value.toString()
        }

        return result
    }

    private fun loadAppName(packageManager: PackageManager, packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun loadAppIcon(packageManager: PackageManager, packageName: String): Drawable? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationIcon(appInfo)
        } catch (_: Exception) {
            null
        }
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

    private fun isBoolean(value: String): Boolean = value == "true" || value == "false"

    private fun isInteger(value: String): Boolean = value.toIntOrNull() != null

    private fun isLong(value: String): Boolean = value.toLongOrNull() != null

    private fun isFloat(value: String): Boolean = value.toFloatOrNull() != null
}
