package com.chaomixian.vflow.core.types.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VariablePathParserTest {

    @Test
    fun `parseSingleVariableReference parses magic variable`() {
        val parsed = VariablePathParser.parseSingleVariableReference("{{step1.output.length}}")

        assertEquals(listOf("step1", "output", "length"), parsed?.path)
        assertFalse(parsed?.isNamedVariable ?: true)
    }

    @Test
    fun `parseSingleVariableReference parses named variable`() {
        val parsed = VariablePathParser.parseSingleVariableReference("[[profile.name]]")

        assertEquals(listOf("profile", "name"), parsed?.path)
        assertTrue(parsed?.isNamedVariable ?: false)
    }

    @Test
    fun `parseSingleVariableReference rejects adjacent variables`() {
        assertNull(VariablePathParser.parseSingleVariableReference("{{a}}{{b}}"))
        assertNull(VariablePathParser.parseSingleVariableReference("[[a]][[b]]"))
    }

    @Test
    fun `appendPathSegment appends property for magic variable`() {
        assertEquals("{{step1.output.width}}", VariablePathParser.appendPathSegment("{{step1.output}}", "width"))
    }

    @Test
    fun `appendPathSegment appends property for named variable`() {
        assertEquals("[[profile.name]]", VariablePathParser.appendPathSegment("[[profile]]", "name"))
    }
}
