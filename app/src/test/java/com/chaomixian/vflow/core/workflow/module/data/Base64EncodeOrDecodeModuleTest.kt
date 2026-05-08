package com.chaomixian.vflow.core.workflow.module.data

import android.content.ContextWrapper
import com.chaomixian.vflow.core.execution.ExecutionContext
import com.chaomixian.vflow.core.execution.ExecutionServices
import com.chaomixian.vflow.core.module.ExecutionResult
import com.chaomixian.vflow.core.types.VObject
import com.chaomixian.vflow.core.types.VObjectFactory
import com.chaomixian.vflow.core.types.VTypeRegistry
import com.chaomixian.vflow.core.types.basic.VString
import com.chaomixian.vflow.core.types.complex.VFile
import com.chaomixian.vflow.core.types.complex.VImage
import com.chaomixian.vflow.core.workflow.model.ActionStep
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Base64
import java.util.Stack

class Base64EncodeOrDecodeModuleTest {

    @Test
    fun getOutputs_decodeMode_includesImageOutput() {
        val module = Base64EncodeOrDecodeModule()
        val outputs = module.getOutputs(
            ActionStep(
                moduleId = module.id,
                parameters = mapOf("operation" to "decode")
            )
        )

        assertEquals(VTypeRegistry.FILE.id, outputs.first { it.id == "result_file" }.typeName)
        assertEquals(VTypeRegistry.IMAGE.id, outputs.first { it.id == "result_image" }.typeName)
    }

    @Test
    fun execute_decodeMode_outputsTextAndImage() = runBlocking {
        val sourceBytes = byteArrayOf(0x10, 0x20, 0x30, 0x40)
        val workDir = Files.createTempDirectory("vflow-base64-module").toFile()
        val module = Base64EncodeOrDecodeModule()
        val context = createContext(
            variables = mutableMapOf(
                "operation" to VObjectFactory.from("decode"),
                "source_text" to VObjectFactory.from(Base64.getEncoder().encodeToString(sourceBytes)),
                "text_encoding" to VObjectFactory.from(CryptoModuleSupport.ENCODING_BASE64)
            ),
            workDir = workDir
        )

        val result = module.execute(context) { }

        assertTrue(result is ExecutionResult.Success)
        val outputs = (result as ExecutionResult.Success).outputs
        assertEquals(Base64.getEncoder().encodeToString(sourceBytes), (outputs["result_text"] as VString).raw)

        val file = outputs["result_file"] as VFile
        assertArrayEquals(sourceBytes, File(java.net.URI(file.uriString)).readBytes())

        val image = outputs["result_image"] as VImage
        assertArrayEquals(sourceBytes, File(java.net.URI(image.uriString)).readBytes())
    }

    private fun createContext(
        variables: MutableMap<String, VObject>,
        workDir: File
    ): ExecutionContext {
        return ExecutionContext(
            applicationContext = ContextWrapper(null),
            variables = variables,
            magicVariables = mutableMapOf(),
            services = ExecutionServices(),
            allSteps = emptyList(),
            currentStepIndex = 0,
            stepOutputs = mutableMapOf(),
            loopStack = Stack(),
            namedVariables = mutableMapOf(),
            workDir = workDir
        )
    }
}
