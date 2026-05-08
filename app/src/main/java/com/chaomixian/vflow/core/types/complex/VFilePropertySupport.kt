package com.chaomixian.vflow.core.types.complex

import android.net.Uri
import com.chaomixian.vflow.core.logging.LogManager
import java.io.File
import java.net.URLConnection
import java.util.Base64

internal object VFilePropertySupport {
    private fun localFile(uriString: String): File? {
        val javaUri = runCatching { java.net.URI(uriString) }.getOrNull()
        if (javaUri?.scheme == "file") {
            return runCatching { File(javaUri) }.getOrNull()
        }

        if (uriString.startsWith("/")) {
            return File(uriString)
        }

        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            return uri.path?.takeIf { it.isNotBlank() }?.let(::File)
        }

        return null
    }

    fun path(uriString: String): String {
        localFile(uriString)?.let { return it.path }

        val uri = Uri.parse(uriString)
        return if (uri.scheme == "file") uri.path ?: "" else uriString
    }

    fun name(uriString: String): String {
        localFile(uriString)?.name
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return runCatching { Uri.parse(uriString).lastPathSegment }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "unknown"
    }

    fun extension(uriString: String): String {
        return name(uriString).substringAfterLast('.', missingDelimiterValue = "")
    }

    fun size(uriString: String): Long? {
        return try {
            localFile(uriString)?.takeIf { it.exists() }?.length()
        } catch (_: Exception) {
            null
        }
    }

    fun mimeType(uriString: String, explicitMimeType: String? = null): String {
        if (!explicitMimeType.isNullOrBlank()) return explicitMimeType

        return try {
            val guessedMimeType = URLConnection.guessContentTypeFromName(path(uriString))
            if (!guessedMimeType.isNullOrBlank()) {
                return guessedMimeType
            }

            val uri = Uri.parse(uriString)
            if (uri.scheme == "content") {
                LogManager.applicationContext.contentResolver.getType(uri).orEmpty()
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun base64(uriString: String): String? {
        val bytes = try {
            val file = localFile(uriString)
            if (file != null) {
                file.readBytes()
            } else {
                val uri = Uri.parse(uriString)
                LogManager.applicationContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        } catch (_: Exception) {
            null
        }
        return bytes?.let { Base64.getEncoder().encodeToString(it) }
    }

    fun textContent(uriString: String): String? {
        return try {
            val file = localFile(uriString)
            if (file != null) {
                file.readText()
            } else {
                val uri = Uri.parse(uriString)
                LogManager.applicationContext.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            }
        } catch (_: Exception) {
            null
        }
    }
}
