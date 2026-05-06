package com.chaomixian.vflow.core.workflow.module

import com.chaomixian.vflow.core.types.VTypeRegistry
import com.chaomixian.vflow.core.workflow.module.core.CoreShellCommandModule
import com.chaomixian.vflow.core.workflow.module.shizuku.ShellCommandModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ShellCommandModuleOutputTest {

    @Test
    fun `shizuku shell command module exposes exit code output`() {
        val output = ShellCommandModule().getOutputs(null).firstOrNull { it.id == "exit_code" }

        assertNotNull(output)
        assertEquals(VTypeRegistry.NUMBER.id, output?.typeName)
    }

    @Test
    fun `core shell command module exposes exit code output`() {
        val output = CoreShellCommandModule().getOutputs(null).firstOrNull { it.id == "exit_code" }

        assertNotNull(output)
        assertEquals(VTypeRegistry.NUMBER.id, output?.typeName)
    }
}
