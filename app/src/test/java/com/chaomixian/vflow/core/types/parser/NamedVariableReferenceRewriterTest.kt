package com.chaomixian.vflow.core.types.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NamedVariableReferenceRewriterTest {

    @Test
    fun `rewrite updates embedded references recursively`() {
        val input = mapOf(
            "plain" to "[[profile]]",
            "rich" to "hello [[profile]] world",
            "nested" to listOf(
                "{{vars.profile}}",
                mapOf("text" to "x [[profile]] y")
            )
        )

        val result = NamedVariableReferenceRewriter.rewrite(input, "profile", "username")

        assertTrue(result.changed)
        assertEquals(
            mapOf(
                "plain" to "{{vars.username}}",
                "rich" to "hello {{vars.username}} world",
                "nested" to listOf(
                    "{{vars.username}}",
                    mapOf("text" to "x {{vars.username}} y")
                )
            ),
            result.value
        )
    }
}
