package com.chaomixian.vflow.core.workflow.module.logic

import com.chaomixian.vflow.core.types.VTypeRegistry
import com.chaomixian.vflow.core.workflow.model.ActionStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JumpModuleTest {

    @Test
    fun validate_acceptsNamedVariableReferenceForTargetStepIndex() {
        val module = JumpModule()

        val result = module.validate(
            ActionStep(
                moduleId = module.id,
                parameters = mapOf("target_step_index" to "[[targetStep]]")
            ),
            emptyList()
        )

        assertTrue(result.isValid)
    }

    @Test
    fun inputs_allowNamedVariablesForTargetStepIndex() {
        val module = JumpModule()

        val input = module.getInputs().first { it.id == "target_step_index" }

        assertTrue(input.acceptsNamedVariable)
        assertEquals(setOf(VTypeRegistry.NUMBER.id), input.acceptedMagicVariableTypes)
    }
}
