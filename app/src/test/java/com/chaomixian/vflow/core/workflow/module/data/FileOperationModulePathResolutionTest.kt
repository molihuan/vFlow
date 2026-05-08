package com.chaomixian.vflow.core.workflow.module.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileOperationModulePathResolutionTest {

    @Test
    fun `resolvePathInput expands template variables before validation`() {
        val result = FileOperationModule().resolvePathInput("{{base}}/notes.txt") { input ->
            input.replace("{{base}}", "/sdcard/Documents")
        }

        assertEquals("/sdcard/Documents/notes.txt", result)
    }

    @Test
    fun `resolvePathInput returns null for blank resolved path`() {
        val result = FileOperationModule().resolvePathInput("{{base}}") { _ -> "   " }

        assertNull(result)
    }
}
