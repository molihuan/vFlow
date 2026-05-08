package com.chaomixian.vflow.core.workflow.module.logic

import android.content.ContextWrapper
import com.chaomixian.vflow.core.execution.ExecutionContext
import com.chaomixian.vflow.core.execution.ExecutionServices
import com.chaomixian.vflow.core.execution.LoopState
import com.chaomixian.vflow.core.module.ExecutionResult
import com.chaomixian.vflow.core.types.VObjectFactory
import com.chaomixian.vflow.core.types.basic.VString
import com.chaomixian.vflow.core.workflow.model.ActionStep
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Stack

class LoopModuleTest {

    @Test
    fun validate_acceptsIntegerLikeDecimalStringCount() {
        val module = LoopModule()

        val result = module.validate(
            ActionStep(
                moduleId = module.id,
                parameters = mapOf("count" to "3.0")
            ),
            emptyList()
        )

        assertTrue(result.isValid)
    }

    @Test
    fun execute_acceptsTextVariableContainingIntegerLikeDecimal() = runBlocking {
        val module = LoopModule()
        val context = ExecutionContext(
            applicationContext = ContextWrapper(null),
            variables = mutableMapOf(),
            magicVariables = mutableMapOf(
                "count" to VString("3.0")
            ),
            services = ExecutionServices(),
            allSteps = emptyList(),
            currentStepIndex = 0,
            stepOutputs = mutableMapOf(),
            loopStack = Stack(),
            namedVariables = mutableMapOf(),
            workDir = File("build/test-workdir")
        )

        val result = module.execute(context) { }

        assertTrue(result is ExecutionResult.Signal)
        assertTrue((result as ExecutionResult.Signal).signal is com.chaomixian.vflow.core.module.ExecutionSignal.Loop)
        val loopState = context.loopStack.peek() as? LoopState.CountLoopState
        assertEquals(3L, loopState?.totalIterations)
    }
}
