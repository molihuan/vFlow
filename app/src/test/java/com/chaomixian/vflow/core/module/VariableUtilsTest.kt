package com.chaomixian.vflow.core.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VariableUtilsTest {

    @Test
    fun `isMagicVariable returns true for single magic variable`() {
        assertTrue("{{step1.output}}".isMagicVariable())
    }

    @Test
    fun `isMagicVariable returns false for adjacent magic variables`() {
        assertFalse("{{step1.output}}{{step2.output}}".isMagicVariable())
    }

    @Test
    fun `isNamedVariable returns true for single named variable`() {
        assertTrue("[[myVar]]".isNamedVariable())
    }

    @Test
    fun `isNamedVariable returns false for adjacent named variables`() {
        assertFalse("[[first]][[second]]".isNamedVariable())
    }
}
